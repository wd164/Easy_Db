package controller;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.NormalStore;
import service.Store;
import utils.LoggerUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Setter
@Getter
public class SocketServerController implements Controller {

    private final Logger LOGGER = LoggerFactory.getLogger(SocketServerController.class);
    private final String logFormat = "[SocketServerController][{}]: {}";
    private String host;
    private int port;
    private Store store; // This should be an instance of a class that implements the Store interface.

    public SocketServerController(String host, int port, Store store) {
        this.host = host;
        this.port = port;
        this.store = store;
    }

    @Override
    public void set(String key, String value) {
        // Implement the logic to set a key-value pair in the store.
        this.store.set(key, value);
    }

    @Override
    public String get(String key) {
        // Implement the logic to retrieve a value by key from the store.
        return this.store.get(key);
    }

    @Override
    public void rm(String key) {
        // Implement the logic to remove a key-value pair from the store.
        this.store.rm(key);
    }

    @Override
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LoggerUtil.info(LOGGER, logFormat, "startServer", "Server started, waiting for connections...");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    LoggerUtil.info(LOGGER, logFormat, "startServer", "New client connected");
                    // For each client connection, create a new thread to handle the request.
                    new Thread(new SocketServerHandler(socket, this.store)).start();
                } catch (IOException e) {
                    LoggerUtil.error(LOGGER, e, "Error accepting client connection");

                }
            }
        } catch (IOException e) {
            LoggerUtil.error(LOGGER, e, "Error starting server");
        }
    }
}