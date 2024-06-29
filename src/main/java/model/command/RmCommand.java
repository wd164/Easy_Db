/*
 *@Type RmCommand.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 01:57
 * @version
 */
package model.command;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RmCommand extends AbstractCommand {
    private String key;

    public RmCommand(String key) {
        super(CommandTypeEnum.RM); // 调用父类的构造函数，并传递CommandTypeEnum.RM
        this.key = key;
    }

    @Override
    public String getKey() {
        return key; // 提供 getKey() 方法的具体实现
    }

    public void setKey(String key) {
        this.key = key;
    }
}
