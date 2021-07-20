package lev.filippov;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        encoder = new ObjectEncoderOutputStream(socket.getOutputStream(), MAX_MESSAGE_SIZE);
        decoder = new ObjectDecoderInputStream(socket.getInputStream());
        threadPool.execute(() -> {
            while(true) {
                try {
                    Object msg = decoder.readObject();
                    if (msg instanceof String)
                        System.out.println((String) msg);
                    if(msg instanceof FileMessage) {
                        ClientUtils.getFromChannelToFile((FileMessage) msg);
                    } else {
                        System.out.println(msg);
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
                    //bloking oparetion
                    String line = reader.readLine();

                    if(line.startsWith("get")) {
                        ServiceMessage sm = new ServiceMessage();
                        sm.setMessageType(MessageType.GET_FILE);
                        sm.getParametersMap().put(LOCAL_PATH, "fold/doc0.pdf");
                        sm.getParametersMap().put(REMOTE_PATH, "doc0.pdf" );
                        encoder.writeObject(sm);
                        encoder.flush();
                    } else  {
                                ClientUtils.writeToChannel(encoder, Paths.get(line), Paths.get(line).getFileName().toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
