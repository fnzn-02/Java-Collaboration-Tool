import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {
    
    // 채팅 메시지 저장 (sender_id 사용)
    public boolean saveMessage(int userId, String message) {
        String sql = "INSERT INTO chat_messages (sender_id, message) VALUES (?, ?)";
        PreparedStatement pstmt = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setString(2, message);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.close(pstmt);
        }
    }
    
    // 최근 채팅 메시지 조회 (sender_id와 created_at 사용)
    public List<ChatMessageDTO> getRecentMessages(int limit) {
        String sql = "SELECT c.message_id, c.message, c.created_at, u.username " +
                     "FROM chat_messages c " +
                     "JOIN users u ON c.sender_id = u.user_id " +
                     "ORDER BY c.created_at DESC " +
                     "LIMIT ?";
        
        List<ChatMessageDTO> messages = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ChatMessageDTO msg = new ChatMessageDTO();
                msg.messageId = rs.getInt("message_id");
                msg.message = rs.getString("message");
                msg.sentAt = rs.getTimestamp("created_at");
                msg.username = rs.getString("username");
                
                messages.add(0, msg);
            }
            
            return messages;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return messages;
        } finally {
            DatabaseConfig.close(rs, pstmt);
        }
    }
}

// DTO 클래스
class ChatMessageDTO {
    public int messageId;
    public String message;
    public Timestamp sentAt;
    public String username;
}