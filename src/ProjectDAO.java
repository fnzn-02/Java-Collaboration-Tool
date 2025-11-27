// ProjectDAO.java

import java.sql.*;
import java.util.*;

class ProjectDAO {

    // 프로젝트 생성
    public int createProject(String name, String description, int ownerId) {
        String sql = "INSERT INTO projects (project_name, description, owner_id) VALUES (?, ?, ?)";
        String memberSql = "INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, 'OWNER')";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setInt(3, ownerId);

            if (pstmt.executeUpdate() > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int projectId = rs.getInt(1);

                    try (PreparedStatement memberPstmt = conn.prepareStatement(memberSql)) {
                        memberPstmt.setInt(1, projectId);
                        memberPstmt.setInt(2, ownerId);
                        memberPstmt.executeUpdate();
                    }
                    return projectId;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 사용자가 속한 프로젝트 목록 조회
    public List<ProjectDTO> getUserProjects(int userId) {
        // 쿼리 단순화: project_members 테이블을 기준으로 조인하여 확실하게 가져옵니다.
        String sql = "SELECT p.project_id, p.project_name, p.description, p.owner_id, pm.role " +
                "FROM project_members pm " +
                "JOIN projects p ON pm.project_id = p.project_id " +
                "WHERE pm.user_id = ? " +
                "AND p.is_active = true " + // 활성화된 프로젝트만
                "ORDER BY p.created_at DESC";

        List<ProjectDTO> projects = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ProjectDTO project = new ProjectDTO();
                project.projectId = rs.getInt("project_id");
                project.projectName = rs.getString("project_name");
                project.description = rs.getString("description");
                project.ownerId = rs.getInt("owner_id");
                project.userRole = rs.getString("role");

                projects.add(project);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projects;
    }

    // 나머지 메서드들 (addMember, getProjectMembers)은 기존 코드 유지
    public boolean addMember(int projectId, int userId, String role) {
        String sql = "INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, ?)";
        String notificationSql = "INSERT INTO notifications (user_id, type, title, message, related_project_id) " +
                "VALUES (?, 'PROJECT_INVITED', '프로젝트에 초대되었습니다', ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, role);

            if (pstmt.executeUpdate() > 0) {
                try (PreparedStatement notifPstmt = conn.prepareStatement(notificationSql)) {
                    notifPstmt.setInt(1, userId);
                    notifPstmt.setString(2, "새 프로젝트에 초대되었습니다.");
                    notifPstmt.setInt(3, projectId);
                    notifPstmt.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<CollaborationServer.ProjectMemberInfo> getProjectMembers(int projectId) {
        String sql = "SELECT u.user_id, u.username, pm.role " +
                "FROM project_members pm " +
                "JOIN users u ON pm.user_id = u.user_id " +
                "WHERE pm.project_id = ?";

        List<CollaborationServer.ProjectMemberInfo> members = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                CollaborationServer.ProjectMemberInfo member = new CollaborationServer.ProjectMemberInfo();
                member.userId = rs.getInt("user_id");
                member.username = rs.getString("username");
                member.role = rs.getString("role");
                members.add(member);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }
}