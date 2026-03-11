package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.net.Socket;
import java.io.*;
import java.util.HashMap;

public class ClientHandler extends Thread {

    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;

    String username;

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

                    if (data.length < 8) {
                        dos.writeUTF("REGISTER_FAIL");
                        continue;
                    }

                    String username = data[1];
                    String password = PasswordUtil.hashPassword(data[2]);
                    String fullname = data[3];
                    String email = data[4];
                    String phone = data[5];
                    String gender = data[6];
                    String birth = data[7];

                    try {

                        Connection conn = DatabaseConnection.getConnection();

                        // ===== CHECK USERNAME =====
                        String checkUser = "SELECT * FROM users WHERE username=?";
                        PreparedStatement psUser = conn.prepareStatement(checkUser);
                        psUser.setString(1, username);
                        ResultSet rsUser = psUser.executeQuery();

                        if (rsUser.next()) {
                            dos.writeUTF("USER_EXIST");
                            continue;
                        }

                        // ===== CHECK EMAIL =====
                        String checkEmail = "SELECT * FROM users WHERE email=?";
                        PreparedStatement psEmail = conn.prepareStatement(checkEmail);
                        psEmail.setString(1, email);
                        ResultSet rsEmail = psEmail.executeQuery();

                        if (rsEmail.next()) {
                            dos.writeUTF("EMAIL_EXIST");
                            continue;
                        }

                        // ===== CHECK PHONE =====
                        String checkPhone = "SELECT * FROM users WHERE phone=?";
                        PreparedStatement psPhone = conn.prepareStatement(checkPhone);
                        psPhone.setString(1, phone);
                        ResultSet rsPhone = psPhone.executeQuery();

                        if (rsPhone.next()) {
                            dos.writeUTF("PHONE_EXIST");
                            continue;
                        }

                        // ===== INSERT USER =====
                        String sql = "INSERT INTO users(username,password_hash,full_name,email,phone,gender,birth_date,is_active) VALUES (?,?,?,?,?,?,?,1)";

                        PreparedStatement ps = conn.prepareStatement(sql);

                        ps.setString(1, username);
                        ps.setString(2, password);
                        ps.setString(3, fullname);
                        ps.setString(4, email);
                        ps.setString(5, phone);
                        ps.setString(6, gender);
                        ps.setString(7, birth);

                        ps.executeUpdate();

                        dos.writeUTF("REGISTER_SUCCESS");

                    } catch (Exception ex) {

                        ex.printStackTrace();
                        dos.writeUTF("REGISTER_FAIL");

                    }
                } // ================= LOGIN =================
                else if (msg.startsWith("LOGIN")) {

                    String[] data = msg.split("\\|");

                    String u = data[1];
                    String p = PasswordUtil.hashPassword(data[2]);

                    boolean loginOK = false;

                    try {

                        Connection conn = DatabaseConnection.getConnection();

                        String sql = "SELECT * FROM users WHERE username=? AND password_hash=?";

                        PreparedStatement ps = conn.prepareStatement(sql);

                        ps.setString(1, u);
                        ps.setString(2, p);

                        ResultSet rs = ps.executeQuery();

                        if (rs.next()) {
                            loginOK = true;
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    if (loginOK) {

                        username = u;

                        Server.clients.put(username, this);

                        dos.writeUTF("LOGIN_SUCCESS");

                        broadcast(username + " đã vào phòng chat");

                        sendUserList();

                        loadHistory();

                    } else {

                        dos.writeUTF("LOGIN_FAIL");

                    }

                } // ================= CHAT CHUNG =================
                else if (msg.startsWith("MSG")) {

                    String text = msg.substring(4);

                    String send = username + ": " + text;

                    broadcast(send);

                    saveHistory(send);

                } // ================= CHAT RIÊNG =================
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

            if (!file.exists()) {
                return;
            }

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
