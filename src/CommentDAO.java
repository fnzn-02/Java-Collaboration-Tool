import java.sql.*;
import java.util.*;

class CommentDAO {
	
    
    // 댓글 작성
    public int addComment(int taskId, int userId, String content) {
        String sql = "INSERT INTO comments (task_id, user_id, content) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, taskId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, content);
            
            if (pstmt.executeUpdate() > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    // 작업의 댓글 조회
    public List<CommentDTO> getTaskComments(int taskId) {
        String sql = "SELECT c.*, u.username " +
                     "FROM comments c " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "WHERE c.task_id = ? " +
                     "ORDER BY c.created_at ASC";
        
        List<CommentDTO> comments = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CommentDTO comment = new CommentDTO();
                comment.commentId = rs.getInt("comment_id");
                comment.taskId = rs.getInt("task_id");
                comment.userId = rs.getInt("user_id");
                comment.username = rs.getString("username");
                comment.content = rs.getString("content");
                comment.createdAt = rs.getTimestamp("created_at");
                comment.isEdited = rs.getBoolean("is_edited");
                comments.add(comment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }
    
    // 댓글 수정
    public boolean updateComment(int commentId, int userId, String content) {
        String sql = "UPDATE comments SET content = ?, is_edited = TRUE WHERE comment_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, content);
            pstmt.setInt(2, commentId);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // 댓글 삭제
    public boolean deleteComment(int commentId, int userId) {
        String sql = "DELETE FROM comments WHERE comment_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, commentId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}