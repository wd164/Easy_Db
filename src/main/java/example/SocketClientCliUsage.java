package example;

import client.CmdClient;

public class SocketClientCliUsage {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        CmdClient cmdClient = new CmdClient(host, port);
        cmdClient.initialize(args);
    }
}
