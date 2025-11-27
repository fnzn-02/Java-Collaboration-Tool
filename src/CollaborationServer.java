import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

public class CollaborationServer {
    private static final int PORT = 8888;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    
    // ProjectMemberInfo 클래스 (CollaborationServer 내부에 추가)
    public static class ProjectMemberInfo {
        int userId;
        String username;
        String role;
    }
    
    // 업그레이드된 DAO
    private static TaskDAO taskDAO = new TaskDAO();
    private static ChatDAO chatDAO = new ChatDAO();
    private static UserDAO userDAO = new UserDAO();
    private static ProjectDAO projectDAO = new ProjectDAO();
    private static CommentDAO commentDAO = new CommentDAO();
    private static NotificationDAO notificationDAO = new NotificationDAO();
    private static TagDAO tagDAO = new TagDAO();
    private static AttachmentDAO attachmentDAO = new AttachmentDAO();
    private static StatisticsDAO statisticsDAO = new StatisticsDAO();
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("    실시간 협업 시스템 서버 시작");
        System.out.println("    포트: " + PORT);
        System.out.println("    새 기능: 권한, 담당자, 알림, 댓글, 태그, 통계");
        System.out.println("==========================================");
        
        if (DatabaseConfig.getConnection() == null) {
            System.err.println("[오류] 데이터베이스 연결 실패");
            return;
        }
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[서버] 클라이언트 접속 대기중...\n");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection();
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private int userId;
        private int currentProjectId = 1; // 기본 프로젝트
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                
                String authInfo = in.readLine();
                String[] parts = authInfo.split("\\|");
                username = parts[0];
                userId = Integer.parseInt(parts[1]);
                if (parts.length > 2) {
                    currentProjectId = Integer.parseInt(parts[2]);
                }
                
                System.out.println("[접속] " + username + " (ID: " + userId + ")");
                broadcast("SYSTEM|" + username + "님이 입장했습니다.");
                sendUserProjects();

                sendRecentChats();
                sendUnreadNotifications();

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.out.println("[종료] " + username);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
                if (username != null) {
                    broadcast("SYSTEM|" + username + "님이 퇴장했습니다.");
                }
            }
        }
        
        // [CollaborationServer.java] 메서드 추가
        private void sendUserProjects() {
            List<ProjectDTO> projects = projectDAO.getUserProjects(userId);
            // 목록 개수 전송
            out.println("PROJECT_LIST_SIZE|" + projects.size());
            // 각 프로젝트 정보 전송
            for (ProjectDTO p : projects) {
                out.println("PROJECT_ITEM|" + p.projectId + "|" + p.projectName + "|" +
                        (p.description == null ? "" : p.description));
            }
        }

        private void handleMessage(String message) {
            System.out.println("[디버그] 클라이언트에게 받은 메시지: " + message);

            String[] parts = message.split("\\|", -1);
            String command = parts[0];
            
            switch (command) {
                case "ADD":
                    handleAddTask(parts);
                    break;
                    
                case "ASSIGN":
                    handleAssignTask(parts);
                    break;
                    
                case "COMPLETE":
                    handleCompleteTask(parts);
                    break;
                    
                case "DELETE":
                    handleDeleteTask(parts);
                    break;
                    
                case "CHAT":
                    handleChat(parts);
                    break;
                    
                case "COMMENT":
                    handleComment(parts);
                    break;
                    
                case "ADD_TAG":
                    handleAddTag(parts);
                    break;
                    
                case "TAG_TASK":
                    handleTagTask(parts);
                    break;
                    
                case "FILTER_BY_TAG":
                    handleFilterByTag(parts);
                    break;
                    
                case "FILTER_BY_ASSIGNEE":
                    handleFilterByAssignee(parts);
                    break;
                    
                case "GET_COMMENTS":
                    handleGetComments(parts);
                    break;
                    
                case "GET_NOTIFICATIONS":
                    sendUnreadNotifications();
                    break;
                    
                case "READ_NOTIFICATION":
                    handleReadNotification(parts);
                    break;
                    
                case "GET_STATISTICS":
                    handleGetStatistics();
                    break;
                    
                case "SWITCH_PROJECT":
                    handleSwitchProject(parts);
                    break;
                    
                case "CREATE_PROJECT":
                    handleCreateProject(parts);
                    break;
                    
                case "UPLOAD_FILE":
                    handleFileUpload(parts);
                    break;
                    
                case "GET_ATTACHMENTS":
                    handleGetAttachments(parts);
                    break;
                    
                case "GET_PROJECT_MEMBERS":
                    handleGetProjectMembers(parts);
                    break;
                    
                case "REFRESH":
                    sendTaskList();
                    break;
                
                case "FILTER_UPCOMING":
                    handleFilterUpcoming(parts);
                    break;
                    
                case "FILTER_OVERDUE":
                    handleFilterOverdue();
                    break;
                    
                case "UPDATE_STATUS":
                    handleUpdateStatus(parts);
                    break;

                case "GET_TASK_TAGS":
                    handleGetTaskTags(parts);
                    break;

                case "INVITE":
                    handleInviteMember(parts);
                    break;

                case "DOWNLOAD":
                    handleFileDownload(parts);
                    break;

                case "EDIT_TASK": // 수정 요청 처리
                    handleEditTask(parts);
                    break;

                case "SEARCH": // 검색 요청
                    handleSearchTasks(parts);
                    break;

                case "GET_HISTORY": // 히스토리 요청
                    handleGetHistory(parts);
                    break;

                case "DELETE_COMMENT":
                    handleDeleteComment(parts);

                    break;

                case "EDIT_COMMENT":
                    handleEditComment(parts);
                    break;
                
                case "DELETE_ATTACHMENT": // 파일 삭제 요청
                    handleDeleteAttachment(parts);
                    break;
            }
        }
        // 우클릭 상태 변경 처리
        private void handleUpdateStatus(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            String newStatus = parts[2];  // "대기", "진행중" 또는 "완료"

            boolean result = false;
            
            if ("대기".equals(newStatus)) {
                // 대기 상태로 변경
                result = taskDAO.updateTaskStatus(taskId, "대기", userId);
            } else if ("진행중".equals(newStatus)) {
                result = taskDAO.startTask(taskId, userId);
            } else if ("완료".equals(newStatus)) {
                result = taskDAO.completeTask(taskId, userId);
            }

            if (result) {
                TaskDTO updatedTask = taskDAO.getTaskById(taskId);
                if (updatedTask != null) {
                    broadcastToProject("TASK_UPDATE|" + updatedTask.serialize());
                }
            } else {
                out.println("ERROR|상태 변경 실패");
            }
        }
        
        // 댓글 삭제 처리
        private void handleDeleteComment(String[] parts) {
            try {
                int taskId = Integer.parseInt(parts[1]);
                int commentId = Integer.parseInt(parts[2]);

                // DB에서 삭제 (userId를 같이 보내서 본인 것만 삭제되게 함 - DAO에 이미 로직 있음)
                if (commentDAO.deleteComment(commentId, userId)) {
                    // 삭제 성공 시 해당 작업의 댓글 목록을 다시 뿌려줌 (새로고침 효과)
                    handleGetComments(new String[] { "GET_COMMENTS", String.valueOf(taskId) });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 댓글 수정 처리
        private void handleEditComment(String[] parts) {
            try {
                int taskId = Integer.parseInt(parts[1]);
                int commentId = Integer.parseInt(parts[2]);
                String newContent = parts[3];

                if (commentDAO.updateComment(commentId, userId, newContent)) {
                    handleGetComments(new String[] { "GET_COMMENTS", String.valueOf(taskId) });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 메서드 구현
        private void handleGetProjectMembers(String[] parts) {
            int projectId = Integer.parseInt(parts[1]);
            List<ProjectMemberInfo> members = projectDAO.getProjectMembers(projectId);
            
            System.out.println("[서버] 프로젝트 멤버 조회: projectId=" + projectId + ", 멤버수=" + members.size());
            
            out.println("PROJECT_MEMBERS|" + members.size());
            for (ProjectMemberInfo member : members) {
                out.println("PROJECT_MEMBER|" + member.userId + "|" + member.username + "|" + member.role);
            }
        }

        
        // 메서드 구현
        private void handleFilterUpcoming(String[] parts) {
            int hours = parts.length > 1 ? Integer.parseInt(parts[1]) : 24;
            List<TaskDTO> tasks = taskDAO.getUpcomingTasks(userId, currentProjectId, hours);
            
            for (TaskDTO task : tasks) {
                out.println("TASK_ADD|" + task.serialize());
            }
        }

        private void handleFilterOverdue() {
            List<TaskDTO> tasks = taskDAO.getOverdueTasks(currentProjectId);
            
            for (TaskDTO task : tasks) {
                out.println("TASK_ADD|" + task.serialize());
            }
        }
        
        // 작업 추가 (담당자, 마감일 포함)
        private void handleAddTask(String[] parts) {
            try {
                String title = parts[1];
                String description = parts[2];
                String priority = parts[3];
                Integer assigneeId = (parts.length > 4 && !parts[4].isEmpty()) ? Integer.parseInt(parts[4]) : null;
                Timestamp dueDate = (parts.length > 5 && !parts[5].isEmpty()) ? new Timestamp(Long.parseLong(parts[5]))
                        : null;

                // 클라이언트가 보낸 태그 ID들을 받음
                String tagIdsStr = (parts.length > 6) ? parts[6] : "";

                int taskId = taskDAO.addTask(title, description, priority, userId, assigneeId, dueDate,
                        currentProjectId);

                if (taskId > 0) {
                    // 태그가 있다면 DB에 저장
                    if (!tagIdsStr.isEmpty()) {
                        String[] tagIds = tagIdsStr.split(",");
                        for (String tagIdStr : tagIds) {
                            try {
                                int tagId = Integer.parseInt(tagIdStr);
                                tagDAO.addTagToTask(taskId, tagId);
                            } catch (NumberFormatException e) {
                                System.err.println("태그 ID 오류: " + tagIdStr);
                            }
                        }
                    }

                    TaskDTO newTask = taskDAO.getTaskById(taskId);
                    if (newTask != null) {
                        broadcastToProject("TASK_ADD|" + newTask.serialize());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 담당자 지정
        private void handleAssignTask(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            int assigneeId = Integer.parseInt(parts[2]);
            
            if (taskDAO.assignTask(taskId, assigneeId, userId)) {
                TaskDTO task = taskDAO.getTaskById(taskId);
                if (task != null) {
                    broadcastToProject("TASK_UPDATE|" + task.serialize());
                }
            }
        }
        
        private void handleCompleteTask(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            
            System.out.println("[서버] 작업 완료 요청: taskId=" + taskId + ", userId=" + userId);
            
            // 작업 완료 처리
            if (taskDAO.completeTask(taskId, userId)) {
                System.out.println("[서버] 작업 완료 성공");
                
                // 업데이트된 작업 정보 조회
                TaskDTO task = taskDAO.getTaskById(taskId);
                if (task != null) {
                    System.out.println("[서버] 작업 상태: " + task.status);
                    
                    // 같은 프로젝트의 모든 클라이언트에게 브로드캐스트
                    broadcastToProject("TASK_UPDATE|" + task.serialize());
                    
                    // 작업 담당자에게 알림
                    if (task.assignee != null && !task.assignee.isEmpty()) {
                        // 담당자에게 완료 알림 (필요시 구현)
                    }
                } else {
                    System.err.println("[서버] 작업 조회 실패: taskId=" + taskId);
                    out.println("ERROR|작업 정보를 찾을 수 없습니다");
                }
            } else {
                System.err.println("[서버] 작업 완료 실패: taskId=" + taskId);
                out.println("ERROR|작업 완료 실패. 진행중 상태인지 확인하세요");
            }
        }
        
        // 파일 다운로드 처리
        private void handleFileDownload(String[] parts) {
            try {
                int attachmentId = Integer.parseInt(parts[1]);
                // 1. DB에서 파일 경로 찾기
                AttachmentDTO att = attachmentDAO.getAttachmentById(attachmentId);

                if (att != null) {
                    File file = new File(att.filePath);
                    if (file.exists()) {
                        // 2. 파일을 읽어서 바이트 배열로 변환
                        byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
                        // 3. 바이트를 문자열(Base64)로 인코딩 (채팅처럼 보내기 위해)
                        String base64Encoded = Base64.getEncoder().encodeToString(fileContent);

                        // 4. 클라이언트에게 전송 (명령어 | 파일명 | 내용)
                        out.println("FILE_DOWNLOAD|" + att.fileName + "|" + base64Encoded);
                        System.out.println("[서버] 파일 전송 완료: " + att.fileName);
                    } else {
                        out.println("ERROR|서버에 파일이 존재하지 않습니다.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERROR|파일 다운로드 중 오류 발생");
            }
        }

        // 작업 삭제
        private void handleDeleteTask(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            
            // 작업 삭제 처리
            if (taskDAO.deleteTask(taskId, userId)) {
                broadcastToProject("TASK_DELETE|" + taskId);
            } else {
                out.println("ERROR|작업 삭제 실패");
            }
        }
        
        // 채팅
        private void handleChat(String[] parts) {
            String chatMsg = parts[1];
            if (chatDAO.saveMessage(userId, chatMsg)) {
                broadcast("CHAT|" + username + "|" + chatMsg);
            }
        }
        
        // 댓글 작성
        private void handleComment(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            String content = parts[2];
            
            int commentId = commentDAO.addComment(taskId, userId, content);
            if (commentId > 0) {
                CommentDTO comment = new CommentDTO();
                comment.commentId = commentId;
                comment.taskId = taskId;
                comment.userId = userId;
                comment.username = username;
                comment.content = content;
                comment.isEdited = false;
                comment.createdAt = new Timestamp(System.currentTimeMillis());
                
                broadcastToProject("COMMENT|" + taskId + "|" + comment.serialize());
            }
        }
        
        // 댓글 조회
        private void handleGetComments(String[] parts) {
            try {
                int taskId = Integer.parseInt(parts[1]);
                List<CommentDTO> comments = commentDAO.getTaskComments(taskId);

                // [★추가됨★] 클라이언트에게 "기존 댓글 싹 지워!"라고 먼저 말함
                out.println("COMMENTS_CLEAR|" + taskId);

                for (CommentDTO comment : comments) {
                    out.println("COMMENT|" + taskId + "|" + comment.serialize());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 태그 추가
        private void handleAddTag(String[] parts) {
            String tagName = parts[1];
            String color = parts[2];
            
            int tagId = tagDAO.createTag(tagName, color, currentProjectId);
            if (tagId > 0) {
                broadcastToProject("TAG_CREATED|" + tagId + "|" + tagName + "|" + color);
            }
        }
        
        // 작업에 태그 지정
        private void handleTagTask(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            int tagId = Integer.parseInt(parts[2]);
            
            if (tagDAO.addTagToTask(taskId, tagId)) {
                broadcastToProject("TASK_TAGGED|" + taskId + "|" + tagId);
            }
        }
        
        // 태그로 필터링
        private void handleFilterByTag(String[] parts) {
            int tagId = Integer.parseInt(parts[1]);
            List<TaskDTO> tasks = taskDAO.getTasksByTag(tagId, currentProjectId);
            
            out.println("FILTER_RESULT|TAG|" + tasks.size());
            for (TaskDTO task : tasks) {
                out.println("TASK_ADD|" + task.serialize());
            }
        }
        
        // 담당자로 필터링
        private void handleFilterByAssignee(String[] parts) {
            int assigneeId = Integer.parseInt(parts[1]);
            List<TaskDTO> tasks = taskDAO.getTasksByAssignee(assigneeId, currentProjectId);
            
            out.println("FILTER_RESULT|ASSIGNEE|" + tasks.size());
            for (TaskDTO task : tasks) {
                out.println("TASK_ADD|" + task.serialize());
            }
        }
        
        // 알림 읽음 처리
        private void handleReadNotification(String[] parts) {
            int notificationId = Integer.parseInt(parts[1]);
            notificationDAO.markAsRead(notificationId);
        }
        
        // 통계 조회
        private void handleGetStatistics() {
            ProjectStatistics projectStats = statisticsDAO.getProjectStatistics(currentProjectId);
            UserStatistics userStats = statisticsDAO.getUserStatistics(userId);
            
            if (projectStats != null) {
                out.println("PROJECT_STATS|" + projectStats.totalTasks + "|" + 
                           projectStats.completedTasks + "|" + projectStats.inProgressTasks + "|" + 
                           projectStats.overdueTasks + "|" + projectStats.memberCount + "|" + 
                           projectStats.totalComments);
            }
            
            if (userStats != null) {
                out.println("USER_STATS|" + userStats.assignedTasks + "|" + 
                           userStats.createdTasks + "|" + userStats.completedTasks + "|" + 
                           userStats.overdueTasks);
            }
        }
        
        // 프로젝트 전환
        private void handleSwitchProject(String[] parts) {
            int newProjectId = Integer.parseInt(parts[1]);

            // 현재 프로젝트 ID 변경
            this.currentProjectId = newProjectId;

            System.out.println("[서버] " + username + " -> 프로젝트 전환: " + newProjectId);

            // 클라이언트에게 전환 완료 신호 보냄
            out.println("PROJECT_SWITCHED|" + newProjectId);

            sendProjectTags();
            sendTaskList();
        }
        
        // 프로젝트 생성
        private void handleCreateProject(String[] parts) {
            String projectName = parts[1];
            String description = parts[2];
            
            int projectId = projectDAO.createProject(projectName, description, userId);
            if (projectId > 0) {
                out.println("PROJECT_CREATED|" + projectId + "|" + projectName);
            }
        }
        
        // 파일 업로드 정보 저장
        private void handleFileUpload(String[] parts) {
            try {
                // 데이터 파싱
                int taskId = Integer.parseInt(parts[1]);
                String fileName = parts[2];
                long fileSize = Long.parseLong(parts[3]);
                String fileType = parts[4];
                String base64Content = parts[5]; // 파일 내용

                // 1. 서버에 저장할 폴더 만들기 (없으면 생성)
                File uploadDir = new File("server_files");
                if (!uploadDir.exists()) {
                    uploadDir.mkdir();
                }

                // 2. 파일 이름 중복 방지를 위해 시간 추가
                String saveName = System.currentTimeMillis() + "_" + fileName;
                File destFile = new File(uploadDir, saveName);

                // 3. 암호문을 다시 파일로 변환해서 저장
                byte[] fileData = Base64.getDecoder().decode(base64Content);
                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    fos.write(fileData);
                }

                System.out.println("[서버] 파일 저장 완료: " + destFile.getAbsolutePath());

                // 4. DB에 '진짜 저장된 경로'를 등록
                // (클라이언트가 준 가짜 경로가 아니라, 서버에 실제 저장된 경로를 넣음)
                int attachmentId = attachmentDAO.addAttachment(taskId, userId, fileName, destFile.getAbsolutePath(),
                        fileSize, fileType);

                // 5. 같은 방 사람들에게 알림
                if (attachmentId > 0) {
                    broadcastToProject("FILE_UPLOADED|" + taskId + "|" + fileName + "|" + fileSize);
                }

            } catch (Exception e) {
                System.err.println("파일 업로드 처리 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 첨부파일 목록 조회
        private void handleGetAttachments(String[] parts) {
            int taskId = Integer.parseInt(parts[1]);
            List<AttachmentDTO> attachments = attachmentDAO.getTaskAttachments(taskId);
            
            out.println("ATTACHMENTS|" + taskId + "|" + attachments.size());
            for (AttachmentDTO att : attachments) {
                out.println("ATTACHMENT|" + att.attachmentId + "|" + att.fileName + "|" + 
                           att.fileSize + "|" + att.uploaderName + "|" + att.uploadedAt.getTime());
            }
        }
        
        // 작업 목록 전송
        private void sendTaskList() {
            List<TaskDTO> tasks = taskDAO.getAllActiveTasks();
            for (TaskDTO task : tasks) {
                if (task.projectId == currentProjectId) {
                    out.println("TASK_ADD|" + task.serialize());
                }
            }
        }
        
        // 최근 채팅 전송
        private void sendRecentChats() {
            List<ChatMessageDTO> messages = chatDAO.getRecentMessages(50);
            for (ChatMessageDTO msg : messages) {
                out.println("CHAT|" + msg.username + "|" + msg.message);
            }
        }
        
        // 읽지 않은 알림 전송
        private void sendUnreadNotifications() {
            List<NotificationDTO> notifications = notificationDAO.getUnreadNotifications(userId);
            
            out.println("NOTIFICATIONS|" + notifications.size());
            for (NotificationDTO notif : notifications) {
                out.println("NOTIFICATION|" + notif.serialize());
            }
        }
        
        // 프로젝트 태그 전송
        private void sendProjectTags() {
            List<TagDTO> tags = tagDAO.getProjectTags(currentProjectId);
            
            out.println("TAGS|" + tags.size());
            for (TagDTO tag : tags) {
                out.println("TAG|" + tag.serialize());
            }
        }
        
        // 모든 클라이언트에게 브로드캐스트
        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }
        
        // 같은 프로젝트의 클라이언트에게만 브로드캐스트
        private void broadcastToProject(String message) {
            for (ClientHandler client : clients) {
                if (client.currentProjectId == this.currentProjectId) {
                    client.out.println(message);
                }
            }
        }
        
        // 상세 화면에서 태그를 달라고 하면 DB에서 꺼내주는 역할
        private void handleGetTaskTags(String[] parts) {
            try {
                int taskId = Integer.parseInt(parts[1]);
                System.out.println("[서버] 태그 조회 요청 받음: 작업ID " + taskId); // 로그 확인용

                List<TagDTO> tags = tagDAO.getTaskTags(taskId);

                StringBuilder sb = new StringBuilder();
                for (TagDTO tag : tags) {
                    if (sb.length() > 0)
                        sb.append(";");
                    // ID,이름,색상 순으로 묶어서 보냄
                    sb.append(tag.tagId).append(",").append(tag.tagName).append(",").append(tag.color);
                }

                // 클라이언트에게 답장 보냄
                out.println("TASK_TAGS|" + taskId + "|" + sb.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // [2] 멤버 초대 기능 
        private void handleInviteMember(String[] parts) {
            try {
                String targetUsername = parts[1];

                // 1. 초대할 사람의 ID 찾기
                int targetUserId = userDAO.getUserIdByUsername(targetUsername);

                if (targetUserId == -1) {
                    out.println("ERROR|존재하지 않는 사용자입니다.");
                    return;
                }

                // 2. 이미 멤버인지 확인 (중복 초대 방지)
                List<ProjectMemberInfo> members = projectDAO.getProjectMembers(currentProjectId);
                for (ProjectMemberInfo member : members) {
                    if (member.userId == targetUserId) {
                        out.println("ERROR|이미 프로젝트 멤버입니다.");
                        return;
                    }
                }

                // 3. 멤버 추가 실행 (기본 역할: MEMBER)
                if (projectDAO.addMember(currentProjectId, targetUserId, "MEMBER")) {
                    broadcastToProject("SYSTEM|" + targetUsername + "님이 프로젝트에 초대되었습니다.");
                    // 멤버 목록 갱신 신호 보내기
                    handleGetProjectMembers(new String[] { "GET_PROJECT_MEMBERS", String.valueOf(currentProjectId) });
                } else {
                    out.println("ERROR|초대에 실패했습니다.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 작업 내용 수정 (제목, 설명 업데이트)
        private void handleEditTask(String[] parts) {
            try {
                int taskId = Integer.parseInt(parts[1]);
                String newTitle = parts[2];
                String newDesc = parts[3];
                
                // TaskDAO에 업데이트 메서드 호출 (아래 TaskDAO 수정 필요)
                if (taskDAO.updateTaskContent(taskId, newTitle, newDesc)) {
                    // 변경된 내용 브로드캐스트 (목록 갱신용)
                    TaskDTO updatedTask = taskDAO.getTaskById(taskId);
                    if (updatedTask != null) {
                        broadcastToProject("TASK_UPDATE|" + updatedTask.serialize());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 작업 검색 처리
        private void handleSearchTasks(String[] parts) {
            try {
                String keyword = parts[1];
                System.out.println("[서버] 검색 요청: " + keyword);

                // TaskDAO의 검색 메서드 호출 (아래 3단계에서 만들 예정)
                List<TaskDTO> tasks = taskDAO.searchTasks(currentProjectId, keyword);

                // 결과가 없으면 알림 (선택사항)
                if (tasks.isEmpty()) {
                    // 클라이언트가 결과 0개인걸 알 수 있게 별도 처리는 안 해도 됨
                    // (FILTER_RESULT 재활용하거나 그냥 빈 목록 보냄)
                }

                // 검색 결과를 클라이언트에 전송 (기존 TASK_ADD 방식 재사용)
                // 클라이언트가 목록을 비우고 기다리고 있으므로, 검색된 것만 보냄
                for (TaskDTO task : tasks) {
                    out.println("TASK_ADD|" + task.serialize());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 첨부파일 삭제 처리
        private void handleDeleteAttachment(String[] parts) {
            try {
                int attachmentId = Integer.parseInt(parts[1]);

                // 1. 파일 정보 조회 (경로를 알아야 지우니까)
                AttachmentDTO att = attachmentDAO.getAttachmentById(attachmentId);

                if (att != null) {
                    // 2. DB에서 삭제
                    if (attachmentDAO.deleteAttachment(attachmentId)) {
                        // 3. 실제 파일 삭제
                        File file = new File(att.filePath);
                        if (file.exists()) {
                            file.delete(); // 파일 삭제
                        }
                        System.out.println("[서버] 파일 삭제 완료: " + att.fileName);

                        // 4. 변경 알림 (새로운 명령어 사용)
                        broadcastToProject("ATTACHMENT_UPDATE|" + att.taskId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 히스토리 목록 전송
        private void handleGetHistory(String[] parts) {
            try {
                int taskId = Integer.parseInt(parts[1]);
                List<String> logs = taskDAO.getTaskHistory(taskId);

                // 목록을 "///"로 이어 붙여서 보냄 (줄바꿈 대신 특수문자 사용)
                String joinedLogs = String.join("///", logs);

                out.println("HISTORY|" + taskId + "|" + joinedLogs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}