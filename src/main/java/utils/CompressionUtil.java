package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class CompressionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionUtil.class);
    private static final String logFormat = "[CompressionUtil][Operation]: {} - {}";

    /**
     * 压缩文件到指定的输出路径，并添加压缩文件后缀。
     *
     * @param sourceFilePath 原始文件的路径。
     * @param suffix 压缩文件的后缀。
     * @throws IOException 如果压缩过程中发生I/O错误。
     */
    public static void compressFile(String sourceFilePath, String suffix) throws IOException {
        // 使用File类来处理文件路径和文件操作
        File sourceFile = new File(sourceFilePath);
        File compressedFile = new File(sourceFilePath + suffix);

        try (FileInputStream fileInputStream = new FileInputStream(sourceFile);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(compressedFile))) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            // 读取原始文件并写入压缩流
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                gzipOutputStream.write(buffer, 0, bytesRead);
            }

            // 日志记录：压缩成功
            LoggerUtil.info(LOGGER, logFormat, "Compression successful", compressedFile.getAbsolutePath());

            gzipOutputStream.finish(); // 完成压缩
        } catch (IOException e) {
            // 处理可能发生的任何I/O异常
            LoggerUtil.error(LOGGER, e, "Error during compression");
            throw new IOException("Error compressing file: " + sourceFilePath, e);
        }
    }
}