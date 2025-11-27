import java.sql.*;
import java.util.*;

class TaskDTO {
    public int taskId;
    public int projectId;
    public String title;
    public String description;
    public String priority;
    public String status;
    public String creator;
    public String assignee;
    public String completedBy;
    public Timestamp createdAt;
    public Timestamp completedAt;
    public Timestamp dueDate;
    public boolean isOverdue;
    public List<TagDTO> tags = new ArrayList<>();
    
    public String serialize() {
        return taskId + "|" + title + "|" + 
               (description != null ? description : "") + "|" + 
               priority + "|" + status + "|" + creator + "|" + 
               (assignee != null ? assignee : "") + "|" +
               (completedBy != null ? completedBy : "") + "|" + 
               createdAt.getTime() + "|" + 
               (completedAt != null ? completedAt.getTime() : "0") + "|" +
               (dueDate != null ? dueDate.getTime() : "0") + "|" +
               isOverdue;
    }
}