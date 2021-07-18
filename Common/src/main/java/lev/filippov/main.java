package lev.filippov;

import java.io.File;
import java.io.IOException;

public class main {
    public static void main(String[] args) throws IOException {
        File file = new File("2.txt");
        System.out.println(file.getAbsolutePath());
        file.createNewFile();
    }
}
