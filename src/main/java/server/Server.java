package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

    public static HashMap<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) {

        try {

            ServerSocket server = new ServerSocket(9999);

            System.out.println("Server started...");

            while (true) {

                Socket socket = server.accept();

                ClientHandler client = new ClientHandler(socket);

                client.start();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}