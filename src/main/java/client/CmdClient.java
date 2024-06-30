/*
 *@Type CmdClient.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 13:58
 * @version
 */
package client;

//public class CmdClient{
//}

import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.cli.*;

import org.apache.commons.cli.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

public class CmdClient {

    public static void main(String[] args) {
        // 创建选项对象
        Options options = new Options();
        SocketClient socketClient = new SocketClient("localhost", 12345);

        // 定义各个命令行参数
        options.addOption("s", "set", true, "set key and value");
        options.addOption("g", "get", true, "get value by key");
        options.addOption("r", "remove", true, "remove key and value by key");
        options.addOption("h", "help", false, "show this help message and exit");

        // 使用 DefaultParser 实例化命令行解析器
        CommandLineParser cliParser = new DefaultParser();
        org.apache.commons.cli.HelpFormatter formatter = new HelpFormatter();

        try {
            // 解析命令行参数
            CommandLine cmd = cliParser.parse(options, args);

            // 帮助信息
            if (cmd.hasOption("h")) {
                formatter.printHelp("CmdClient [-s SET] [-g GET] [-r REMOVE] [-h] {} ...", options);
            }

            // set
            if (cmd.hasOption("s")) {
                String[] k_v = cmd.getOptionValue("s").split("-");
                socketClient.set(k_v[0], k_v[1]);
            }

            // get
            if (cmd.hasOption("g")) {
                String key = cmd.getOptionValue("g");
                socketClient.get(key);
            }

            // rm
            if (cmd.hasOption("r")) {
                String key = cmd.getOptionValue("r");
                socketClient.rm(key);
            }


        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            formatter.printHelp("CmdClient [-s SET] [-g GET] [-r REMOVE] [-h] {} ...", options);
        }
    }
}