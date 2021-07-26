package lev.filippov;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

import static lev.filippov.Constants.*;

public class ServerUtils {

//    public static void writeSmallFile(FileMessage fileMessage) {
//        try {
//            checkFileMessageDatNonNull(fileMessage);
//            Path localPath = Paths.get(SERVER_RELATIVE_PATH, fileMessage.getRemotePath());
//            System.out.println(localPath);
//            Path directory = localPath.getParent();
////            Path directory = localPath.getParent();
//            if(!Files.exists(directory));
//                Files.createDirectories(directory);
////            Files.createFile(localPath);
//            Files.write(localPath, fileMessage.getBytes(), StandardOpenOption.CREATE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private static void checkFileMessageDatNonNull(FileMessage fm) throws IOException {
        if (fm.getBytes() == null) {
            throw new IOException("Байтовый массив is null!");
        }
        if (fm.getRemotePath() == null) {
            throw new IOException("Path is null!");
        }
    }

    static void getFromChannelToFile(FileMessage msg) {
        try {
            checkFileMessageDatNonNull(msg);
            //TODO: Нужно бороться с конкатенацией, если не указана слэш
            Path localPath = getLocalPath(msg.getRemotePath());

            if (msg.getPart().equals(0L)) {
                if(!Files.exists(localPath.getParent()))
                    Files.createDirectories(localPath.getParent());
                Files.createFile(localPath);
                System.out.printf("Начинается копирование файла %s", localPath.getFileName());
            }

            System.out.println("Получена часть " + msg.getPart() + " из " + msg.getParts());
            Files.write(localPath, msg.getBytes(), StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path getLocalPath(String localPath) {
        return localPath.equals("root") ? Paths.get(SERVER_RELATIVE_PATH) : Paths.get(SERVER_RELATIVE_PATH, localPath);
    }


    static void writeToChannelManager(ChannelHandlerContext ctx, ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH));
        //если запрашивается файл
        if (!Files.isDirectory(localPath)) {
            writeFileToChannel(ctx, msg);
        }
        //если запрашивается каталог
        else {
            try {
                System.out.println("Исходный путь: " + localPath);
                Files.walkFileTree(localPath,new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        msg.getParametersMap().put(REMOTE_PATH, localPath.getParent().relativize(file).toString());
                        System.out.println("Relative path for " + file + " is " + msg.getParametersMap().get(REMOTE_PATH));
                        writeFileToChannel(ctx, msg);
                        return FileVisitResult.CONTINUE;
                    }
                });

            } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

//    private static void createFolderCommand(ChannelHandlerContext ctx, ServiceMessage msg) {
//        msg.setMessageType(MessageType.CREATE_FOLDER);
//    }

    private static void writeFileToChannel(ChannelHandlerContext ctx, ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH));

        if (!Files.exists(localPath)) {
            System.out.println("Файл отсутствует или адрес указан не верно!");
            return;
        }

        Long part = null;
        Long parts = null;
        long size = 0;
        try {
            size = Files.size(localPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //quantity of parts
        parts = (size % MAX_BYTE_ARRAY_SIZE > 0) ? size / MAX_BYTE_ARRAY_SIZE : size / MAX_BYTE_ARRAY_SIZE + 1;
        part = 0L;
        System.out.printf("Количество частей у файла размером %1$d байт равно %2$d", size, parts);
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
                fileMessage.setRemotePath((String) msg.getParametersMap().get(LOCAL_PATH) + msg.getParametersMap().get(REMOTE_PATH));
                ctx.writeAndFlush(fileMessage).sync();
                byteBuffer.clear();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sendFilesListManager(ChannelHandlerContext ctx, ServiceMessage sm) {
        Path path = getLocalPath((String) sm.getParametersMap().get(REMOTE_PATH));

        if (!Files.exists(path)) {
            sm.getParametersMap().put(MESSAGE, "No such folder!");
            sm.setMessageType(MessageType.MESSAGE);
            ctx.writeAndFlush(sm);
            return;
        }

        List<String> pathList = getFilesList(path);

        sm.getParametersMap().put(Constants.FILES_LIST, pathList);
        ctx.writeAndFlush(sm);
        }

    private static List<String> getFilesList(Path localPath) {

        List<String> filesList = null;

        try {
            filesList = Files.walk(localPath, 1, FileVisitOption.FOLLOW_LINKS)
                    .filter(p -> !p.equals(localPath))
                    .map(p -> Files.isDirectory(p) ? p.getFileName().toString() + "\\": p.getFileName().toString())
                    .collect(Collectors.toList());

            for (String s : filesList) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filesList;
    }

}
