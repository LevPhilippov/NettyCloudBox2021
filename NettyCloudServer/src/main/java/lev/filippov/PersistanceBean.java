package lev.filippov;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
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


    static {
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
            throwables.printStackTrace();
        }
    }

    public static boolean isUserExist(String login, String password) {
        try {
            ps=connection.prepareStatement(isUserExistStatement);
            ps.setString(1, login);
            ps.setString(2, password);
            return ps.execute();
        }catch (SQLException e){
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return answer;
    }

    private static void initializeClassVariables(){
        StringBuilder sqlQuery = new StringBuilder();

        try {
            FileReader fileReader = new FileReader("NettyCloudServer\\src\\main\\resources\\jdbc.construct");
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
            BufferedReader br = new BufferedReader(new FileReader("NettyCloudServer\\src\\main\\resources\\jdbc.properties"));
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
            e.printStackTrace();
        }

//        System.out.println(statement);
//        System.out.println(url);
//        System.out.println(user);
//        System.out.println(password);
//        System.out.println(initializeString);
    }


}
