package example;

import client.Client;
import client.SocketClient;

import java.util.Scanner;

public class SocketClientCliUsage {
//    private static Client client;
    private static SocketClient socketClient;

    public static void main(String[] args) {
        // 假设主机和端口从命令行参数中获取
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12345;

//        client = new SocketClient(host, port);
        socketClient = new SocketClient(host, port);

        // 命令行交互循环
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter command (set, get, rm, exit):");
            String commandLine = scanner.nextLine();
            String[] commands = commandLine.split(" ");

            if (commands[0].equalsIgnoreCase("set")) {
                if (commands.length != 3) {
                    System.out.println("Usage: set <key> <value>");
                    continue;
                }
                socketClient.set(commands[1], commands[2]);
            } else if (commands[0].equalsIgnoreCase("get")) {
                if (commands.length != 2) {
                    System.out.println("Usage: get <key>");
                    continue;
                }
                socketClient.get(commands[1]);
            } else if (commands[0].equalsIgnoreCase("rm")) {
                if (commands.length != 2) {
                    System.out.println("Usage: rm <key>");
                    continue;
                }
                socketClient.rm(commands[1]);
            } else if (commands[0].equalsIgnoreCase("exit")) {
                break;
            } else {
                System.out.println("Unknown command");
            }
        }

        scanner.close();
    }
}