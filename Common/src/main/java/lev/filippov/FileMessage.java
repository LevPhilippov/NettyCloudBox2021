package lev.filippov;

public class FileMessage extends Message{

    private byte[] bytes;
    private String remotePath;
    private Long part;
    private Long parts;

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

    public Long getPart() {
        return part;
    }

    public void setPart(Long part) {
        this.part = part;
    }

    public Long getParts() {
        return parts;
    }

    public void setParts(Long parts) {
        this.parts = parts;
    }


}
