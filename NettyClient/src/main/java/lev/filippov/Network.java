package lev.filippov;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static lev.filippov.Constants.*;
import org.apache.commons.codec.digest.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Network {
    private Logger logger;
    private final String url;
    private final int port;
    private Socket socket;
    private ObjectEncoderOutputStream encoder;
    private ObjectDecoderInputStream decoder;
    private ExecutorService threadPool;
    private AuthKey authKey;
    private String currentRemoteFolderPath;


    public static void main(String[] args) throws IOException {
        new Network("localhost", 8189).startClient();
    }

    public Network(String url, int port) {
        this.currentRemoteFolderPath = "";
        this.logger = LogManager.getLogger(this.getClass().getName());
        this.url = url;
        this.port = port;
    }

    public void startClient() {
//        System.out.println(DigestUtils.md5Hex("12345"));
        threadPool = Executors.newCachedThreadPool();
        threadPool.execute(this::startCommandLine);
        logger.trace("Client app was launched.");
        startNetwork();
    }

    private void startNetwork()  {
        try {
            socket = new Socket(url, port);
            encoder = new ObjectEncoderOutputStream(socket.getOutputStream(), MAX_MESSAGE_SIZE);
            decoder = new ObjectDecoderInputStream(socket.getInputStream());
            logger.info("Network and streams was initialized.");
            autoLogIn();
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
        threadPool.execute(this::startReadingInputStream);
        logger.info("App is ready to handle incoming data.");
        System.out.println("Command line is runned! For authorization in system please type: auth login [login] pass [password] where words " +
                "with brackest are your login and password in the system!");
    }

    private void autoLogIn() throws IOException {
        AuthKey authKey = new AuthKey();
        authKey.setLogin("qwerty");
        authKey.setPassword(DigestUtils.md5Hex("12345"));
        encoder.writeObject(authKey);
        encoder.flush();
    }


    private void startReadingInputStream() {
        while(true) {
            if(threadPool.isShutdown())
                break;
            try {
                Object msg = decoder.readObject();

                if (msg instanceof AuthKey){
                    authKey = (AuthKey) msg;
                    logger.info("Получен валидный экземпляр AuthKey.");
                }
                if (msg instanceof ServiceMessage) {
                    ServiceMessage sm = (ServiceMessage) msg;
                    switch (sm.getMessageType()){
                        case GET_STRUCTURE:
                            currentRemoteFolderPath = (String) sm.getParametersMap().get(REMOTE_PATH);
                            List<String> filesList = (List<String>) sm.getParametersMap().get(FILES_LIST);
//                            List<String> filesList = (List<String>) sm.getParametersMap().get(FILES_LIST);
                            System.out.print("Current folder is: " + currentRemoteFolderPath + "\n");
                            for (String s : filesList) {
                                System.out.println(s);
                            }
                            break;
                        default:
                            System.out.println("Unknown message format!");
                            break;
                    }
                }
                if(msg instanceof FileMessage) {
                    ClientUtils.getFromChannelToFile((FileMessage) msg);
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                if(!threadPool.isShutdown())
                    shutdown();
            }
        }
    }

    private void startCommandLine() {
        //TODO: сделать рефактор на читаемый код
            try(InputStreamReader isr = new InputStreamReader(System.in, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr))
            {
                while (true) {
                    //loop breaking operation
                    if( threadPool.isShutdown())
                        break;
                    //bloking oparetion
                    String line = reader.readLine();
                    //when not authorized in a system
                    if(authKey == null) {
                        //encounter only auth query
                        if(line.startsWith("auth")) {
                            logger.info("Получена команда на авторизацию.");
                            AuthKey tempAuthKey = new AuthKey();
                            String[] strings = line.split("\\s");
                            Iterator<String> iter = Arrays.stream(strings).iterator();
                            String prev=null;
                            String cur=null;
                            while (iter.hasNext()) {
                                cur = iter.next();
                                if (prev != null) {
                                    if (prev.equals("login") && tempAuthKey.getLogin() == null){
                                        tempAuthKey.setLogin(cur);
                                    }
                                    if (prev.equals("pass") && tempAuthKey.getPassword() == null){
                                        tempAuthKey.setPassword(DigestUtils.md5Hex(cur));
                                    }
                                }
                                prev=cur;
                            }
                            if (tempAuthKey.getLogin() != null && tempAuthKey.getPassword()!= null) {
                                encoder.writeObject(tempAuthKey);
                                encoder.flush();
                                logger.info("Отправлены данные авторизации.");
                            }
                        }
                        //encounter shutdown
                        else if (line.startsWith("shutdown")) {
                            shutdown();
                        }
                    } /*when you already authorized in a system*/
                    else {
                        String[] tokens = line.split("\\s");

                        //TODO: составить запрос на отправку файлов в каталоге
                        if (tokens[0].equals("dl")) {
                            ServiceMessage sm = new ServiceMessage(authKey);
                            sm.setMessageType(MessageType.GET_STRUCTURE);
                            if (tokens.length == 2) {
                                sm.getParametersMap().put(REMOTE_PATH,tokens[1]);
                            } else {
                                sm.getParametersMap().put(REMOTE_PATH, currentRemoteFolderPath);
                            }
                            encoder.writeObject(sm);
                            encoder.flush();
                        } else if(tokens[0].equals("get")) {
                            ServiceMessage sm = new ServiceMessage(authKey);
                            sm.setMessageType(MessageType.GET_FILE);
                            sm.getParametersMap().put(LOCAL_PATH, "");
                            sm.getParametersMap().put(REMOTE_PATH, tokens[1]);
                            encoder.writeObject(sm);
                            encoder.flush();
                        }  else if (line.startsWith("send"))   {
                            ClientUtils.writeToChannel(encoder, Paths.get(line), Paths.get(line).getFileName().toString());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void shutdown() {
        logger.info("App is closing.");
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        try {
//            if(!threadPool.awaitTermination(5, TimeUnit.SECONDS)){
//                List<Runnable> runnableList = threadPool.shutdownNow();
//                for (Runnable runnable : runnableList) {
//                    logger.debug("Следующий поток не может завершиться: " + runnable);
//                }
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        try {
            if(encoder != null)
                encoder.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            if(decoder != null)
                decoder.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
