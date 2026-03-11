package server;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    public static Connection getConnection() {

        Connection conn = null;

        try {

            String url = "jdbc:mysql://localhost:3306/chat_socket_lttt";
            String user = "root";
            String password = "";

            conn = DriverManager.getConnection(url, user, password);

            System.out.println("Kết nối MySQL thành công!");

        } catch (Exception e) {

            System.out.println("Kết nối MySQL thất bại!");
            e.printStackTrace();

        }

        return conn;
    }

    public static void main(String[] args) {

        Connection conn = getConnection();

        if (conn != null) {
            System.out.println("Database đã kết nối!");
        }

    }
}