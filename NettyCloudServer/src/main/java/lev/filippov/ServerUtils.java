package lev.filippov;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static lev.filippov.Constants.*;

public class ServerUtils {

    public static void writeSmallFile(FileMessage fileMessage) {
        try {
            checkFileMessageDatNonNull(fileMessage);
            Path localPath = Paths.get(SERVER_RELATIVE_PATH, fileMessage.getRemotePath());
            System.out.println(localPath);
            Path directory = localPath.getParent();
//            Path directory = localPath.getParent();
            if(!Files.exists(directory));
                Files.createDirectories(directory);
//            Files.createFile(localPath);
            Files.write(localPath, fileMessage.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
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

}
