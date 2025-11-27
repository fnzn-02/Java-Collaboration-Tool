import java.sql.*;
import java.util.*;

class NotificationDAO {
    
    // 사용자의 읽지 않은 알림 조회
    public List<NotificationDTO> getUnreadNotifications(int userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE " +
                     "ORDER BY created_at DESC LIMIT 50";
        
        List<NotificationDTO> notifications = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }
    
    // 알림 읽음 처리
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // 모든 알림 읽음 처리
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private NotificationDTO mapResultSetToNotification(ResultSet rs) throws SQLException {
        NotificationDTO notif = new NotificationDTO();
        notif.notificationId = rs.getInt("notification_id");
        notif.userId = rs.getInt("user_id");
        notif.type = rs.getString("type");
        notif.title = rs.getString("title");
        notif.message = rs.getString("message");
        notif.relatedTaskId = rs.getInt("related_task_id");
        notif.isRead = rs.getBoolean("is_read");
        notif.createdAt = rs.getTimestamp("created_at");
        return notif;
    }
}