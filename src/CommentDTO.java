import java.sql.*;

class CommentDTO {
    public int commentId;
    public int taskId;
    public int userId;
    public String username;
    public String content;
    public boolean isEdited;
    public Timestamp createdAt;
    
    public String serialize() {
        return commentId + "|" + username + "|" + content + "|" + 
               isEdited + "|" + createdAt.getTime();
    }
}