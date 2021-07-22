package lev.filippov;

import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static lev.filippov.Constants.*;

public class ClientUtils {
        private static final Logger logger = LogManager.getLogger(ClientUtils.class.getName());

//    public static void writeSmallFile(FileMessage fileMessage) {
//        try {
//            checkFileMessageDatNonNull(fileMessage);
//            Path localPath = Paths.get(CLIENT_RELATIVE_PATH, fileMessage.getRemotePath());
//            System.out.println(localPath);
//            Path directory = localPath.getParent();
////            Path directory = localPath.getParent();
//            if(!Files.exists(directory))
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


    static void writeToChannel(ObjectEncoderOutputStream encoder, Path localPath, String remotePath) {
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
                    fileMessage.setRemotePath(remotePath);

                    encoder.writeObject(fileMessage);
                    encoder.flush();
                    byteBuffer.clear();
                }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void getFromChannelToFile(FileMessage msg) {
        try {
            checkFileMessageDatNonNull(msg);
            Path localPath = getLocalPath(msg.getRemotePath());

            if(msg.getPart().equals(0L)) {
                Files.createFile(localPath);
                logger.info("Начинается копирование файла {}", localPath.getFileName());
//                System.out.printf("Начинается копирование файла %s", localPath.getFileName());
            }
                logger.info("Получена часть {} из {} ", msg.getPart() , msg.getParts());
            System.out.println("Получена часть " +  msg.getPart() + " из " + msg.getParts());
            Files.write(localPath, msg.getBytes(), StandardOpenOption.APPEND);

            if(msg.getPart().equals(msg.getParts())){
                System.out.println("Copying completed!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path getLocalPath(String remotePath) {
        return Paths.get(CLIENT_RELATIVE_PATH, remotePath);
    }

}
