package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

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
        if(fm.getBytes() == null) {
            throw new IOException("Байтовый массив is null!");
        }
        if(fm.getRemotePath() == null) {
            throw new IOException("Path is null!");
        }
    }

    public static void sendFileToClient(ChannelHandlerContext ctx, Map<String, Object> params) {
        Path localPath = Paths.get(SERVER_RELATIVE_PATH, (String) params.get(REMOTE_PATH));
            if (Files.isDirectory(localPath)) {
                System.out.println("Directory!");
                //sendStructure(ctx, params);
                return;
            }
            if(Files.exists(localPath)) {
                FileMessage fileMessage = new FileMessage();
                fileMessage.setRemotePath((String) params.get(LOCAL_PATH) + localPath.getFileName().toString());
                try {
                    fileMessage.setBytes(Files.readAllBytes(localPath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ctx.writeAndFlush(fileMessage);
            } else
                System.out.println("File not exist!");
    }

    static void getFromChannelToFile(FileMessage msg) {
        try {
            checkFileMessageDatNonNull(msg);
            Path localPath = getLocalPath(msg.getRemotePath());

            if(msg.getPart().equals(0L)) {
                Files.createFile(localPath);
                System.out.printf("Начинается копирование файла %s", localPath.getFileName());
            }

            System.out.println("Получена часть " +  msg.getPart() + " из " + msg.getParts());
            Files.write(localPath, msg.getBytes(), StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path getLocalPath(String localPath) {
        return Paths.get(SERVER_RELATIVE_PATH, localPath);
    }

    static void writeToChannel(ChannelHandlerContext ctx, ServiceMessage msg) {
        Path localPath = getLocalPath((String) msg.getParametersMap().get(REMOTE_PATH));
        if (!Files.exists(localPath)) {
            System.out.println("Файл отсутствует или адрес указан не верно!");
            return;
        }
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
        System.out.printf("Количество частей у файла размером %1$d байт равно %2$d", size, parts);
        ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_BYTE_ARRAY_SIZE);
        FileMessage fileMessage = new FileMessage();

        try(FileChannel fileChannel = new RandomAccessFile(localPath.toFile(),"r").getChannel()) {
            int read;
            byte[] bytes;
            while ((read = fileChannel.read(byteBuffer)) !=-1 || part==0) {
                byteBuffer.flip();

                bytes = new byte[read];
                byteBuffer.get(bytes);
                fileMessage.setParts(parts);
                fileMessage.setPart(part++);
                fileMessage.setBytes(bytes);
                fileMessage.setRemotePath((String) msg.getParametersMap().get(LOCAL_PATH));

                ctx.writeAndFlush(fileMessage);
                byteBuffer.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
