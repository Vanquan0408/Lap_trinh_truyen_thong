package server;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class MessageDB {

    public static void saveMessage(String sender, String receiver, String content) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO messages(sender_id, receiver_id, content, sent_at, is_file) VALUES (?, ?, ?, NOW(), 0)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);

            ps.executeUpdate();

            System.out.println("Đã lưu tin nhắn vào database");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}