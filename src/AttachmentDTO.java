import java.sql.*;



class AttachmentDTO {
    public int attachmentId;
    public int taskId;
    public int uploadedBy;
    public String uploaderName;
    public String fileName;
    public String filePath;
    public long fileSize;
    public String fileType;
    public Timestamp uploadedAt;
}