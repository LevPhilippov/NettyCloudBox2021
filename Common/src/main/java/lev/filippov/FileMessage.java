package lev.filippov;

public class FileMessage extends Message{

    private byte[] bytes;
    private String remotePath;

    public FileMessage() {
        type = Message.FILE;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
}
