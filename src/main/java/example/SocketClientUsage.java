/*
 *@Type SocketClientUsage.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 14:07
 * @version
 */
package example;

import client.Client;
import client.SocketClient;

public class SocketClientUsage {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        Client client = new SocketClient(host, port);
//        client.set("zsy3","letitbe");
//        client.set("zsy12","for test");
//        client.get("zsy12");
//        client.rm("zsy12" );
//        client.get("zsy12");
//        client.set("1","1");
//            client.get("1");
//        client.rm("1");
//         插入大量数据
//        client.set("a","dddgsrgfgfgd");
//        for (int i = 0; i < 10; i++) {
//            String key = "key" + i;
//            // 生成一个较大的字符串
//            String value = "value" + i + new String(new char[1024]).replace("\0", "A");
//            client.set(key, value);
//            System.out.println("Set " + key);
//        }
//            client.get("aa");
//        for (int i = 0; i < 400; i++) {
//            client.set("1", "1");
//            client.set("2", "1");
//            client.set("3", "1");
//        }
//            client.rm("1");
//          client.get("3");
//        // 读取部分数据进行验证
//        for (int i = 0; i < 10; i++) {
//            String key = "key" + i;
//            String value = client.get(key);
//            System.out.println("Get " + key + ": " + value);
//        }
//
//        // 删除部分数据进行验证
//        for (int i = 0; i < 10; i++) {
//            String key = "key" + i;
//            client.rm(key);
//            System.out.println("Removed " + key);
//            String value = client.get(key);
//            System.out.println("Get after removal " + key + ": " + value);
//        }
//        client.get("zsy12");
//        client.set("zsy12","for test");
//        client.get("zsy1");
//        client.rm("zsy12");
//          client.get("key999");
//        client.set("wcf","sb");
//            client.get("wcf");
        // 连接并测试数据操作
//        System.out.println("Get key 'zsy1': " + client.get("zsy1"));
//        client.set("zsy1", "for test");
//        System.out.println("Set key 'zsy12': for test");
//        System.out.println("Get key 'zsy12': " + client.get("zsy12"));
//        client.rm("zsy12");
//        System.out.println("Removed key 'zsy12'");
//        System.out.println("Get key 'zsy12': " + client.get("zsy12"));
    }
}