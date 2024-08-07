/*
 *@Type CommandPos.java
 * @Desc
 * @Author urmsone urmsone@163.com
 * @date 2024/6/13 02:35
 * @version
 */
package model.command;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommandPos {
    private int pos;
    private int len;
    private String fileName;

    public CommandPos(int pos, int len,String fileName) {
        this.pos = pos;
        this.len = len;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "CommandPos{" +
                "pos=" + pos +
                ", len=" + len +
                ",fileName=" + fileName +
                '}';
    }

    public String getFileName() {
        return fileName;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }
}
