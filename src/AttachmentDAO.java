import java.sql.*;
import java.util.*;

class AttachmentDAO {
    
    // 파일 첨부 정보 저장
    public int addAttachment(int taskId, int userId, String fileName, 
                            String filePath, long fileSize, String fileType) {
        String sql = "INSERT INTO attachments (task_id, uploaded_by, file_name, file_path, file_size, file_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, taskId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, fileName);
            pstmt.setString(4, filePath);
            pstmt.setLong(5, fileSize);
            pstmt.setString(6, fileType);
            
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
    
    // 작업의 첨부파일 목록
    public List<AttachmentDTO> getTaskAttachments(int taskId) {
        String sql = "SELECT a.*, u.username " +
                     "FROM attachments a " +
                     "JOIN users u ON a.uploaded_by = u.user_id " +
                     "WHERE a.task_id = ? " +
                     "ORDER BY a.uploaded_at DESC";
        
        List<AttachmentDTO> attachments = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                AttachmentDTO att = new AttachmentDTO();
                att.attachmentId = rs.getInt("attachment_id");
                att.taskId = rs.getInt("task_id");
                att.uploadedBy = rs.getInt("uploaded_by");
                att.uploaderName = rs.getString("username");
                att.fileName = rs.getString("file_name");
                att.filePath = rs.getString("file_path");
                att.fileSize = rs.getLong("file_size");
                att.fileType = rs.getString("file_type");
                att.uploadedAt = rs.getTimestamp("uploaded_at");
                attachments.add(att);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attachments;
    }

    // 파일 경로 조회를 위해 필요 (파일 삭제용)
    public AttachmentDTO getAttachmentById(int attachmentId) {
        String sql = "SELECT * FROM attachments WHERE attachment_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attachmentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                AttachmentDTO att = new AttachmentDTO();
                att.attachmentId = rs.getInt("attachment_id");
                att.taskId = rs.getInt("task_id");
                att.uploadedBy = rs.getInt("uploaded_by");
                // DB 컬럼명과 DTO 필드명이 일치해야 합니다.
                att.fileName = rs.getString("file_name");
                att.filePath = rs.getString("file_path");
                att.fileSize = rs.getLong("file_size");
                att.fileType = rs.getString("file_type");
                att.uploadedAt = rs.getTimestamp("uploaded_at");
                return att;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // 첨부파일 삭제 (ID로 삭제)
    public boolean deleteAttachment(int attachmentId) {
        String sql = "DELETE FROM attachments WHERE attachment_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, attachmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
