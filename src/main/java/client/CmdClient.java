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

public class CmdClient implements Client {
    // 实现接口中定义的set方法
    @Override
    public void set(String key, String value) {
        // 命令行设置键值对的逻辑
    }

    // 实现接口中定义的get方法
    @Override
    public String get(String key) {
        // 命令行获取键值的逻辑
        return null;
    }

    // 实现接口中定义的rm方法
    @Override
    public void rm(String key) {
        // 命令行删除键值的逻辑
    }

    // 可能还有其他与命令行交互相关的方法
}