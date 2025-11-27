import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

class TaskDAO {

	// 담당자 지정하여 작업 추가
	public int addTask(String title, String description, String priority, int creatorId, Integer assigneeId,
	        Timestamp dueDate, int projectId) {
	    String sql = "INSERT INTO tasks (title, description, priority, creator_id, assignee_id, due_date, project_id) " +
	                 "VALUES (?, ?, ?, ?, ?, ?, ?)";
	    String historySql = "INSERT INTO task_history (task_id, user_id, action) VALUES (?, ?, '생성')";  

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

	        pstmt.setString(1, title);
	        pstmt.setString(2, description);
	        pstmt.setString(3, priority);
	        pstmt.setInt(4, creatorId);
	        if (assigneeId != null) {
	            pstmt.setInt(5, assigneeId);
	        } else {
	            pstmt.setNull(5, Types.INTEGER);
	        }
	        if (dueDate != null) {
	            pstmt.setTimestamp(6, dueDate);
	        } else {
	            pstmt.setNull(6, Types.TIMESTAMP);
	        }
	        pstmt.setInt(7, projectId);

	        int result = pstmt.executeUpdate();
	        if (result > 0) {
	            ResultSet rs = pstmt.getGeneratedKeys();
	            if (rs.next()) {
	                int taskId = rs.getInt(1);

	                try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
	                    historyPstmt.setInt(1, taskId);
	                    historyPstmt.setInt(2, creatorId);
	                    historyPstmt.executeUpdate();
	                }

	                return taskId;
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return -1;
	}

	// 특정 작업 조회
	public TaskDTO getTaskById(int taskId) {
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue " + "FROM tasks t "
				+ "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id " + "WHERE t.task_id = ?";

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, taskId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return mapResultSetToTask(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// 모든 활성 작업 조회
	public List<TaskDTO> getAllActiveTasks() {
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue " + "FROM tasks t "
				+ "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id " + "WHERE t.status != '삭제' "
				+ "ORDER BY t.created_at DESC";

		List<TaskDTO> tasks = new ArrayList<>();

		try (Connection conn = DatabaseConfig.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				tasks.add(mapResultSetToTask(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tasks;
	}

	// 대기 여부 상관없이 바로 진행중으로 변경
	public boolean startTask(int taskId, int userId) {
	    String sql = "UPDATE tasks SET status = '진행중' WHERE task_id = ?";
	    String historySql = "INSERT INTO task_history (task_id, user_id, action) VALUES (?, ?, '진행')";  

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, taskId);
	        int result = pstmt.executeUpdate();

	        if (result > 0) {
	            try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
	                historyPstmt.setInt(1, taskId);
	                historyPstmt.setInt(2, userId);
	                historyPstmt.executeUpdate();
	            }
	            System.out.println("[DB] 작업 시작: ID " + taskId);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}


	// 작업 완료 메서드 추가
	public boolean completeTask(int taskId, int userId) {
	    // WHERE 조건에서 status 체크를 제거 (모든 상태에서 완료 가능)
	    String sql = "UPDATE tasks SET status = '완료', completed_by_id = ?, completed_at = NOW() " +
	                 "WHERE task_id = ?";
	    String historySql = "INSERT INTO task_history (task_id, user_id, action) VALUES (?, ?, '완료')";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, userId);
	        pstmt.setInt(2, taskId);
	        int result = pstmt.executeUpdate();

	        if (result > 0) {
	            try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
	                historyPstmt.setInt(1, taskId);
	                historyPstmt.setInt(2, userId);
	                historyPstmt.executeUpdate();
	            }
	            System.out.println("[DB] 작업 완료: ID " + taskId);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	// 작업 삭제 메서드 추가
	public boolean deleteTask(int taskId, int userId) {
	    String sql = "UPDATE tasks SET status = '삭제' WHERE task_id = ?";
	    String historySql = "INSERT INTO task_history (task_id, user_id, action) VALUES (?, ?, '삭제')";  

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, taskId);
	        int result = pstmt.executeUpdate();

	        if (result > 0) {
	            try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
	                historyPstmt.setInt(1, taskId);
	                historyPstmt.setInt(2, userId);
	                historyPstmt.executeUpdate();
	            }
	            System.out.println("[DB] 작업 삭제: ID " + taskId);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	// 담당자 변경
	public boolean assignTask(int taskId, int assigneeId, int changedBy) {
	    String sql = "UPDATE tasks SET assignee_id = ? WHERE task_id = ?";
	    String historySql = "INSERT INTO task_history (task_id, user_id, action, field_changed, new_value) " +
	                       "VALUES (?, ?, '수정', 'assignee', ?)"; 

	    try (Connection conn = DatabaseConfig.getConnection(); 
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, assigneeId);
	        pstmt.setInt(2, taskId);

	        if (pstmt.executeUpdate() > 0) {
	            try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
	                historyPstmt.setInt(1, taskId);
	                historyPstmt.setInt(2, changedBy);
	                historyPstmt.setInt(3, assigneeId);
	                historyPstmt.executeUpdate();
	            }
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	// 담당자별 작업 조회
	public List<TaskDTO> getTasksByAssignee(int assigneeId, int projectId) {
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue " + "FROM tasks t "
				+ "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id "
				+ "WHERE t.assignee_id = ? AND t.project_id = ? AND t.status != '삭제' " + "ORDER BY t.due_date ASC";

		List<TaskDTO> tasks = new ArrayList<>();

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, assigneeId);
			pstmt.setInt(2, projectId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				tasks.add(mapResultSetToTask(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tasks;
	}

	// 마감일 임박 작업 조회
	public List<TaskDTO> getUpcomingTasks(int userId, int projectId, int hours) {
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue " + "FROM tasks t "
				+ "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id "
				+ "WHERE t.assignee_id = ? AND t.project_id = ? " + "AND t.status = '진행중' AND t.due_date IS NOT NULL "
				+ "AND t.due_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL ? HOUR) " + "ORDER BY t.due_date ASC";

		List<TaskDTO> tasks = new ArrayList<>();

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, userId);
			pstmt.setInt(2, projectId);
			pstmt.setInt(3, hours);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				tasks.add(mapResultSetToTask(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tasks;
	}

	// 태그로 작업 필터링
	public List<TaskDTO> getTasksByTag(int tagId, int projectId) {
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue " + "FROM tasks t "
				+ "JOIN task_tags tt ON t.task_id = tt.task_id " + "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id "
				+ "WHERE tt.tag_id = ? AND t.project_id = ? AND t.status != '삭제' " + "ORDER BY t.created_at DESC";

		List<TaskDTO> tasks = new ArrayList<>();

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, tagId);
			pstmt.setInt(2, projectId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				tasks.add(mapResultSetToTask(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tasks;
	}

	// ResultSet을 TaskDTO로 변환
	private TaskDTO mapResultSetToTask(ResultSet rs) throws SQLException {
		TaskDTO task = new TaskDTO();
		task.taskId = rs.getInt("task_id");
		task.projectId = rs.getInt("project_id");
		task.title = rs.getString("title");
		task.description = rs.getString("description");
		task.priority = rs.getString("priority");
		task.status = rs.getString("status");
		task.creator = rs.getString("creator");
		task.assignee = rs.getString("assignee");
		task.completedBy = rs.getString("completed_by");
		task.createdAt = rs.getTimestamp("created_at");
		task.completedAt = rs.getTimestamp("completed_at");
		task.dueDate = rs.getTimestamp("due_date");
		task.isOverdue = rs.getBoolean("is_overdue");
		return task;
	}

	public List<TaskDTO> getOverdueTasks(int projectId) {
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue " + "FROM tasks t "
				+ "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id " + "WHERE t.project_id = ? "
				+ "AND t.status != '삭제' AND t.status != '완료' " + "AND t.due_date IS NOT NULL "
				+ "AND t.due_date < NOW() " + "ORDER BY t.due_date ASC";

		List<TaskDTO> tasks = new ArrayList<>();

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, projectId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				TaskDTO task = mapResultSetToTask(rs);
				task.isOverdue = true; // 명시적으로 설정
				tasks.add(task);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tasks;
	}
	
	// 작업 상태 변경 (범용)
	public boolean updateTaskStatus(int taskId, String newStatus, int userId) {
	    String sql = "UPDATE tasks SET status = ? WHERE task_id = ?";
	    String historySql = "INSERT INTO task_history (task_id, user_id, action) VALUES (?, ?, ?)";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setString(1, newStatus);
	        pstmt.setInt(2, taskId);
	        int result = pstmt.executeUpdate();

	        if (result > 0) {
	            try (PreparedStatement historyPstmt = conn.prepareStatement(historySql)) {
	                historyPstmt.setInt(1, taskId);
	                historyPstmt.setInt(2, userId);
	                historyPstmt.setString(3, newStatus);
	                historyPstmt.executeUpdate();
	            }
	            System.out.println("[DB] 작업 상태 변경: ID " + taskId + " -> " + newStatus);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	// 작업 제목과 설명 수정
	public boolean updateTaskContent(int taskId, String title, String description) {
		String sql = "UPDATE tasks SET title = ?, description = ? WHERE task_id = ?";

		try (Connection conn = DatabaseConfig.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, title);
			pstmt.setString(2, description);
			pstmt.setInt(3, taskId);

			return pstmt.executeUpdate() > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	// 제목 또는 설명으로 작업 검색
	public List<TaskDTO> searchTasks(int projectId, String keyword) {
		// 제목이나 설명에 키워드가 포함된 작업 검색 (삭제된 것 제외)
		String sql = "SELECT t.task_id, t.project_id, t.title, t.description, t.priority, t.status, "
				+ "u1.username as creator, u2.username as assignee, u3.username as completed_by, "
				+ "t.created_at, t.completed_at, t.due_date, t.is_overdue "
				+ "FROM tasks t "
				+ "LEFT JOIN users u1 ON t.creator_id = u1.user_id "
				+ "LEFT JOIN users u2 ON t.assignee_id = u2.user_id "
				+ "LEFT JOIN users u3 ON t.completed_by_id = u3.user_id "
				+ "WHERE t.project_id = ? AND t.status != '삭제' "
				+ "AND (t.title LIKE ? OR t.description LIKE ?) " // 검색 조건
				+ "ORDER BY t.created_at DESC";

		List<TaskDTO> tasks = new ArrayList<>();

		try (Connection conn = DatabaseConfig.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, projectId);
			pstmt.setString(2, "%" + keyword + "%"); // 앞뒤로 % 붙여서 부분 일치 검색
			pstmt.setString(3, "%" + keyword + "%"); // 설명에도 똑같이 적용

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				tasks.add(mapResultSetToTask(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tasks;
	}
	// 작업 변경 이력 조회
    public List<String> getTaskHistory(int taskId) {
        List<String> historyList = new ArrayList<>();
        String sql = "SELECT u.username, th.action, th.created_at " +
                     "FROM task_history th " +
                     "JOIN users u ON th.user_id = u.user_id " +
                     "WHERE th.task_id = ? " +
                     "ORDER BY th.created_at DESC"; // 최신순 정렬
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            while (rs.next()) {
                String user = rs.getString("username");
                String action = rs.getString("action");
                String time = sdf.format(rs.getTimestamp("created_at"));
                
                // 예: [2023-11-25 14:00:00] admin님이 작업을 '수정' 했습니다.
                String log = String.format("[%s] %s님이 작업을 '%s' 했습니다.", time, user, action);
                historyList.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }
}