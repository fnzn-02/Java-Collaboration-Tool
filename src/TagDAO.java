import java.sql.*;
import java.util.*;

class TagDAO {
    
    // 태그 생성
    public int createTag(String tagName, String color, int projectId) {
        String sql = "INSERT INTO tags (tag_name, color, project_id) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tagName);
            pstmt.setString(2, color);
            pstmt.setInt(3, projectId);
            
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
    
    // 프로젝트의 태그 목록
    public List<TagDTO> getProjectTags(int projectId) {
        String sql = "SELECT * FROM tags WHERE project_id = ? ORDER BY tag_name";
        List<TagDTO> tags = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                TagDTO tag = new TagDTO();
                tag.tagId = rs.getInt("tag_id");
                tag.tagName = rs.getString("tag_name");
                tag.color = rs.getString("color");
                tag.projectId = rs.getInt("project_id");
                tags.add(tag);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }
    
    // 작업에 태그 추가
    public boolean addTagToTask(int taskId, int tagId) {
        String sql = "INSERT INTO task_tags (task_id, tag_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.setInt(2, tagId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // 작업의 태그 조회
    public List<TagDTO> getTaskTags(int taskId) {
        String sql = "SELECT t.* FROM tags t " +
                     "JOIN task_tags tt ON t.tag_id = tt.tag_id " +
                     "WHERE tt.task_id = ?";
        List<TagDTO> tags = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                TagDTO tag = new TagDTO();
                tag.tagId = rs.getInt("tag_id");
                tag.tagName = rs.getString("tag_name");
                tag.color = rs.getString("color");
                tags.add(tag);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }
}