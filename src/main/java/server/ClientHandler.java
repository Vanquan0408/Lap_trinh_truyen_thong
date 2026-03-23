package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.net.Socket;
import java.io.*;
import server.MessageDB;

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
                System.out.println("Server nhận: " + msg);

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

                        //loadHistory();
                    } else {

                        dos.writeUTF("LOGIN_FAIL");

                    }

                } // ================= CHAT CHUNG =================
                else if (msg.startsWith("MSG")) {
                    String text = msg.substring(4).trim();
                    String send = username + ": " + text;
                    broadcast(send);

                    // === THAY ĐOẠN NÀY ===
                    MessageDB.saveMessage(username, "ALL", text);
                } // ================= GỬI FILE RIÊNG =================
                else if (msg.startsWith("PRIVATE_FILE|")) {

                    String[] parts = msg.split("\\|");

                    String toUser = parts[1];
                    String fileName = parts[2];

                    int fileSize = dis.readInt();

                    byte[] buffer = new byte[fileSize];
                    dis.readFully(buffer);

                    ClientHandler target = Server.clients.get(toUser);

                    if (target != null) {

                        target.dos.writeUTF("PRIVATE_FILE_FROM|" + username + "|" + fileName);

                        target.dos.writeInt(fileSize);

                        target.dos.write(buffer);

                        target.dos.flush();

                        String log = username + " đã gửi file: " + fileName;
                        broadcast(log);
                    } else {

                        dos.writeUTF("HỆ THỐNG: Người dùng không online.");

                    }

                } // ================= GỬI FILE CHO TẤT CẢ =================
                else if (msg.startsWith("FILE|")) {

                    String[] parts = msg.split("\\|");

                    String fileName = parts[1];

                    int fileSize = dis.readInt();

                    byte[] buffer = new byte[fileSize];

                    dis.readFully(buffer);

                    for (ClientHandler c : Server.clients.values()) {

                        if (!c.username.equals(username)) {

                            try {

                                c.dos.writeUTF("FILE_FROM|" + username + "|" + fileName);

                                c.dos.writeInt(fileSize);

                                c.dos.write(buffer);

                                c.dos.flush();

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }

                    }

                } // ================= CHAT RIÊNG =================
                else if (msg.startsWith("PRIVATE|")) {
                    String[] data = msg.split("\\|", 3);
                    String toUser = data[1].trim();
                    String text = data[2].trim();

                    // lưu database trước
                    MessageDB.saveMessage(username, toUser, text);

                    ClientHandler target = Server.clients.get(toUser);

                    if (target != null) {

                        target.dos.writeUTF("PRIVATE_FROM|" + username + "|" + text);
                        target.dos.flush();

                    } else {

                        dos.writeUTF("HỆ THỐNG: Người dùng " + toUser + " hiện không online.");

                    }
                } // ================= CHAT NHÓM =================
                else if (msg.startsWith("GROUP|")) {

                    String[] data = msg.split("\\|", 3);

                    String groupId = data[1];
                    String text = data[2];

                    try {

                        // Lấy user_id
                        int senderId = MessageDB.getUserIdByUsername(username);

                        // ===== LƯU DATABASE =====
                        MessageDB.saveGroupMessage(senderId, Integer.parseInt(groupId), text);

                        // ===== LẤY DANH SÁCH MEMBER 1 LẦN =====
                        var members = MessageDB.getUsersInGroup(Integer.parseInt(groupId));

                        for (String member : members) {

                            ClientHandler c = Server.clients.get(member);

                            if (c != null) {
                                c.dos.writeUTF("GROUP_FROM|" + groupId + "|" + username + "|" + text);
                                c.dos.flush();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (msg.startsWith("LOAD_HISTORY|")) {

                    String toUser = msg.split("\\|")[1];

                    try {

                        Integer senderId = MessageDB.getUserIdByUsername(username);
                        Integer receiverId = MessageDB.getUserIdByUsername(toUser);

                        ResultSet rs = MessageDB.getPrivateChatHistory(senderId, receiverId);

                        while (rs.next()) {

                            int senderDB = rs.getInt("sender_id");
                            String content = rs.getString("content");

                            String senderName;

                            if (senderDB == senderId) {
                                senderName = username;
                            } else {
                                senderName = toUser;
                            }

                            dos.writeUTF("HISTORY|" + senderName + "|" + content);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } // ================= LOAD LỊCH SỬ NHÓM =================
                else if (msg.startsWith("LOAD_GROUP_HISTORY|")) {

                    String groupId = msg.split("\\|")[1];

                    try {

                        ResultSet rs = MessageDB.getGroupChatHistory(Integer.parseInt(groupId));

                        while (rs.next()) {

                            int senderId = rs.getInt("sender_id");
                            String content = rs.getString("content");

                            String senderName = MessageDB.getUsernameById(senderId);

                            dos.writeUTF("GROUP_HISTORY|" + senderName + "|" + content);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

        } catch (Exception e) {

            e.printStackTrace(); // THÊM DÒNG NÀY

            try {

                Server.clients.remove(username);

                broadcast(username + " đã rời phòng");

                sendUserList();

            } catch (Exception ex) {
                ex.printStackTrace();
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

}
