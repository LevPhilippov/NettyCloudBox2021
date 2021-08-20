package lev.filippov;

import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

import static lev.filippov.Constants.*;

public class ClientUtils {
    private static final Logger logger = LogManager.getLogger(ClientUtils.class.getName());

    static {
        String clientFolder = NettyClient.getInstance().getCLIENT_FOLDER();
        Path clientFolderPath = Paths.get(clientFolder);
        if(!Files.exists(clientFolderPath)){
            try {
                Files.createDirectory(clientFolderPath);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }


    private static void checkFileMessageDatNonNull(FileMessage fm) throws IOException {
        if(fm.getBytes() == null) {
            throw new IOException("Байтовый массив is null!");
        }
        if(fm.getRemotePath() == null) {
            throw new IOException("Path is null!");
        }
    }
    /**
     * Method defines either file or folder structure are requested to upload.
     * In case of file it builds a path composed of server folder where client wants to put this file and file name and writes
     * it into ctx.
     * In case of folder structure it walks file tree and builds complex path composed of server folder where client wants
     * to put the origin folder, walked path and file name and writes it to ctx.
     * Before writing files the method requests server to create all the directories of file tree.
     * @param localPath describes path to file into client file system.
     * @param remotePath describes path to place where file(origin folder) is requested to put.
     * @param authKey authorisation key, earned after authorisation procedure. Key is creating      *
     *
     *
     * */
    static void writeToChannelManager(Channel channel, String localPath, String remotePath, AuthKey authKey){
        Path systemLocalPath = getLocalPath(localPath);
        if (!Files.exists(systemLocalPath)) {
            System.out.printf("File or folder %1$s you are querying isn't exist or path is wrong!\n", localPath);
            return;
        }
        if (Files.isDirectory(systemLocalPath)) {
            try {
                Files.walkFileTree(systemLocalPath, new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String finalRemotePath  = remotePath + systemLocalPath.getParent().relativize(file).toString();
                        System.out.println(systemLocalPath);
                        writeFileToChannel(channel,file, finalRemotePath, authKey);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        createRemoteDirectory(channel, systemLocalPath.getParent().relativize(dir).toString(), remotePath, authKey);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            writeFileToChannel(channel, systemLocalPath, remotePath + systemLocalPath.getFileName(), authKey);
        }
    }
    /**
     * Method requests server side to create a folder into user's space using
     *@param localRelativePath describes the path to file into client file space. If folder are creating by user request
     *                         (not by writeToChannel method - this param is name of folder you are creating)
     * @param remotePath describes the path to file into remote users file space
     * */
    static void createRemoteDirectory(Channel channel, String localRelativePath, String remotePath, AuthKey authKey) {
        ServiceMessage sm = new ServiceMessage(authKey);
        sm.setMessageType(MessageType.CREATE_FOLDER);
        StringBuilder remotePathBuilder = new StringBuilder(remotePath);
        if (!remotePath.endsWith("/") && !remotePath.endsWith("\\") && !remotePath.equals("")) {
            remotePathBuilder.append("\\");
        }
        remotePathBuilder.append(localRelativePath);
        if (!localRelativePath.endsWith("/") && !localRelativePath.endsWith("\\")&& !localRelativePath.equals("")) {
            remotePathBuilder.append("\\");
        }
        sm.getParametersMap().put(REMOTE_PATH, remotePathBuilder.toString());
        logger.info("Путь создания папки на сервере: " + sm.getParametersMap().get(REMOTE_PATH));
        try {
            channel.writeAndFlush(sm).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method writes file to channel using chunk-driven way.
     *@param  localPath the path to file into client file space.
     * @param remotePath describes the path to file into remote users file space.
     * */

    static void writeFileToChannel(Channel channel, Path localPath, String remotePath, AuthKey authKey) {
        Long part=null;
        Long parts=null;
        long size = 0;
        try {
            size = Files.size(localPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //quantity of parts
        parts = (size % MAX_BYTE_ARRAY_SIZE > 0) ? size/ MAX_BYTE_ARRAY_SIZE : size/ MAX_BYTE_ARRAY_SIZE + 1 ;
        part=0L;
        System.out.printf("Количество частей у файла %1$s размером %2$d байт равно %3$d.\n", localPath.toString(), size, parts );
        ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_BYTE_ARRAY_SIZE);
        FileMessage fileMessage = new FileMessage(authKey);

        try(FileChannel fileChannel = new RandomAccessFile(localPath.toFile(),"r").getChannel()) {
            int read;
            byte[] bytes;
            while ((read = fileChannel.read(byteBuffer)) !=-1 || part==0) {
                    byteBuffer.flip();
                    if(read==-1)
                        read=0;
                    bytes = new byte[read];
                    byteBuffer.get(bytes);
                    fileMessage.setParts(parts);
                    fileMessage.setPart(part++);
                    fileMessage.setBytes(bytes);
                    fileMessage.setRemotePath(remotePath);
                    channel.writeAndFlush(fileMessage).sync();
                    byteBuffer.clear();
                }

        } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
}

    static void getFromChannelToFile(FileMessage msg) {
        try {
            checkFileMessageDatNonNull(msg);
            Path localPath = getLocalPath(msg.getRemotePath());

            if(msg.getPart().equals(0L)) {
                if (!Files.exists(localPath.getParent())) {
                    Files.createDirectories(localPath.getParent());
                }
                Files.createFile(localPath);
                logger.info("Начинается копирование файла {}", localPath.getFileName());
            }
            logger.info("Получена часть {} из {} ", msg.getPart() , msg.getParts());
            Files.write(localPath, msg.getBytes(), StandardOpenOption.APPEND);

            if(msg.getPart().equals(msg.getParts())){
                System.out.println("Copying completed to " + localPath);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static Path getLocalPath(String remotePath) {
        return Paths.get(NettyClient.getInstance().getCLIENT_FOLDER(), remotePath);
    }


    public static void createDirectory(ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH));
        if(Files.exists(localPath)){
            System.out.printf("Folder %1$s already exist!\n", localPath.toString());
        } else {
            try {
                logger.info("Попытка создать папку по адресу " + localPath);
                Files.createDirectory(localPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void removeRemote(Channel channel, String remotePath, AuthKey authkey) {
        ServiceMessage sm = new ServiceMessage(authkey);
        sm.setMessageType(MessageType.REMOVE);
        sm.getParametersMap().put(REMOTE_PATH, remotePath);
        try {
            channel.writeAndFlush(sm).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void printFilesList(ServiceMessage sm) {
        String currentRemoteFolder = (String) sm.getParametersMap().get(REMOTE_PATH);
        NettyClient.currentRemoteFolderPath = Objects.isNull(currentRemoteFolder) ? "root": currentRemoteFolder;
        List<String> filesList = (List<String>) sm.getParametersMap().get(FILES_LIST);
        System.out.println("Current folder is: " + NettyClient.currentRemoteFolderPath);
        if (Objects.isNull(filesList))
            System.out.println("Congratulations! Your storage is empty! You have a bunch of space!");
        else
            filesList.forEach(System.out::println);
    }


}
