package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.*;
import static lev.filippov.Constants.*;

public class ServerUtils {

    private static Logger logger = LogManager.getLogger(ServerUtils.class.getName());

    static {
        logger.info("Server folder is: {}", Paths.get(MainServer.getInstance().getSERVER_RELATIVE_PATH()).toAbsolutePath());
        try {
            Files.createDirectories(Paths.get(MainServer.getInstance().getSERVER_RELATIVE_PATH()));
        } catch (IOException e) {
            logger.error(e.getMessage());

        }
    }

    private static void checkFileMessageDatNonNull(FileMessage fm) throws IOException {
        if (fm.getBytes() == null) {
            throw new IOException("Байтовый массив is null!");
        }
        if (fm.getRemotePath() == null) {
            throw new IOException("Path is null!");
        }
    }
    /**
     * Method writes useful data from FileMessage object, received from ChannelHandlerContext to file.
     * A FileMessage has to contain a complex path points on place where to save a data.
     * @author Lev Philippov
     * @param msg Message containing useful data to write, received from FileMessageHandler.
     * @see lev.filippov.FileMessageHandler
     * @throws IOException
     * */
    static void getFromChannelToFile(FileMessage msg) {
        try {
            checkFileMessageDatNonNull(msg);
            //TODO: Нужно бороться с конкатенацией, если не указана слэш
            Path localPath = getLocalPath(msg.getRemotePath(), msg.getAuthKey());

            if (msg.getPart().equals(0L)) {
                if(!Files.exists(localPath.getParent()))
                    Files.createDirectories(localPath.getParent());
                Files.createFile(localPath);
                System.out.printf("Начинается копирование файла %s", localPath.getFileName());
            } if (msg.getPart().equals(msg.getParts())) {
                ServiceMessage sm = new ServiceMessage(msg.getAuthKey());
                sm.setMessageType(MessageType.MESSAGE);
                sm.getParametersMap().put(MESSAGE, String.format("Файл успешно скопирован по адресу %s.", msg.getRemotePath()));
            }

            System.out.println("Получена часть " + msg.getPart() + " из " + msg.getParts());
            Files.write(localPath, msg.getBytes(), StandardOpenOption.APPEND);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static Path getLocalPath(String localPath, AuthKey authKey) {
            String userFolder = PersistanceBean.getUserFolderPath(authKey.getLogin());
            return Objects.isNull(localPath) ? Paths.get(MainServer.getInstance().getSERVER_RELATIVE_PATH(),userFolder) : Paths.get(MainServer.getInstance().getSERVER_RELATIVE_PATH(), userFolder,localPath);
    }
    /**
    Method handle ServiceMessage from ServiceMessageHandler by defining what is asking either file or file structure with embedded folders.
     If case of file method starts writing requested file to ChannelHandlerContext.
     In case of file structure before writing files the method walks file tree starting from folder is requested
     and before writing files requests client to create folder structure for each.
     @param ctx ChannelHandlerContext from channel's pipeline.
     @param msg ServiceMessage which contains request.
     @see ServiceMessageHandler
     */
    static void writeToChannelManager(ChannelHandlerContext ctx, ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH), msg.getAuthKey());
        //если запрашивается файл
        if (!Files.isDirectory(localPath)) {
            writeFileToChannel(ctx, msg, false);
        }
        //если запрашивается каталог
        else {
            try {
                logger.info("Исходный путь: " + localPath);
                Files.walkFileTree(localPath,new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                      msg.getParametersMap().put(REMOTE_PATH, file.subpath(1,file.getNameCount()).toString());
                        msg.getParametersMap().put(REMOTE_PATH, localPath.getParent().relativize(file).toString());
                        /*TODO: вместо сабпас сделать релатив относительно SERVER_RELATIVE_PATH + имени каталога пользователя.
                        SERVER_R_P подавать либо как входной параметр при запуске приложения,
                        либо он должен определяться самостоятельно (изучить такую возможнсть)*/
                        System.out.println("Relative path for " + file + " is " + msg.getParametersMap().get(REMOTE_PATH));
                        writeFileToChannel(ctx, msg, true);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        msg.setMessageType(MessageType.CREATE_FOLDER);
                        StringBuilder remotePathBuilder = new StringBuilder((String) msg.getParametersMap().get(LOCAL_PATH));
                        if (!localPath.endsWith("/") && !localPath.endsWith("\\")){
                            remotePathBuilder.append("\\");
                        }
                        remotePathBuilder.append(localPath.getParent().relativize(dir).toString()).append("\\");
                        msg.getParametersMap().put(REMOTE_PATH, remotePathBuilder.toString());
                        logger.info("Составленный путь запрошенного каталога клиента: " + remotePathBuilder.toString());
                        try {
                            ctx.writeAndFlush(msg).sync();
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

            } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }


    private static void writeFileToChannel(ChannelHandlerContext ctx, ServiceMessage msg, boolean copyWithFolder) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH), msg.getAuthKey());

        if (!Files.exists(localPath)) {
            System.out.printf("Файл %1$s отсутствует или адрес указан не верно!\n", localPath.toString());
            sendServiceMessage(ctx, msg, String.format("Файл %1$s отсутствует или адрес указан не верно!\n", (String) msg.getParametersMap().get(REMOTE_PATH)));
            return;
        }

        Long part = null;
        Long parts = null;
        long size = 0;
        try {
            size = Files.size(localPath);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        //quantity of parts
        parts = (size % MAX_BYTE_ARRAY_SIZE > 0) ? size / MAX_BYTE_ARRAY_SIZE : size / MAX_BYTE_ARRAY_SIZE + 1;
        part = 0L;
        logger.info("Количество частей у файла размером {} байт равно {}", size, parts);
        ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_BYTE_ARRAY_SIZE);
        FileMessage fileMessage = new FileMessage(msg.authKey);

        try (FileChannel fileChannel = new RandomAccessFile(localPath.toFile(), "r").getChannel()) {
            int read;
            byte[] bytes;
            while ((read = fileChannel.read(byteBuffer)) != -1 || part == 0/*второе условие позволяет отправить пустой файл, например 1.txt без данных*/) {
                byteBuffer.flip();
                if(read ==-1)
                    read=0;//условие для создания массива с нулем байт, иначе ловится эксепшн
                bytes = new byte[read];
                byteBuffer.get(bytes);
                fileMessage.setParts(parts);
                fileMessage.setPart(part++);
                fileMessage.setBytes(bytes);
                if(copyWithFolder){
                    fileMessage.setRemotePath((String) msg.getParametersMap().get(LOCAL_PATH) + msg.getParametersMap().get(REMOTE_PATH));
                } else {
                    fileMessage.setRemotePath((String) msg.getParametersMap().get(LOCAL_PATH) + localPath.getFileName());
                }
                ctx.writeAndFlush(fileMessage);
                byteBuffer.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendServiceMessage(ChannelHandlerContext ctx, ServiceMessage msg, String text) {
        msg.setMessageType(MessageType.MESSAGE);
        msg.getParametersMap().put(MESSAGE, text);
        try {
            ctx.writeAndFlush(msg).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * Method calls by ServiceMessageHandler in case of files list request is came up. Wrap list of files in its paramsMap
     * and send is back to the client.
     * @param sm ServiceMessage which wraps List of files in its paramsMap HashMap.
     * */
    static void sendFilesListManager(ChannelHandlerContext ctx, ServiceMessage sm) {
        Path path = getLocalPath((String) sm.getParametersMap().get(REMOTE_PATH),sm.getAuthKey());

        if (!Files.exists(path)) {
            sendServiceMessage(ctx, sm, String.format("No %1$s folder!", (String) sm.getParametersMap().get(REMOTE_PATH)));
            return;
        }

        List<String> pathList = getFilesList(path); //if there aren't files in directory client are asking getFilesList(path) will return null!
        sm.getParametersMap().put(FILES_LIST, pathList);
        ctx.writeAndFlush(sm);
        }

    private static List<String> getFilesList(Path localPath) {
        List<String> filesList = null;
        try {
            filesList = Files.walk(localPath, 1, FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> !p.equals(localPath))
                    .map(p -> Files.isDirectory(p) ? p.getFileName().toString() + "\\": p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filesList;
    }
    /**
     * Method creates folder or folders by clients request in an individual client folder.
     * */
    static void createFolder(ChannelHandlerContext ctx, ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH), msg.getAuthKey());
        if(Files.exists(localPath)){
            msg.setMessageType(MessageType.MESSAGE);
            msg.getParametersMap().put(MESSAGE, new String("Folder " + msg.getParametersMap().get(REMOTE_PATH) + " already exist!"));
            try {
                ctx.writeAndFlush(msg).sync();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        } else {
            try {
                logger.info("Попытка создать папку по адресу " + localPath);
                Files.createDirectory(localPath);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Method removes files of folders by clients request in an individual client folder.
     * */
    static void remove(ChannelHandlerContext ctx, ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH), msg.getAuthKey());
        if (!Files.exists(localPath)) {
            sendServiceMessage(ctx, msg, String.format("Folder or file %1$s not exist!", (String) msg.getParametersMap().get(REMOTE_PATH)));
        } else {
            try {
                Files.walkFileTree(localPath, new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        sendServiceMessage(ctx, msg, "Files deleted!");
    }

    /**
     * Method removes files of folders by clients request in an individual client folder.
     * */
    static void createUserFolder(String login) {
        String userFolder = PersistanceBean.getUserFolderPath(login);
        if (Objects.isNull(userFolder)){
            try {
                throw new SQLException(String.format("Userfolder field fo user %1$s is empty!", login));
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
        Path userFolderPath = Paths.get(MainServer.getInstance().getSERVER_RELATIVE_PATH(),userFolder);
        if (!Files.exists(userFolderPath)) {
            try {
                Files.createDirectory(userFolderPath);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }
}
