package lev.filippov;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static lev.filippov.Constants.*;

public class Network {
    private Socket socket;
    private ObjectEncoderOutputStream encoder;
    private ObjectDecoderInputStream decoder;
    private ExecutorService threadPool = Executors.newCachedThreadPool();


    public static void main(String[] args) throws IOException {
        new Network(new Socket("localhost", 8189)).start();
    }
    public Network(Socket socket) {
        this.socket = socket;
    }

    public void start() throws IOException {
        encoder = new ObjectEncoderOutputStream(socket.getOutputStream(), Message.MAX_MESSAGE_SIZE);
        decoder = new ObjectDecoderInputStream(socket.getInputStream());
        threadPool.execute(() -> {
            while(true) {
                try {
                    Object msg = decoder.readObject();
                    if (msg instanceof String)
                        System.out.println((String) msg);
                    if(msg instanceof FileMessage) {
                        ClientUtils.writeSmallFile((FileMessage)msg);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
        threadPool.execute(() -> {
            try(InputStreamReader isr = new InputStreamReader(System.in, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr))
            {
                while (true) {
                    String line = reader.readLine();
                    if(line.startsWith("get")) {
                        ServiceMessage sm = new ServiceMessage();
                        sm.setMessageType(MessageType.GET_FILE);
                        sm.getParametersMap().put(LOCAL_PATH, "\\fold\\");
                        sm.getParametersMap().put(REMOTE_PATH, "fold\\fol2\\folder\\2.txt" );
                        encoder.writeObject(sm);
                        encoder.flush();
                        continue;
                    }
                    Path filePath = Paths.get(line);
                    if (!Files.exists(filePath)) {
                        System.out.println("Файл отсутствует или адрес указан не верно!");
                        continue;
                    }
                    if (Files.size(filePath) <= Message.MAX_MESSAGE_SIZE) {
                        FileMessage msg = new FileMessage();
                        msg.setBytes(Files.readAllBytes(filePath));
                        msg.setRemotePath("fold\\fol2\\folder\\2.txt");
                        encoder.writeObject(msg);
                        encoder.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
