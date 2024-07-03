/*
 *@Type NormalStore.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 02:07
 * @version
 */
package service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import controller.SocketServerHandler;
import model.command.Command;
import model.command.CommandPos;
import model.command.RmCommand;
import model.command.SetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CommandUtil;
import utils.LoggerUtil;
import utils.RandomAccessFileUtil;
import utils.CompressionUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import utils.CompressionUtil;


public class NormalStore implements Store {

    public static final String TABLE = ".table";
    public static final String DB = ".db";
    public static final String RW_MODE = "rw";
    public static final String NAME = "data";
    public static final String Data_NAME = "SSTable";
    private final Logger LOGGER = LoggerFactory.getLogger(NormalStore.class);
    private final String logFormat = "[NormalStore][{}]: {}";
    private static final int MEM_TABLE_THRESHOLD = 10 * 10; // 持久化阈值 10KB
    private static final long FILE_SIZE_THRESHOLD = 10 * 10; // 文件大小阈值 10MB
    private String currentFilePath;
    private final String dataFilePath;

//    private static final String COMPRESSED_FILE_SUFFIX = ".compressed"; // 压缩文件后缀


    /**
     * 内存表，类似缓存
     */
    private TreeMap<String, Command> memTable;

    /**
     * hash索引，存的是数据长度和偏移量
     */
    private HashMap<String, CommandPos> index;

    /**
     * 数据目录
     */
    private final String dataDir;

    /**
     * 读写锁，支持多线程，并发安全写入
     */
    private final ReadWriteLock indexLock;

    /**
     * 暂存数据的日志句柄
     */
    private RandomAccessFile writerReader;

    private CompressionUtil CompressionUtil;

    private final ExecutorService executorService;

    /**
     * 持久化阈值
     */
//    private final int storeThreshold;
    public NormalStore(String dataDir) throws IOException {
        this.dataDir = dataDir;
        this.indexLock = new ReentrantReadWriteLock();
        this.memTable = new TreeMap<String, Command>();
        this.index = new HashMap<>();
        this.currentFilePath = dataDir + File.separator + NAME + TABLE;
        this.executorService = Executors.newFixedThreadPool(2); // 创建一个固定大小为2的线程池
        this.dataFilePath = dataDir + File.separator + Data_NAME + DB; // 实际数据文件路径

        File file = new File(dataDir);
        if (!file.exists()) {
            LoggerUtil.info(LOGGER, logFormat, "NormalStore", "dataDir isn't exist,creating...");
            file.mkdirs();
        }
        this.reloadIndex();

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                flushMemTableToDisk();
            } catch (Exception e) {
                LoggerUtil.error(LOGGER, e, logFormat, "Error flushing memTable during shutdown");
            }
        }));
    }


    public String genFilePath() {
        return this.dataDir + File.separator + NAME + TABLE;
    }


    public void reloadIndex() {
        try {
            RandomAccessFile file = new RandomAccessFile(this.genFilePath(), RW_MODE);
            long len = file.length();
            long start = 0;
            file.seek(start);
            while (start < len) {
                int cmdLen = file.readInt();
                byte[] bytes = new byte[cmdLen];
                file.read(bytes);
                JSONObject value = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
                Command command = CommandUtil.jsonToCommand(value);
                start += 4;
                if (command != null) {
                    CommandPos cmdPos = new CommandPos((int) start, cmdLen);
                    index.put(command.getKey(), cmdPos);
                }
                start += cmdLen;
            }
            file.seek(file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoggerUtil.debug(LOGGER, logFormat, "reload index: " + index.toString());
    }


//    没有rotate前版本
//    private void reloadIndex() throws IOException {
//        File file = new File(genFilePath());
//        if (!file.exists()) {
//            return;
//        }
//
//        try (RandomAccessFile reader = new RandomAccessFile(file, RW_MODE)) {
//            long fileLength = file.length();
//            long pos = 0;
//            while (pos < fileLength) {
//                reader.seek(pos);
//                if (fileLength - pos < 4) {
//                    LOGGER.warn(logFormat, "Incomplete length information at position " + pos);
//                    break;
//                }
//                int length = reader.readInt();
//                if (fileLength - pos - 4 < length) {
//                    LOGGER.warn(logFormat, "Incomplete data at position " + pos + " with length " + length);
//                    break;
//                }
//                byte[] commandBytes = new byte[length];
//                reader.readFully(commandBytes);
//                String jsonString = new String(commandBytes, StandardCharsets.UTF_8);
//
//                // 打印解析前的 JSON 字符串
//                System.out.println("Parsing JSON: " + jsonString);
//
//                Command command = JSON.parseObject(jsonString, Command.class);
//                CommandPos cmdPos = new CommandPos((int) pos, length);
//                index.put(command.getKey(), cmdPos);
//                pos += 4 + length;
//            }
//        }
//    }

    @Override
    public void set(String key, String value) {
        try {
            SetCommand command = new SetCommand(key, value);
            byte[] commandBytes = JSONObject.toJSONBytes(command);
            // 加锁
            indexLock.writeLock().lock();
            // TODO://先写内存表，内存表达到一定阀值再写进磁盘
            // 写内存表（memTable）
            memTable.put(key, command);
            // 写table（wal）文件
            RandomAccessFileUtil.writeInt(this.genFilePath(), commandBytes.length);
            int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);
            // 保存到memTable
            // 添加索引
            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            index.put(key, cmdPos);
            // TODO://判断是否需要将内存表中的值写回table
            if (memTable.size() >= MEM_TABLE_THRESHOLD) {
                // 检查内存表是否达到阀值
                // 将内存表写入磁盘
                flushMemTableToDisk();
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }
        checkAndRotateIfNecessary();
        checkAndMergeIfNecessary(); // 写入操作后检查是否需要合并
    }

    @Override
    public String get(String key) {
        try {
            indexLock.readLock().lock();

            // 先从内存表中获取数据
            if (memTable.containsKey(key)) {
                Command cmd = memTable.get(key);
                if (cmd instanceof SetCommand) {
                    return ((SetCommand) cmd).getValue();
                }
                if (cmd instanceof RmCommand) {
                    return null; // 数据已被删除
                }
            }

            // 从索引中获取信息
            CommandPos cmdPos = index.get(key);
            if (cmdPos == null) {
                return null;
            }
            byte[] commandBytes = RandomAccessFileUtil.readByIndex(this.genFilePath(), cmdPos.getPos(), cmdPos.getLen());

            JSONObject value = JSONObject.parseObject(new String(commandBytes));
            Command cmd = CommandUtil.jsonToCommand(value);
            if (cmd instanceof SetCommand) {
                return ((SetCommand) cmd).getValue();
            }
            if (cmd instanceof RmCommand) {
                return null;
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            indexLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void rm(String key) {
        try {
            RmCommand command = new RmCommand(key);
            byte[] commandBytes = JSONObject.toJSONBytes(command);
            indexLock.writeLock().lock();

            // 写入内存表
            memTable.put(key, command);

            // 写入 WAL 文件
            RandomAccessFileUtil.writeInt(this.genFilePath(), commandBytes.length);
            int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);
            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            index.put(key, cmdPos);
            // 写入磁盘后，清空内存表
            memTable.clear();
            // 检查内存表是否达到阈值，达到则写入磁盘
            if (memTable.size() >= MEM_TABLE_THRESHOLD) {
                flushMemTableToDisk();
            }

            LOGGER.info("Removed key: {} with command length: {}", key, commandBytes.length);
        } catch (Throwable t) {
            LOGGER.error(logFormat, "rm", t.getMessage(), t);
            throw new RuntimeException(t);
        } finally {
            indexLock.writeLock().unlock();
        }
        checkAndRotateIfNecessary();
    }

//@Override
//public void rm(String key) {
//    try {
//        RmCommand command = new RmCommand(key);
//        byte[] commandBytes = JSONObject.toJSONBytes(command);
//
//        // 加写锁
//        indexLock.writeLock().lock();
//        try {
//            // 从内存表中删除键
//            memTable.remove(key);
//            // 从索引中删除键
//            index.remove(key);
//
//            // 写入WAL日志文件，记录删除操作
//            RandomAccessFileUtil.writeInt(this.genFilePath(), commandBytes.length);
//            int pos = RandomAccessFileUtil.write(this.genFilePath(), commandBytes);
//
//            // 由于键已被删除，我们不需要在索引中保留它的位置信息
//            // 如果需要持久化删除操作，可以在这里添加逻辑
//        } finally {
//            // 释放写锁
//            indexLock.writeLock().unlock();
//        }
//    } catch (Throwable t) {
//        // 处理异常，例如打印堆栈跟踪或者记录日志
//        LoggerUtil.error(LOGGER, t, logFormat, "Error removing key: " + key);
//    }
//}

    private void flushMemTableToDisk() {
        indexLock.writeLock().lock();
        try {
            try (RandomAccessFile dataFile = new RandomAccessFile(this.dataFilePath, RW_MODE)) {
                for (Map.Entry<String, Command> entry : memTable.entrySet()) {
                    String key = entry.getKey();
                    Command command = entry.getValue();
                    byte[] commandBytes = JSONObject.toJSONBytes(command);

                    // 将命令写入实际数据文件
                    dataFile.seek(dataFile.length());
                    dataFile.writeInt(commandBytes.length);
                    dataFile.write(commandBytes);

                    // 更新索引
                    CommandPos cmdPos = new CommandPos((int) dataFile.length() - commandBytes.length - 4, commandBytes.length);
                    index.put(key, cmdPos);

                    LOGGER.info("Flushed command to disk: key={}, length={}", key, commandBytes.length);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Error flushing memTable to disk", t);
        } finally {
            // 清空内存表
            memTable.clear();
            indexLock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        // 关闭资源
        if (writerReader != null) {
            writerReader.close();
        }
    }

    private void rotate() {
        try {
            if (writerReader != null) {
                writerReader.close();
            }

            // 使用时间生成唯一的文件名
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String rotatedFilePath = dataFilePath + "." + dateFormat.format(System.currentTimeMillis()) + ".old";
            File dataFile = new File(dataFilePath);
            if (dataFile.renameTo(new File(rotatedFilePath))) {
                LOGGER.info(logFormat, "文件rotate成功，重命名为: {}", rotatedFilePath);
            } else {
                LOGGER.error(logFormat, "文件重命名失败");
                return;
            }

            writerReader = new RandomAccessFile(dataFilePath, RW_MODE);
            LOGGER.info(logFormat, "创建新的data文件: {}", dataFilePath);

            // 使用多线程进行压缩
//            executorService.submit(() -> compressRotatedFile(rotatedFilePath));

        } catch (IOException e) {
            LoggerUtil.error(LOGGER, e, logFormat, "文件rotate时发生错误");
        }
    }

//    private void compressRotatedFile(String filePath) {
//        try {
//            CompressionUtil.compressFile(filePath, COMPRESSED_FILE_SUFFIX);
//            LOGGER.info(logFormat, "压缩rotate文件: {}", filePath);
//        } catch (IOException e) {
//            LoggerUtil.error(LOGGER, e, logFormat, "压缩旋转文件时发生错误: " + filePath);
//        }
//    }

    private void compressRotatedFile() {

    }

    private void checkAndRotateIfNecessary() {
        File DataFilePath = new File(dataFilePath);
        if (DataFilePath.length() >= FILE_SIZE_THRESHOLD) {
            rotate();
        }
    }

    private File[] countSSTableFiles() {
        // 使用正则表达式匹配以SSTable开头的文件
        FilenameFilter filter = (dir, name) -> name.matches("^SSTable.*");
        File dir = new File(dataDir);
        File[] sstableFiles = dir.listFiles(filter);
        System.out.println(sstableFiles.length);
        return sstableFiles;
    }

    private void checkAndMergeIfNecessary() {
        File[] sstableFiles = countSSTableFiles();
        if (sstableFiles.length >= 5) {
            mergeSSTables(sstableFiles);
        }
    }

    public void mergeSSTables(File[] sstableFiles) {
        if (sstableFiles == null || sstableFiles.length == 0) {
            LOGGER.info("No SSTable files to merge.");
            return;
        }

        // 使用TreeMap来去除重复的键值对，并保持键的有序性
        TreeMap<String, Command> mergedData = new TreeMap<>();
        for (File file : sstableFiles) {
            try (RandomAccessFile reader = new RandomAccessFile(file, "r")) { // 以只读模式打开文件
                while (reader.getFilePointer() < reader.length()) {
                    int length = reader.readInt(); // 读取长度前缀
                    byte[] jsonBytes = new byte[length]; // 根据长度创建字节数组
                    reader.readFully(jsonBytes); // 读取JSON字符串
                    String json = new String(jsonBytes, StandardCharsets.UTF_8); // 转换为字符串
                    JSONObject jsonObject = JSON.parseObject(json); // 解析JSON
                    Command command = CommandUtil.jsonToCommand(jsonObject); // 转换为Command对象
                    // 只添加尚未在mergedData中存在的键
                    mergedData.putIfAbsent(command.getKey(), command);
                }
            } catch (IOException e) {
                LoggerUtil.error(LOGGER, e, logFormat, "Error reading SSTable file: " + file.getName());
            }
        }

        // 将合并后的数据写入新的SSTable文件
        try (RandomAccessFile dataFile = new RandomAccessFile(new File(dataDir, Data_NAME + "_merged" + DB), "rw")) { // 以读写模式打开文件
            for (Map.Entry<String, Command> entry : mergedData.entrySet()) {
                Command command = entry.getValue();
                String json = JSONObject.toJSONString(command); // 转换为JSON字符串
                byte[] commandBytes = json.getBytes(StandardCharsets.UTF_8); // 转换为字节数组
                dataFile.writeInt(commandBytes.length); // 写入长度
                dataFile.write(commandBytes); // 写入JSON字符串的字节
            }
        } catch (IOException e) {
            LoggerUtil.error(LOGGER, e, logFormat, "Error writing merged SSTable file.");
        }

        // 删除旧的SSTable文件，并重命名新的SSTable文件
        for (File file : sstableFiles) {
            System.out.println(file);
            file.delete();
        }
        // 这里应该有重命名逻辑，将新的SSTable文件重命名为原始文件名

    }
}