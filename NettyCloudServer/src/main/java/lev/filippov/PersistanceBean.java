package lev.filippov;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Paths;
import java.sql.*;

public class PersistanceBean {

    private static Connection connection;
    private static PreparedStatement ps;
    private static String url;
    private static String user;
    private static String password;
    private static String isUserExistStatement;
    private static String getUserFolderStatementByLogin;
    private static String initializeString;
    private static final Logger logger;
//    private static String jdbcConstructPath = "NettyCloudServer\\src\\main\\resources\\jdbc.construct";
//    private static String jdbcPropertiesPath = "NettyCloudServer\\src\\main\\resources\\jdbc.properties";
    private static String jdbcConstructPath = "setting\\jdbc.construct";
    private static String jdbcPropertiesPath = "setting\\jdbc.properties";


    static {
        logger = LogManager.getLogger(PersistanceBean.class.getName());
        initializeClassVariables();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(url,user,password);
            connection.createStatement().execute(initializeString);
        } catch (SQLException throwables) {
            logger.error(throwables.getMessage());
        }
    }

    public static boolean isUserExist(String login, String password) {
        try {
            ps=connection.prepareStatement(isUserExistStatement);
            ps.setString(1, login);
            ps.setString(2, password);
            return ps.execute();
        }catch (SQLException e){
            logger.error(e.getMessage());
        }
        return false;
    }

    public static String getUserFolderPath(String login) {
        String answer=null;
        try {
            ps = connection.prepareStatement(getUserFolderStatementByLogin);
            ps.setString(1,login);
            ResultSet resultSet = ps.executeQuery();
            resultSet.next();
            answer = resultSet.getString("userfolder");
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return answer;
    }

    private static void initializeClassVariables(){
        StringBuilder sqlQuery = new StringBuilder();

        try {
            FileReader fileReader = new FileReader(jdbcConstructPath);
            int i;
            CharBuffer buffer = CharBuffer.allocate(50);
            while (fileReader.read(buffer) !=-1) {
                buffer.flip();
                sqlQuery.append(buffer);
                buffer.clear();
            }
            initializeString = sqlQuery.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(jdbcPropertiesPath));
            String line;
            while((line = br.readLine())!=null) {
                String[] tokens = line.split("\\s",2);

                switch (tokens[0]){
                    case "user":
                        user = tokens[1];
                        break;
                    case"password":
                        password = tokens[1];
                        break;
                    case "url":
                        url = tokens[1];
                        break;
                    case "preparedStatement1":
                        isUserExistStatement = tokens[1];
                        break;
                    case "preparedStatement2":
                        getUserFolderStatementByLogin = tokens[1];
                        break;
                    default:
                        System.out.println("Unknown variable");
                        break;
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    public static void printCurrentSettings(){
        System.out.println("-----Settings-----");
        System.out.printf("System construction file path is %s\n", Paths.get(jdbcConstructPath).toAbsolutePath());
        System.out.printf("System properties file path is %s\n", Paths.get(jdbcPropertiesPath).toAbsolutePath());
        System.out.printf("System path is %s\n", Paths.get(jdbcConstructPath).toAbsolutePath());
        System.out.printf("JDBC user is %s\n", user);
        System.out.printf("JDBC password is %s\n", password);
        System.out.printf("JDBC url is %s\n", url);
        System.out.printf("JDBC reconstruction data:\n %s\n", initializeString);

    }


}
