package pl.zuz.sages.p1;

import java.io.*;

public class MessageType implements Serializable{
    static final int users = 0, message = 1, logout = 2, file = 3;

    private int message_type;

    private String message_context;

    MessageType(int type, String message){
        this.message_type = type;
        this.message_context = message;
    }


    public int getMessage_type() {
        return message_type;
    }

    public String getMessage_context() {
        return message_context;
    }
}
