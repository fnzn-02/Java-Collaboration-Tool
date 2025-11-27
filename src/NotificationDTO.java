import java.sql.*;

class NotificationDTO {
    public int notificationId;
    public int userId;
    public String type;
    public String title;
    public String message;
    public int relatedTaskId;
    public boolean isRead;
    public Timestamp createdAt;
    
    public String serialize() {
        return notificationId + "|" + type + "|" + title + "|" + message + "|" + 
               relatedTaskId + "|" + isRead + "|" + createdAt.getTime();
    }
}