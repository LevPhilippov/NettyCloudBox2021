package lev.filippov;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        encoder = new ObjectEncoderOutputStream(socket.getOutputStream(), 1024);
        decoder = new ObjectDecoderInputStream(socket.getInputStream());
        threadPool.execute(() -> {
            while(true) {
                try {
                    Object msg = decoder.readObject();
                    if (msg instanceof String)
                        System.out.println((String) msg);
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
                    String a = reader.readLine();
                    System.out.println(a);
                    encoder.writeObject(a);
                    encoder.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
