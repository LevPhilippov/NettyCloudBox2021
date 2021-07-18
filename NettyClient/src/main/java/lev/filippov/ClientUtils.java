package lev.filippov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static lev.filippov.Constants.*;

public class ClientUtils {

    public static void writeSmallFile(FileMessage fileMessage) {
        try {
            checkFileMessageDatNonNull(fileMessage);
            Path localPath = Paths.get(CLIENT_RELATIVE_PATH, fileMessage.getRemotePath());
            System.out.println(localPath);
            Path directory = localPath.getParent();
//            Path directory = localPath.getParent();
            if(!Files.exists(directory))
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


}
