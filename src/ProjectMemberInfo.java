public class ProjectMemberInfo {
    public int userId;
    public String username;
    public String role;
    
    public ProjectMemberInfo() {}
    
    public ProjectMemberInfo(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
}