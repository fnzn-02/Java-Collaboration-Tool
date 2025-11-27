import java.sql.*;

class TagDTO {
    public int tagId;
    public String tagName;
    public String color;
    public int projectId;
    
    public String serialize() {
        return tagId + "|" + tagName + "|" + color;
    }
}