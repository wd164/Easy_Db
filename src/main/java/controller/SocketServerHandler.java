/*
 *@Type SocketServerHandler.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 12:50
 * @version
 */
package controller;

import dto.ActionDTO;
import dto.ActionTypeEnum;
import dto.RespDTO;
import dto.RespStatusTypeEnum;
import service.NormalStore;
import service.Store;
import utils.LoggerUtil;

import java.io.*;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketServerHandler implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(SocketServerHandler.class);
    private Socket socket;
    private Store store;

    public SocketServerHandler(Socket socket, Store store) {
        this.socket = socket;
        this.store = store;
    }

//    @Override
//    public void run() {
//        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
//             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
//
//            // 接收序列化对象
//            ActionDTO dto = (ActionDTO) ois.readObject();
//            LoggerUtil.debug(LOGGER, "[SocketServerHandler][ActionDTO]: {}", dto.toString());
//            System.out.println("" + dto.toString());
//
//            // 处理命令逻辑(TODO://改成可动态适配的模式)
//            if (dto.getType() == ActionTypeEnum.GET) {
//                String value = this.store.get(dto.getKey());
//                LoggerUtil.debug(LOGGER, "[SocketServerHandler][run]: {}", "get action resp" + dto.toString());
//                RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, value);
//                oos.writeObject(resp);
//                oos.flush();
//            }
//            if (dto.getType() == ActionTypeEnum.SET) {
//                this.store.set(dto.getKey(), dto.getValue());
//                LoggerUtil.debug(LOGGER, "[SocketServerHandler][run]: {}", "set action resp" + dto.toString());
//                RespDTO resp = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
//                oos.writeObject(resp);
//                oos.flush();
//            }
//            if (dto.getType() == ActionTypeEnum.RM) {
//                this.store.rm(dto.getKey());
//            }
//
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()) {
                try {
                    // 接收序列化对象
                    ActionDTO dto = (ActionDTO) ois.readObject();
                    LoggerUtil.debug(LOGGER, "[SocketServerHandler][ActionDTO]: {}", dto.toString());

                    // 根据ActionDTO类型处理不同的数据库操作
                    switch (dto.getType()) {
                        case SET:
                            store.set(dto.getKey(), dto.getValue());
                            RespDTO respSet = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                            oos.writeObject(respSet);
                            break;
                        case GET:
                            String value = store.get(dto.getKey());
                            RespDTO respGet = new RespDTO(RespStatusTypeEnum.SUCCESS, value);
                            oos.writeObject(respGet);
                            break;
                        case RM:
                            store.rm(dto.getKey());
                            RespDTO respRm = new RespDTO(RespStatusTypeEnum.SUCCESS, null);
                            oos.writeObject(respRm);
                            break;
                        default:
                            RespDTO respError = new RespDTO(RespStatusTypeEnum.FAIL, "Unknown command");
                            oos.writeObject(respError);
                    }

                    // 刷新输出流以发送响应
                    oos.flush();
                } catch (EOFException e) {
                    // 客户端可能已经关闭连接
                    LoggerUtil.info(LOGGER, "Client disconnected: {}", e.getMessage());
                    break;
                } catch (ClassNotFoundException | IOException e) {
                    // 发送错误响应或记录日志
                    RespDTO errorResp = new RespDTO(RespStatusTypeEnum.FAIL, "Server error: " + e.getMessage());
                    try {
                        oos.writeObject(errorResp);
                        oos.flush();
                    } catch (IOException ex) {
                        // 如果无法发送错误响应，记录日志
                        LoggerUtil.error(LOGGER, ex, "Failed to send error response to client");
                    }
                    e.printStackTrace();
                    break;
                }
            }
        } catch (IOException e) {
            // 处理创建流时的异常
            LoggerUtil.error(LOGGER, e, "Error in SocketServerHandler: {}", e.getMessage());
        } finally {
            // 关闭socket连接
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                LoggerUtil.error(LOGGER, e, "Error closing socket: {}", e.getMessage());
            }finally {
                // 在这里执行必要的清理操作，例如刷新内存表到磁盘
                if (store instanceof NormalStore) {
                    ((NormalStore) store).flushMemTableToDisk();
                    LoggerUtil.info(LOGGER, "Flushed memTable to disk after client disconnect");
                }
            }
        }
    }


}
