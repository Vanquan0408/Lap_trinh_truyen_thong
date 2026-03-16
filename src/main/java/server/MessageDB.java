package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class MessageDB {

    // ================= LẤY ID TỪ USERNAME =================
    public static Integer getUserIdByUsername(String username) {

        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ================= LƯU TIN NHẮN CHÍNH =================
    public static boolean saveMessage(Integer senderId, Integer receiverId, String content,
            boolean isFile, String fileName, String filePath) {

        if (senderId == null) {
            return false;
        }

        String sql = "INSERT INTO messages(sender_id, receiver_id, content, sent_at, is_file, file_name, file_path) "
                + "VALUES (?, ?, ?, NOW(), ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, senderId);

            if (receiverId != null) {
                ps.setInt(2, receiverId);
            } else {
                ps.setNull(2, Types.INTEGER);   // chat phòng chung
            }

            ps.setString(3, content);
            ps.setBoolean(4, isFile);
            ps.setString(5, fileName);
            ps.setString(6, filePath);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= HÀM DỄ DÙNG =================
    public static void saveMessage(String senderUsername, String receiverUsernameOrAll, String content) {

        Integer senderId = getUserIdByUsername(senderUsername);

        if (senderId == null) {
            System.err.println("Không tìm thấy ID của " + senderUsername);
            return;
        }

        Integer receiverId = null;

        if (!"ALL".equalsIgnoreCase(receiverUsernameOrAll)) {
            receiverId = getUserIdByUsername(receiverUsernameOrAll);
        }

        boolean saved = saveMessage(senderId, receiverId, content, false, null, null);

        if (saved) {
            System.out.println("Đã lưu tin nhắn vào DB");
        } else {
            System.err.println("Lưu tin nhắn thất bại");
        }
    }

    public static ResultSet getPrivateChatHistory(int user1, int user2) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT * FROM messages WHERE "
                    + "(sender_id = ? AND receiver_id = ?) "
                    + "OR (sender_id = ? AND receiver_id = ?) "
                    + "ORDER BY sent_at ASC";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, user1);
            ps.setInt(2, user2);
            ps.setInt(3, user2);
            ps.setInt(4, user1);

            return ps.executeQuery();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
