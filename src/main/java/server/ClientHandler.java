package server;

import java.net.Socket;
import java.io.*;
import java.util.HashMap;

public class ClientHandler extends Thread {

    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;

    String username;

    // lưu tài khoản
    static HashMap<String, String> users = new HashMap<>();

    public ClientHandler(Socket socket) {

        this.socket = socket;

        try {

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void run() {

        try {

            while (true) {

                String msg = dis.readUTF();

                // ================= REGISTER =================
                if (msg.startsWith("REGISTER")) {

                    String[] data = msg.split("\\|");

                    String u = data[1];
                    String p = data[2];

                    if (users.containsKey(u)) {

                        dos.writeUTF("REGISTER_FAIL");

                    } else {

                        users.put(u, p);

                        dos.writeUTF("REGISTER_SUCCESS");

                    }

                }

                // ================= LOGIN =================
                else if (msg.startsWith("LOGIN")) {

                    String[] data = msg.split("\\|");

                    String u = data[1];
                    String p = data[2];

                    if (users.containsKey(u) && users.get(u).equals(p)) {

                        username = u;

                        Server.clients.put(username, this);

                        dos.writeUTF("LOGIN_SUCCESS");

                        broadcast(username + " đã vào phòng chat");

                        sendUserList();

                        loadHistory();

                    } else {

                        dos.writeUTF("LOGIN_FAIL");

                    }

                }

                // ================= CHAT CHUNG =================
                else if (msg.startsWith("MSG")) {

                    String text = msg.substring(4);

                    String send = username + ": " + text;

                    broadcast(send);

                    saveHistory(send);

                }

                // ================= CHAT RIÊNG =================
// ================= CHAT RIÊNG =================
                else if (msg.startsWith("PRIVATE")) {
                    String[] data = msg.split("\\|", 3); // Split thành 3 phần: PRIVATE | người_nhận | nội_dung

                    String toUser = data[1];
                    String text = data[2];

                    ClientHandler target = Server.clients.get(toUser);

                    if (target != null) {
                        // QUAN TRỌNG: Gửi theo định dạng chuẩn để Client dễ xử lý
                        // Định dạng: PRIVATE_FROM | tên_người_gửi | nội_dung
                        target.dos.writeUTF("PRIVATE_FROM|" + username + "|" + text);
                    } else {
                        // Gửi thông báo lại cho người gửi nếu target không online (tùy chọn)
                        dos.writeUTF("HỆ THỐNG: Người dùng " + toUser + " hiện không online.");
                    }
                }

            }

        } catch (Exception e) {

            try {

                Server.clients.remove(username);

                broadcast(username + " đã rời phòng");

                sendUserList();

            } catch (Exception ex) {
            }

        }

    }

    // ================= GỬI CHO TẤT CẢ =================
    void broadcast(String msg) {

        try {

            for (ClientHandler c : Server.clients.values()) {

                c.dos.writeUTF(msg);

            }

        } catch (Exception e) {
        }

    }

    // ================= DANH SÁCH USER ONLINE =================
    void sendUserList() {

        try {

            String list = "USERS|";

            for (String u : Server.clients.keySet()) {

                list += u + ",";

            }

            for (ClientHandler c : Server.clients.values()) {

                c.dos.writeUTF(list);

            }

        } catch (Exception e) {
        }

    }

    // ================= LƯU LỊCH SỬ CHAT =================
    void saveHistory(String msg) {

        try {

            FileWriter fw = new FileWriter("chat.txt", true);

            fw.write(msg + "\n");

            fw.close();

        } catch (Exception e) {
        }

    }

    // ================= LOAD LỊCH SỬ =================
    void loadHistory() {

        try {

            File file = new File("chat.txt");

            if (!file.exists())
                return;

            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;

            while ((line = br.readLine()) != null) {

                dos.writeUTF(line);

            }

            br.close();

        } catch (Exception e) {
        }

    }

}