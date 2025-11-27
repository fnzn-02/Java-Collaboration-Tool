import java.sql.*;
import java.util.*;

class StatisticsDAO {

    // 프로젝트 통계
    public ProjectStatistics getProjectStatistics(int projectId) {
        System.out.println("[디버그] 프로젝트 ID " + projectId + "번 통계 계산 시작..."); // ★ 로그

        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status != '삭제') AS total_tasks, " +
                "(SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status = '완료') AS completed_tasks, " +
                "(SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status = '진행중') AS in_progress_tasks, " +
                "(SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status != '완료' AND status != '삭제' AND due_date < NOW()) AS overdue_tasks, "
                +
                "(SELECT COUNT(*) FROM project_members WHERE project_id = ?) AS member_count, " +
                "(SELECT COUNT(*) FROM comments c JOIN tasks t ON c.task_id = t.task_id WHERE t.project_id = ?) AS total_comments";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 6; i++) {
                pstmt.setInt(i, projectId);
            }

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ProjectStatistics stats = new ProjectStatistics();
                stats.projectId = projectId;
                stats.totalTasks = rs.getInt("total_tasks");
                stats.completedTasks = rs.getInt("completed_tasks");
                stats.inProgressTasks = rs.getInt("in_progress_tasks");
                stats.overdueTasks = rs.getInt("overdue_tasks");
                stats.memberCount = rs.getInt("member_count");
                stats.totalComments = rs.getInt("total_comments");

                System.out.println("[디버그] DB 조회 결과: 전체=" + stats.totalTasks +
                        ", 완료=" + stats.completedTasks +
                        ", 진행=" + stats.inProgressTasks);
                return stats;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 사용자 통계
    public UserStatistics getUserStatistics(int userId) {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND status != '삭제') AS assigned_tasks, " +
                "(SELECT COUNT(*) FROM tasks WHERE creator_id = ? AND status != '삭제') AS created_tasks, " +
                "(SELECT COUNT(*) FROM tasks WHERE completed_by_id = ?) AS completed_tasks, " +
                "(SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND status != '완료' AND status != '삭제' AND due_date < NOW()) AS overdue_tasks";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 4; i++) {
                pstmt.setInt(i, userId);
            }

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserStatistics stats = new UserStatistics();
                stats.userId = userId;
                stats.username = "";
                stats.assignedTasks = rs.getInt("assigned_tasks");
                stats.createdTasks = rs.getInt("created_tasks");
                stats.completedTasks = rs.getInt("completed_tasks");
                stats.overdueTasks = rs.getInt("overdue_tasks");
                return stats;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}