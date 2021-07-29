package lev.filippov;


public class Constants {
    //server relative path
    public static final String SERVER_RELATIVE_PATH = "server\\";
    //temp user relative path
    public static final String CLIENT_RELATIVE_PATH = "client\\";
    //max bya array for one messagefile size in bytes
    public static final int MAX_BYTE_ARRAY_SIZE = 1024*100;
    //max messagefile size
    public static final int MAX_MESSAGE_SIZE = MAX_BYTE_ARRAY_SIZE*2;
    //ParamsMap key for text message
    public static final String MESSAGE = "message";
    //ParamsMap key for GET_STRUCTURE command
    public static final String FILES_LIST = "filesList";
    //ParamsMap key for local path
    public static final String LOCAL_PATH = "localPath";
    //ParamsMap key for remote path
    public static final String REMOTE_PATH = "remotePath";

}
