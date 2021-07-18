package lev.filippov;

import java.io.Serializable;

public abstract class Message implements Serializable {
    public static final int MAX_MESSAGE_SIZE = 1024;
    protected static final int SERVICE = 1;
    protected static final int FILE = 2;
    protected int type;
}
