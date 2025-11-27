import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class CollaborationClient extends JFrame {
	
	// 간단한 DTO 클래스 추가 (CollaborationClient 내부 클래스로)
    public static class ProjectMemberDTO {
        int userId;
        String username;
        String role;
    }
	
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private int userId;
    private String userRole;
    private int currentProjectId = -1;
    
    // UI 컴포넌트
    private DefaultTableModel taskTableModel;
    private JTable taskTable;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel statusLabel;
    private JLabel notificationBadge;
    private JComboBox<String> projectSelector;
    private JComboBox<String> filterCombo;
    private JPanel tagPanel;
    private JTabbedPane rightPanel;
    private JPanel detailsTagPanel;
    private DefaultListModel<String> fileListModel;
    private List<Integer> attachmentIds = new ArrayList<>(); // 파일 ID 저장용 리스트
    private DefaultListModel<String> dialogAttachmentModel; // 상세 창 파일 목록
    private List<Integer> dialogAttachmentIds = new ArrayList<>();
    private JTextArea detailsHistoryArea; // 히스토리 탭의 텍스트 영역 (전역 변수)
    private DefaultListModel<String> dialogCommentModel;
    
    // 데이터 저장
    private List<ProjectDTO> userProjects = new ArrayList<>();
    private List<TagDTO> projectTags = new ArrayList<>();
    private Map<Integer, List<CommentDTO>> taskComments = new HashMap<>();
    private List<NotificationDTO> unreadNotifications = new ArrayList<>();
    private List<ProjectMemberDTO> projectMembers = new ArrayList<>();
    private int selectedTaskId = -1;
    
    public CollaborationClient(String username, int userId, String role) {
        this.username = username;
        this.userId = userId;
        this.userRole = role;
        
        setTitle("실시간 협업 시스템 - " + username + " (" + role + ")");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        if (!connectToServer()) {
            dispose();
            return;
        }
        
        initUI();
        
        setLocationRelativeTo(null);
        setVisible(true);
        
        startMessageListener();
    }
    
    private boolean connectToServer() {
        try {
            socket = new Socket("localhost", 8888);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            out.println(username + "|" + userId + "|" + currentProjectId);
            
            System.out.println("[클라이언트] 서버 연결 성공");
            return true;
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "서버 연결 실패!\n서버가 실행 중인지 확인하세요.", 
                "연결 오류", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        // 상단 패널
        add(createTopPanel(), BorderLayout.NORTH);
        
        // 중앙 분할 패널
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(createLeftPanel());
        mainSplitPane.setRightComponent(createRightPanel());
        mainSplitPane.setDividerLocation(900);
        
        add(mainSplitPane, BorderLayout.CENTER);
        
        // 하단 상태바
        add(createStatusBar(), BorderLayout.SOUTH);
    }
    
    // 상단 패널
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // 헤더
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(93, 156, 236));
        headerPanel.setPreferredSize(new Dimension(1400, 70));

        JLabel titleLabel = new JLabel("  실시간 협업 시스템  ");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        // 오른쪽 사용자 정보 패널
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        userPanel.setOpaque(false);

        JButton notificationBtn = new JButton("🔔");
        notificationBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        notificationBtn.setFocusPainted(false);
        notificationBtn.setBorderPainted(false);
        notificationBtn.setContentAreaFilled(false);
        notificationBtn.setForeground(Color.WHITE);
        notificationBtn.addActionListener(e -> showNotifications());

        notificationBadge = new JLabel("0");
        notificationBadge.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        notificationBadge.setForeground(Color.WHITE);
        notificationBadge.setBackground(Color.RED);
        notificationBadge.setOpaque(true);
        notificationBadge.setPreferredSize(new Dimension(20, 20));
        notificationBadge.setHorizontalAlignment(SwingConstants.CENTER);
        notificationBadge.setBorder(new LineBorder(Color.RED, 2, true));

        JLabel userLabel = new JLabel(username + " (" + userRole + ")  ");
        userLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        userLabel.setForeground(Color.WHITE);

        // 로그아웃 버튼
        JButton logoutBtn = new JButton("로그아웃");
        logoutBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        logoutBtn.setBackground(new Color(231, 76, 60)); // 빨간색
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false); // 깔끔하게
        logoutBtn.setPreferredSize(new Dimension(90, 30));

        // 로그아웃 동작
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    // 서버 연결 종료
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // 현재 창 닫기
                dispose();

                // 로그인 창 다시 열기
                new LoginWindow();
            }
        });

        userPanel.add(notificationBtn);
        userPanel.add(notificationBadge);
        userPanel.add(userLabel);
        userPanel.add(logoutBtn); // 패널에 추가

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(userPanel, BorderLayout.EAST);

        // 툴바 
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbarPanel.setBackground(Color.WHITE);
        toolbarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel projectLabel = new JLabel("프로젝트:");
        projectLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        projectSelector = new JComboBox<>();
        projectSelector.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        projectSelector.setPreferredSize(new Dimension(180, 35));
        projectSelector.addItem("--- 프로젝트 선택 ---");
        projectSelector.addActionListener(e -> switchProject());

        JButton addButton = createToolbarButton("+ 새 할 일", new Color(46, 204, 113));
        JButton addProjectBtn = createToolbarButton("+ 프로젝트", new Color(52, 152, 219));
        JButton inviteBtn = createToolbarButton("멤버 초대", new Color(155, 89, 182));
        inviteBtn.setForeground(Color.WHITE);

        JButton refreshButton = createToolbarButton("새로고침", null);
        JButton statsButton = createToolbarButton("대시보드", null);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchPanel.setOpaque(false);

        JTextField searchField = new JTextField(15);
        searchField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(150, 35));

        JButton searchBtn = new JButton("검색");
        searchBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        searchBtn.setPreferredSize(new Dimension(70, 35));
        searchBtn.setBackground(new Color(52, 73, 94));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setOpaque(true);
        searchBtn.setBorderPainted(false);

        ActionListener searchAction = e -> {
            String keyword = searchField.getText().trim();
            if (!keyword.isEmpty() && currentProjectId > 0) {
                taskTableModel.setRowCount(0);
                out.println("SEARCH|" + keyword);
            } else if (currentProjectId <= 0) {
                JOptionPane.showMessageDialog(this, "프로젝트를 먼저 선택하세요.");
            } else {
                refreshTasks();
            }
        };
        searchBtn.addActionListener(searchAction);
        searchField.addActionListener(searchAction);

        searchPanel.add(new JLabel("🔍"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        addButton.addActionListener(e -> showAddTaskDialog());
        addProjectBtn.addActionListener(e -> showCreateProjectDialog());

        inviteBtn.addActionListener(e -> {
            String targetUser = JOptionPane.showInputDialog(this, "초대할 사용자의 아이디(Username)를 입력하세요:");
            if (targetUser != null && !targetUser.trim().isEmpty()) {
                out.println("INVITE|" + targetUser.trim());
            }
        });

        refreshButton.addActionListener(e -> {
            projectSelector.setSelectedIndex(0);
            currentProjectId = -1;
            taskTableModel.setRowCount(0);
            projectTags.clear();
            searchField.setText("");
            if (tagPanel != null) {
                tagPanel.removeAll();
                tagPanel.revalidate();
                tagPanel.repaint();
            }
            JOptionPane.showMessageDialog(this, "화면이 초기화되었습니다. 프로젝트를 다시 선택해주세요.");
        });

        statsButton.addActionListener(e -> showDashboard());

        toolbarPanel.add(projectLabel);
        toolbarPanel.add(projectSelector);
        toolbarPanel.add(addButton);
        toolbarPanel.add(addProjectBtn);
        toolbarPanel.add(inviteBtn);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(statsButton);
        toolbarPanel.add(Box.createHorizontalStrut(10));
        toolbarPanel.add(searchPanel);

        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(toolbarPanel, BorderLayout.CENTER);

        return topPanel;
    }
    
    // 툴바 버튼 생성
    private JButton createToolbarButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(text.contains("+") ? 120 : 100, 35));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        
        if (bgColor != null) {
            button.setBackground(bgColor);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(240, 240, 240));
        }
        
        return button;
    }
    
    // 왼쪽 패널
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        // 1. 태그 패널 설정
        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        tagPanel.setPreferredSize(new Dimension(900, 50));
        tagPanel.setBorder(BorderFactory.createTitledBorder("태그 필터"));

        JButton addTagBtn = new JButton("+ 태그 추가");
        addTagBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        addTagBtn.addActionListener(e -> showAddTagDialog());
        tagPanel.add(addTagBtn);

        // 2. 테이블 모델 설정
        String[] columns = { "ID", "제목", "설명", "우선순위", "상태", "작성자", "담당자", "완료자", "마감일", "작성시간" };
        taskTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(taskTableModel);
        taskTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        taskTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        taskTable.getTableHeader().setBackground(new Color(230, 230, 230));
        taskTable.setRowHeight(35);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.setGridColor(new Color(220, 220, 220));

        // 3. 클릭 리스너 
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = taskTable.getSelectedRow();
                if (row >= 0) {
                    try {
                        // ID 가져오기 (안전하게 변환)
                        Object idObj = taskTableModel.getValueAt(row, 0);
                        selectedTaskId = Integer.parseInt(idObj.toString());

                        // 오른쪽 화면 초기화 (try-catch로 감싸서 안전하게 처리)
                        try {
                            // 댓글창 비우기
                            JPanel cPanel = (JPanel) rightPanel.getComponentAt(1);
                            JScrollPane cScroll = (JScrollPane) cPanel.getComponent(0);
                            JTextArea cArea = (JTextArea) cScroll.getViewport().getView();
                            cArea.setText("");

                            // 첨부파일창 비우기
                            JPanel aPanel = (JPanel) rightPanel.getComponentAt(2);
                            JScrollPane aScroll = (JScrollPane) aPanel.getComponent(0);
                            JList<?> aList = (JList<?>) aScroll.getViewport().getView();
                            ((DefaultListModel<?>) aList.getModel()).clear();
                            attachmentIds.clear(); //
                        } catch (Exception ex) {
                            // UI 초기화 중 에러는 무시 (기능엔 지장 없음)
                        }

                        // 서버에 데이터 요청
                        out.println("GET_COMMENTS|" + selectedTaskId);
                        out.println("GET_ATTACHMENTS|" + selectedTaskId);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // 4. 테이블 컬럼 너비 설정
        int[] widths = { 50, 180, 250, 80, 70, 80, 80, 80, 120, 120 };
        for (int i = 0; i < widths.length; i++) {
            taskTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // 5. 테이블 색상 렌더러
        taskTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 4); // 상태 컬럼
                    String dueDate = (String) table.getValueAt(row, 8); // 마감일 컬럼
                    String priority = (String) table.getValueAt(row, 3); // 우선순위

                    // 색상 로직
                    if (dueDate != null && dueDate.contains("지연") && !"완료".equals(status)) {
                        c.setBackground(new Color(255, 200, 200)); // 지연 (빨강)
                    } else if ("완료".equals(status)) {
                        c.setBackground(new Color(212, 237, 218)); // 완료 (초록)
                    } else if ("높음".equals(priority)) {
                        c.setBackground(new Color(248, 215, 218)); // 높음 (연빨강)
                    } else {
                        c.setBackground(Color.WHITE); // 기본
                    }
                    c.setForeground(Color.BLACK);
                } else {
                    c.setForeground(Color.WHITE); // 선택됨
                }
                return c;
            }
        });

        // 6. 마우스 이벤트 (더블클릭, 우클릭)
        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 더블클릭
                    int row = taskTable.getSelectedRow();
                    if (row >= 0) {
                        selectedTaskId = Integer.parseInt(taskTableModel.getValueAt(row, 0).toString());
                        showTaskDetailsDialog();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPop(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPop(e);
            }

            private void doPop(MouseEvent e) {
                int row = taskTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    taskTable.setRowSelectionInterval(row, row);
                    selectedTaskId = Integer.parseInt(taskTableModel.getValueAt(row, 0).toString());

                    // 우클릭 시에도 오른쪽 갱신
                    out.println("GET_COMMENTS|" + selectedTaskId);
                    out.println("GET_ATTACHMENTS|" + selectedTaskId);

                    showTaskContextMenu(e);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " 할 일 목록 ",
                0, 0, new Font("맑은 고딕", Font.BOLD, 15)));

        leftPanel.add(tagPanel, BorderLayout.NORTH);
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        return leftPanel;
    }
    
    // 오른쪽 패널 (탭: 채팅, 댓글, 첨부파일)
    private JPanel createRightPanel() {
        rightPanel = new JTabbedPane();
        rightPanel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        // 채팅 탭
        rightPanel.addTab(" 팀 채팅", createChatPanel());
        
        // 댓글 탭 (작업 선택 시 활성화)
        rightPanel.addTab(" 댓글", createCommentPanel());
        
        // 첨부파일 탭
        rightPanel.addTab(" 첨부파일", createAttachmentPanel());
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        container.add(rightPanel, BorderLayout.CENTER);
        
        return container;
    }
    
    // 채팅 패널
    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInput = new JTextField();
        chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        chatInput.setPreferredSize(new Dimension(0, 35));
        
        JButton sendButton = new JButton("전송");
        sendButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        sendButton.setPreferredSize(new Dimension(80, 35));
        sendButton.setBackground(new Color(52, 152, 219));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        
        chatInput.addActionListener(e -> sendChat());
        sendButton.addActionListener(e -> sendChat());
        
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    // 댓글 패널
    private JPanel createCommentPanel() {
        JPanel commentPanel = new JPanel(new BorderLayout(5, 5));

        JTextArea commentArea = new JTextArea();
        commentArea.setEditable(false);
        commentArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        commentArea.setLineWrap(true);
        JScrollPane commentScroll = new JScrollPane(commentArea);

        JPanel commentInputPanel = new JPanel(new BorderLayout(5, 0));
        JTextField commentInput = new JTextField();
        commentInput.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        commentInput.setPreferredSize(new Dimension(0, 35));

        JButton commentButton = new JButton("댓글 작성");
        commentButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        commentButton.setPreferredSize(new Dimension(100, 35));
        commentButton.setBackground(new Color(155, 89, 182)); // 보라색
        commentButton.setForeground(Color.WHITE);
        commentButton.setFocusPainted(false);
        commentButton.setBorderPainted(false);

        // 전송 동작을 하나로 묶기 (버튼 클릭 + 엔터키 공용)
        ActionListener sendAction = e -> {
            if (selectedTaskId > 0 && !commentInput.getText().trim().isEmpty()) {
                out.println("COMMENT|" + selectedTaskId + "|" + commentInput.getText().trim());
                commentInput.setText(""); // 입력창 비우기
            } else if (selectedTaskId <= 0) {
                JOptionPane.showMessageDialog(this, "작업을 먼저 선택해주세요.");
            }
        };

        // 1. 버튼을 누를 때 실행
        commentButton.addActionListener(sendAction);

        // 2. 입력창에서 엔터 칠 때 실행
        commentInput.addActionListener(sendAction);

        commentInputPanel.add(commentInput, BorderLayout.CENTER);
        commentInputPanel.add(commentButton, BorderLayout.EAST);

        commentPanel.add(commentScroll, BorderLayout.CENTER);
        commentPanel.add(commentInputPanel, BorderLayout.SOUTH);

        return commentPanel;
    }
    
    // 첨부파일 패널 (리스트 모델을 전역 변수와 연결)
    private JPanel createAttachmentPanel() {
        JPanel attachmentPanel = new JPanel(new BorderLayout(5, 5));
        fileListModel = new DefaultListModel<>();
        JList<String> fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        // 더블클릭 이벤트
        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = fileList.getSelectedIndex();
                    if (index >= 0 && index < attachmentIds.size()) {
                        int fileId = attachmentIds.get(index);
                        // 서버에 다운로드 요청
                        out.println("DOWNLOAD|" + fileId);
                    }
                }
            }
        });

        JScrollPane fileScroll = new JScrollPane(fileList);
        JPanel fileButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton uploadButton = new JButton("파일 업로드");
        uploadButton.addActionListener(e -> uploadFile());
        fileButtonPanel.add(uploadButton);

        attachmentPanel.add(fileScroll, BorderLayout.CENTER);
        attachmentPanel.add(fileButtonPanel, BorderLayout.SOUTH);
        return attachmentPanel;
    }
    
    // 하단 상태바
    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        
        statusLabel = new JLabel(" 서버 연결됨 | MySQL 데이터베이스 연동 | v2.0");
        statusLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setBackground(new Color(240, 240, 240));
        statusLabel.setOpaque(true);
        
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        
        return statusPanel;
    }
    
 // CollaborationClientUpgraded.java 계속...

    // ============================================
    // 다이얼로그 및 기능 메서드들
    // ============================================
    
    // 새 할 일 추가
    private void showAddTaskDialog() {
        JDialog dialog = new JDialog(this, "새 할 일 추가", true);
        dialog.setSize(600, 500);
        dialog.setLayout(new BorderLayout(15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // 제목, 설명, 우선순위, 담당자, 마감일 등 기존 UI 구성
        addFormLabel(formPanel, gbc, "제목:", 0);
        JTextField titleField = new JTextField();
        titleField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        addFormField(formPanel, gbc, titleField, 0);

        addFormLabel(formPanel, gbc, "설명:", 1);
        JTextArea descArea = new JTextArea(5, 30);
        descArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 3;
        formPanel.add(descScroll, gbc);
        gbc.gridheight = 1;

        addFormLabel(formPanel, gbc, "우선순위:", 4);
        String[] priorities = { "높음", "중간", "낮음" };
        JComboBox<String> priorityCombo = new JComboBox<>(priorities);
        priorityCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        addFormField(formPanel, gbc, priorityCombo, 4);

        addFormLabel(formPanel, gbc, "담당자:", 5);
        JComboBox<String> assigneeCombo = new JComboBox<>();
        assigneeCombo.addItem("없음");
        for (ProjectMemberDTO m : projectMembers)
            assigneeCombo.addItem(m.username);
        assigneeCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        addFormField(formPanel, gbc, assigneeCombo, 5);

        addFormLabel(formPanel, gbc, "마감일:", 6);
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd HH:mm");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JCheckBox enableDateCheck = new JCheckBox("마감일 설정");
        dateSpinner.setEnabled(false);
        enableDateCheck.addActionListener(e -> dateSpinner.setEnabled(enableDateCheck.isSelected()));
        datePanel.add(enableDateCheck);
        datePanel.add(dateSpinner);
        gbc.gridx = 1;
        gbc.gridy = 6;
        formPanel.add(datePanel, gbc);

        // 태그 선택 UI
        addFormLabel(formPanel, gbc, "태그:", 7);
        JPanel tagSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        List<JCheckBox> tagCheckBoxes = new ArrayList<>();
        List<TagDTO> currentTagsList = new ArrayList<>(projectTags);

        for (TagDTO tag : currentTagsList) {
            JCheckBox tagCheck = new JCheckBox(tag.tagName);
            try {
                tagCheck.setForeground(Color.decode(tag.color));
            } catch (Exception e) {
            }
            tagCheckBoxes.add(tagCheck);
            tagSelectionPanel.add(tagCheck);
        }
        gbc.gridx = 1;
        gbc.gridy = 7;
        formPanel.add(tagSelectionPanel, gbc);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("추가");
        addButton.setBackground(new Color(46, 204, 113));
        addButton.setForeground(Color.WHITE);
        addButton.setOpaque(true);
        addButton.setBorderPainted(false);

        JButton cancelButton = new JButton("취소");

        addButton.addActionListener(e -> {
            System.out.println("[클라이언트] 추가 버튼 눌림!");
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "제목 입력 필요");
                return;
            }

            String desc = descArea.getText().trim();
            String priority = (String) priorityCombo.getSelectedItem();

            String assigneeId = "";
            String selectedAssignee = (String) assigneeCombo.getSelectedItem();
            if (!"없음".equals(selectedAssignee)) {
                for (ProjectMemberDTO m : projectMembers) {
                    if (m.username.equals(selectedAssignee)) {
                        assigneeId = String.valueOf(m.userId);
                        break;
                    }
                }
            }

            String dueDate = "";
            if (enableDateCheck.isSelected()) {
                dueDate = String.valueOf(((Date) dateSpinner.getValue()).getTime());
            }

            // 선택된 태그 ID 수집
            List<String> selectedTagIds = new ArrayList<>();
            for (int i = 0; i < tagCheckBoxes.size(); i++) {
                if (tagCheckBoxes.get(i).isSelected()) {
                    selectedTagIds.add(String.valueOf(currentTagsList.get(i).tagId));
                }
            }
            String tagIdsStr = String.join(",", selectedTagIds);

            String msg = "ADD|" + title + "|" + desc + "|" + priority + "|" + assigneeId + "|" + dueDate + "|"
                    + tagIdsStr;
            System.out.println("[클라이언트] 서버로 전송할 메시지: " + msg); 

            out.println(msg); // 서버로 전송
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    // 작업 상세 다이얼로그
    private void showTaskDetailsDialog() {
        if (selectedTaskId < 0)
            return;

        JDialog dialog = new JDialog(this, "작업 상세 정보 및 수정", true);
        dialog.setSize(700, 750); 
        dialog.setLayout(new BorderLayout(10, 10));

        // 1. 상단 정보 패널 (수정 가능한 영역)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        int row = taskTable.getSelectedRow();
        if (row < 0)
            return;

        // 기존 데이터 가져오기
        String title = (String) taskTableModel.getValueAt(row, 1);
        String desc = (String) taskTableModel.getValueAt(row, 2);
        String priority = (String) taskTableModel.getValueAt(row, 3);
        String status = (String) taskTableModel.getValueAt(row, 4);
        String creator = (String) taskTableModel.getValueAt(row, 5);
        String assignee = (String) taskTableModel.getValueAt(row, 6);
        String dueDate = (String) taskTableModel.getValueAt(row, 8);

        // 제목 입력 필드
        addLeftAlignedLabel(infoPanel, "제목:", new Font("맑은 고딕", Font.BOLD, 14));
        JTextField titleField = new JTextField(title);
        titleField.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        infoPanel.add(titleField);

        infoPanel.add(Box.createVerticalStrut(10));

        // 설명 입력 필드
        addLeftAlignedLabel(infoPanel, "설명:", new Font("맑은 고딕", Font.BOLD, 14));
        JTextArea descArea = new JTextArea(desc);
        descArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setPreferredSize(new Dimension(600, 80));
        infoPanel.add(descScroll);

        infoPanel.add(Box.createVerticalStrut(10));

        // 수정 내용 저장 버튼
        JButton saveBtn = new JButton("수정 내용 저장");
        saveBtn.setBackground(new Color(52, 73, 94)); 
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveBtn.addActionListener(e -> {
            String newTitle = titleField.getText().trim();
            String newDesc = descArea.getText().trim();

            if (newTitle.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "제목은 비울 수 없습니다.");
                return;
            }

            // 서버에 수정 요청
            out.println("EDIT_TASK|" + selectedTaskId + "|" + newTitle + "|" + newDesc);
            JOptionPane.showMessageDialog(dialog, "저장되었습니다."); // 멘트 수정됨
        });
        infoPanel.add(saveBtn);
        infoPanel.add(Box.createVerticalStrut(15));

        // 태그 표시 영역
        JPanel tagContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tagContainer.setOpaque(false);
        tagContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagTitle = new JLabel("태그: ");
        tagTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        tagContainer.add(tagTitle);

        detailsTagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        detailsTagPanel.setOpaque(false);
        JLabel loadingLbl = new JLabel("(불러오는 중...)");
        detailsTagPanel.add(loadingLbl);
        tagContainer.add(detailsTagPanel);
        infoPanel.add(tagContainer);

        // 태그 정보 요청
        out.println("GET_TASK_TAGS|" + selectedTaskId);

        infoPanel.add(Box.createVerticalStrut(15));

        // 기타 정보 (읽기 전용)
        addLeftAlignedLabel(infoPanel, "상태: " + status + " | 우선순위: " + priority, new Font("맑은 고딕", Font.PLAIN, 13));
        infoPanel.add(Box.createVerticalStrut(5));
        addLeftAlignedLabel(infoPanel, "담당자: " + assignee + " (작성자: " + creator + ")",
                new Font("맑은 고딕", Font.PLAIN, 13));

        if (dueDate != null && !dueDate.equals("-")) {
            infoPanel.add(Box.createVerticalStrut(5));
            addLeftAlignedLabel(infoPanel, "마감일: " + dueDate, new Font("맑은 고딕", Font.PLAIN, 13));
        }

        // 2. 탭 패널
        JTabbedPane tabPane = new JTabbedPane();

        // (1) 댓글 탭 (JList + 우클릭 메뉴 적용됨)
        JPanel commentTabPanel = new JPanel(new BorderLayout());
        dialogCommentModel = new DefaultListModel<>();
        JList<String> commentList = new JList<>(dialogCommentModel);
        commentList.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        commentList.setToolTipText("내 댓글을 우클릭하면 수정/삭제할 수 있습니다.");

        // 기존 댓글 채우기
        List<CommentDTO> currentComments = taskComments.get(selectedTaskId);
        if (currentComments != null) {
            for (CommentDTO c : currentComments) {
                String display = String.format("[%s] %s", c.username, c.content);
                if (c.isEdited)
                    display += " (수정됨)";
                dialogCommentModel.addElement(display);
            }
        }

        // 댓글 우클릭 메뉴
        commentList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = commentList.locationToIndex(e.getPoint());
                    commentList.setSelectedIndex(index);

                    // taskComments에서 최신 리스트를 다시 가져와야 안전함
                    List<CommentDTO> latestComments = taskComments.get(selectedTaskId);

                    if (latestComments != null && index >= 0 && index < latestComments.size()) {
                        CommentDTO selectedComment = latestComments.get(index);

                        if (selectedComment.userId == userId || userRole.equals("ADMIN")) {
                            JPopupMenu popup = new JPopupMenu();
                            JMenuItem editItem = new JMenuItem("수정");
                            JMenuItem deleteItem = new JMenuItem("삭제");

                            editItem.addActionListener(ev -> {
                                String newContent = JOptionPane.showInputDialog(dialog, "댓글 수정:",
                                        selectedComment.content);
                                if (newContent != null && !newContent.trim().isEmpty()) {
                                    out.println("EDIT_COMMENT|" + selectedTaskId + "|" + selectedComment.commentId + "|"
                                            + newContent.trim());
                                }
                            });

                            deleteItem.addActionListener(ev -> {
                                int confirm = JOptionPane.showConfirmDialog(dialog, "정말 삭제하시겠습니까?", "댓글 삭제",
                                        JOptionPane.YES_NO_OPTION);
                                if (confirm == JOptionPane.YES_OPTION) {
                                    out.println("DELETE_COMMENT|" + selectedTaskId + "|" + selectedComment.commentId);
                                }
                            });

                            popup.add(editItem);
                            popup.add(deleteItem);
                            popup.show(commentList, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        commentTabPanel.add(new JScrollPane(commentList), BorderLayout.CENTER);
        tabPane.addTab(" 댓글", commentTabPanel);

        commentTabPanel.add(new JScrollPane(commentList), BorderLayout.CENTER);
        tabPane.addTab(" 댓글", commentTabPanel);

        // (2) 첨부파일 탭 (여기서 변수들이 다 정의됩니다!)
        JPanel attachmentTabPanel = new JPanel(new BorderLayout());
        dialogAttachmentModel = new DefaultListModel<>();
        dialogAttachmentIds = new ArrayList<>();
        JList<String> attachmentList = new JList<>(dialogAttachmentModel);

        attachmentList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = attachmentList.getSelectedIndex();
                    if (index >= 0 && index < dialogAttachmentIds.size()) {
                        int fileId = dialogAttachmentIds.get(index);
                        out.println("DOWNLOAD|" + fileId);
                    }
                }
            }
        });
        attachmentTabPanel.add(new JScrollPane(attachmentList), BorderLayout.CENTER);

        // 첨부파일 버튼 패널 
        JPanel fileBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton dialogUploadBtn = new JButton("업로드");
        dialogUploadBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        dialogUploadBtn.setBackground(new Color(46, 204, 113));
        dialogUploadBtn.setForeground(Color.WHITE);
        dialogUploadBtn.setOpaque(true);
        dialogUploadBtn.setBorderPainted(false);
        dialogUploadBtn.addActionListener(e -> uploadFile());

        JButton dialogFileDelBtn = new JButton("삭제");
        dialogFileDelBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        dialogFileDelBtn.setBackground(new Color(231, 76, 60));
        dialogFileDelBtn.setForeground(Color.WHITE);
        dialogFileDelBtn.setOpaque(true);
        dialogFileDelBtn.setBorderPainted(false);

        dialogFileDelBtn.addActionListener(e -> {
            int index = attachmentList.getSelectedIndex();
            if (index >= 0 && index < dialogAttachmentIds.size()) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "선택한 파일을 삭제하시겠습니까?\n(복구할 수 없습니다)", "파일 삭제", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    int fileId = dialogAttachmentIds.get(index);
                    out.println("DELETE_ATTACHMENT|" + fileId);
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "삭제할 파일을 선택해주세요.");
            }
        });

        fileBtnPanel.add(dialogUploadBtn);
        fileBtnPanel.add(dialogFileDelBtn);
        attachmentTabPanel.add(fileBtnPanel, BorderLayout.SOUTH);

        tabPane.addTab(" 첨부파일", attachmentTabPanel);

        // (3) 히스토리 탭
        JPanel historyTabPanel = new JPanel(new BorderLayout());
        detailsHistoryArea = new JTextArea("히스토리를 불러오는 중...");
        detailsHistoryArea.setEditable(false);
        historyTabPanel.add(new JScrollPane(detailsHistoryArea), BorderLayout.CENTER);
        tabPane.addTab(" 히스토리", historyTabPanel);

        // 데이터 로드
        out.println("GET_COMMENTS|" + selectedTaskId);
        tabPane.addChangeListener(e -> {
            if (tabPane.getSelectedIndex() == 1)
                out.println("GET_ATTACHMENTS|" + selectedTaskId);
            else if (tabPane.getSelectedIndex() == 2)
                out.println("GET_HISTORY|" + selectedTaskId);
        });

        // 3. 하단 버튼 패널 (알록달록 버튼들)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton assignBtn = new JButton("담당자 변경");

        JButton waitBtn = new JButton("대기");
        waitBtn.setBackground(new Color(243, 156, 18));
        waitBtn.setForeground(Color.WHITE);

        JButton startBtn = new JButton("진행중");
        startBtn.setBackground(new Color(52, 152, 219));
        startBtn.setForeground(Color.WHITE);

        JButton completeBtn = new JButton("완료 처리");
        completeBtn.setBackground(new Color(46, 204, 113));
        completeBtn.setForeground(Color.WHITE);

        JButton deleteBtn = new JButton("삭제");
        deleteBtn.setBackground(new Color(231, 76, 60));
        deleteBtn.setForeground(Color.WHITE);

        JButton closeBtn = new JButton("닫기");

        // 모든 버튼 스타일 통일
        for (JButton btn : new JButton[] { assignBtn, waitBtn, startBtn, completeBtn, deleteBtn, closeBtn }) {
            if (btn.getBackground().equals(new Color(238, 238, 238)))
                continue; // 기본색은 건너뜀
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
        }

        assignBtn.addActionListener(e -> {
            showAssignDialog();
            dialog.dispose();
        });
        waitBtn.addActionListener(e -> {
            out.println("UPDATE_STATUS|" + selectedTaskId + "|대기");
            dialog.dispose();
        });
        startBtn.addActionListener(e -> {
            out.println("UPDATE_STATUS|" + selectedTaskId + "|진행중");
            dialog.dispose();
        });
        completeBtn.addActionListener(e -> {
            out.println("COMPLETE|" + selectedTaskId);
            dialog.dispose();
        });

        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "정말 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                out.println("DELETE|" + selectedTaskId);
                dialog.dispose();
            }
        });
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(assignBtn);
        buttonPanel.add(waitBtn);
        buttonPanel.add(startBtn);
        buttonPanel.add(completeBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(closeBtn);

        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(tabPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                dialogAttachmentModel = null;
                dialogAttachmentIds = null;
                detailsHistoryArea = null;
                dialogCommentModel = null;
            }
        });
    }

    // 왼쪽 정렬 라벨 추가 헬퍼 메서드 (showTaskDetailsDialog 바로 아래에 붙여넣으세요)
    private void addLeftAlignedLabel(JPanel panel, String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.LEFT_ALIGNMENT); 
        panel.add(label);
    }
    
 // 작업 컨텍스트 메뉴
    private void showTaskContextMenu(MouseEvent e) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        
        // 현재 선택된 작업의 정보 확인
        int row = taskTable.getSelectedRow();
        String currentStatus = (String) taskTableModel.getValueAt(row, 4);
        String dueDate = (String) taskTableModel.getValueAt(row, 8);
        
        // 기본 메뉴
        JMenuItem detailsItem = new JMenuItem("상세 보기");
        JMenuItem assignItem = new JMenuItem("담당자 지정");
        
        detailsItem.addActionListener(ev -> showTaskDetailsDialog());
        assignItem.addActionListener(ev -> showAssignDialog());
        
        popupMenu.add(detailsItem);
        popupMenu.add(assignItem);
        popupMenu.addSeparator();
        
        // 상태별 동적 메뉴 구성
        switch (currentStatus) {
            case "대기":
                // 대기 → 진행중 또는 완료
                JMenuItem startFromWait = new JMenuItem("진행중으로 변경");
                JMenuItem completeFromWait = new JMenuItem("바로 완료 처리");
                
                startFromWait.addActionListener(ev -> {
                    out.println("UPDATE_STATUS|" + selectedTaskId + "|진행중");
                });
                
                completeFromWait.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "대기 중인 작업을 바로 완료 처리하시겠습니까?",
                        "확인", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        out.println("UPDATE_STATUS|" + selectedTaskId + "|완료");
                    }
                });
                
                popupMenu.add(startFromWait);
                popupMenu.add(completeFromWait);
                break;
                
            case "진행중":
                // 진행중 → 완료 또는 대기로 되돌리기
                JMenuItem completeFromProgress = new JMenuItem("완료 처리");
                JMenuItem backToWait = new JMenuItem("대기로 되돌리기");
                
                completeFromProgress.addActionListener(ev -> {
                    out.println("UPDATE_STATUS|" + selectedTaskId + "|완료");
                });
                
                backToWait.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "작업을 대기 상태로 되돌리시겠습니까?",
                        "확인", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        out.println("UPDATE_STATUS|" + selectedTaskId + "|대기");
                    }
                });
                
                popupMenu.add(completeFromProgress);
                popupMenu.add(backToWait);
                
                // 지연된 작업이면 경고 표시
                if (dueDate.contains("지연")) {
                    completeFromProgress.setForeground(new Color(231, 76, 60));
                    completeFromProgress.setText("완료 처리 (지연됨)");
                }
                break;
                
            case "완료":
                // 완료 → 진행중으로 재개
                JMenuItem reopenTask = new JMenuItem("진행중으로 재개");
                
                reopenTask.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "완료된 작업을 다시 진행중으로 변경하시겠습니까?",
                        "확인", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        out.println("UPDATE_STATUS|" + selectedTaskId + "|진행중");
                    }
                });
                
                popupMenu.add(reopenTask);
                break;
                
            default:
                // 알 수 없는 상태
                JMenuItem unknownItem = new JMenuItem("상태를 확인할 수 없습니다");
                unknownItem.setEnabled(false);
                popupMenu.add(unknownItem);
                break;
        }
        
        popupMenu.addSeparator();
        
        // 삭제 메뉴 (완료된 작업만 삭제 가능)
        JMenuItem deleteItem = new JMenuItem("삭제");
        if (currentStatus.equals("완료")) {
            deleteItem.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "정말 삭제하시겠습니까?\n완료된 작업은 복구할 수 없습니다.",
                    "확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    out.println("DELETE|" + selectedTaskId);
                }
            });
        } else {
            deleteItem.setEnabled(false);
            deleteItem.setToolTipText("완료된 작업만 삭제할 수 있습니다");
        }
        
        popupMenu.add(deleteItem);
        
        // 지연 정보 표시 (하단에 라벨 추가)
        if (dueDate.contains("지연") && !currentStatus.equals("완료")) {
            popupMenu.addSeparator();
            JMenuItem warningItem = new JMenuItem("이 작업은 마감일이 지났습니다");
            warningItem.setForeground(new Color(231, 76, 60));
            warningItem.setEnabled(false);
            popupMenu.add(warningItem);
        }
        
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    // 담당자 지정 다이얼로그
    private void showAssignDialog() {
        if (selectedTaskId < 0) {
            JOptionPane.showMessageDialog(this, "작업을 먼저 선택하세요.");
            return;
        }
        
        // 프로젝트 멤버 목록 요청
        out.println("GET_PROJECT_MEMBERS|" + currentProjectId);
        
        // 잠시 대기 후 다이얼로그 표시 (비동기 처리를 위한 임시 방법)
        Timer timer = new Timer(300, e -> {
            if (projectMembers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "프로젝트 멤버를 불러올 수 없습니다.");
                return;
            }
            
            String[] memberNames = projectMembers.stream()
                .map(m -> m.username)
                .toArray(String[]::new);
            
            String selected = (String) JOptionPane.showInputDialog(
                this,
                "담당자를 선택하세요:",
                "담당자 지정",
                JOptionPane.QUESTION_MESSAGE,
                null,
                memberNames,
                memberNames.length > 0 ? memberNames[0] : null
            );
            
            if (selected != null) {
                // 선택된 사용자의 ID 찾기
                int assigneeId = projectMembers.stream()
                    .filter(m -> m.username.equals(selected))
                    .findFirst()
                    .map(m -> m.userId)
                    .orElse(-1);
                
                if (assigneeId > 0) {
                    System.out.println("[클라이언트] 담당자 지정: taskId=" + selectedTaskId + ", assigneeId=" + assigneeId);
                    out.println("ASSIGN|" + selectedTaskId + "|" + assigneeId);
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    // 태그 추가 다이얼로그
    private void showAddTagDialog() {
        JDialog dialog = new JDialog(this, "새 태그 추가", true);
        dialog.setSize(400, 250);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        formPanel.add(new JLabel("태그 이름:"));
        JTextField tagNameField = new JTextField();
        formPanel.add(tagNameField);
        
        formPanel.add(new JLabel("색상:"));
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        String[] colors = {"#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c"};
        JComboBox<String> colorCombo = new JComboBox<>(colors);
        colorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    label.setForeground(Color.decode(value.toString()));
                }
                return label;
            }
        });
        
        colorPanel.add(colorCombo);
        formPanel.add(colorPanel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createBtn = new JButton("생성");
        JButton cancelBtn = new JButton("취소");
        
        createBtn.addActionListener(e -> {
            String tagName = tagNameField.getText().trim();
            String color = (String) colorCombo.getSelectedItem();
            
            if (!tagName.isEmpty()) {
                out.println("ADD_TAG|" + tagName + "|" + color);
                dialog.dispose();
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(createBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    // 알림 표시
    private void showNotifications() {
        JDialog dialog = new JDialog(this, "알림", true);
        dialog.setSize(500, 600);
        dialog.setLayout(new BorderLayout(10, 10));
        
        DefaultListModel<String> notifListModel = new DefaultListModel<>();
        
        for (NotificationDTO notif : unreadNotifications) {
            notifListModel.addElement(notif.title + " - " + notif.message);
        }
        
        if (notifListModel.isEmpty()) {
            notifListModel.addElement("읽지 않은 알림이 없습니다.");
        }
        
        JList<String> notifList = new JList<>(notifListModel);
        notifList.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton markAllReadBtn = new JButton("모두 읽음");
        JButton closeBtn = new JButton("닫기");
        
        markAllReadBtn.addActionListener(e -> {
            for (NotificationDTO notif : unreadNotifications) {
                out.println("READ_NOTIFICATION|" + notif.notificationId);
            }
            unreadNotifications.clear();
            updateNotificationBadge();
            dialog.dispose();
        });
        
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(markAllReadBtn);
        buttonPanel.add(closeBtn);
        
        dialog.add(new JScrollPane(notifList), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    // 대시보드 표시
    private JDialog statsDialog; // 클래스 필드로 추가
    private Map<String, JLabel> statLabels = new HashMap<>(); // 클래스 필드로 추가

    private void showDashboard() {
        if (currentProjectId <= 0) {
            JOptionPane.showMessageDialog(this,
                    "프로젝트를 먼저 선택해주세요.",
                    "알림", JOptionPane.WARNING_MESSAGE);
            return; // 여기서 메서드 강제 종료 (창 안 띄움)
        }

        out.println("GET_STATISTICS");
        
        statsDialog = new JDialog(this, "프로젝트 대시보드", true);
        statsDialog.setSize(800, 600);
        statsDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel statsPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 통계 카드들 생성 및 저장
        JPanel totalCard = createStatCard("전체 작업", "0", new Color(52, 152, 219));
        JPanel completedCard = createStatCard("완료된 작업", "0", new Color(46, 204, 113));
        JPanel inProgressCard = createStatCard("진행중인 작업", "0", new Color(241, 196, 15));
        JPanel overdueCard = createStatCard("지연된 작업", "0", new Color(231, 76, 60));
        JPanel memberCard = createStatCard("팀 멤버", "0", new Color(155, 89, 182));
        JPanel commentCard = createStatCard("전체 댓글", "0", new Color(52, 73, 94));
        
        // 레이블 참조 저장
        statLabels.put("total", (JLabel) ((JPanel) totalCard.getComponent(1)).getComponent(0));
        statLabels.put("completed", (JLabel) ((JPanel) completedCard.getComponent(1)).getComponent(0));
        statLabels.put("inProgress", (JLabel) ((JPanel) inProgressCard.getComponent(1)).getComponent(0));
        statLabels.put("overdue", (JLabel) ((JPanel) overdueCard.getComponent(1)).getComponent(0));
        statLabels.put("members", (JLabel) ((JPanel) memberCard.getComponent(1)).getComponent(0));
        statLabels.put("comments", (JLabel) ((JPanel) commentCard.getComponent(1)).getComponent(0));
        
        statsPanel.add(totalCard);
        statsPanel.add(completedCard);
        statsPanel.add(inProgressCard);
        statsPanel.add(overdueCard);
        statsPanel.add(memberCard);
        statsPanel.add(commentCard);
        
        JButton closeBtn = new JButton("닫기");
        closeBtn.addActionListener(e -> {
            statsDialog.dispose();
            statsDialog = null;
            statLabels.clear();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeBtn);
        
        statsDialog.add(statsPanel, BorderLayout.CENTER);
        statsDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        statsDialog.setLocationRelativeTo(this);
        statsDialog.setVisible(true);
    }
    
    // 통계 카드 생성
    private JPanel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color.darker(), 2, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("맑은 고딕", Font.BOLD, 36));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // 값 레이블을 패널로 감싸기
        JPanel valuePanel = new JPanel(new BorderLayout());
        valuePanel.setOpaque(false);
        valuePanel.add(valueLabel, BorderLayout.CENTER);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valuePanel, BorderLayout.CENTER);
        
        return card;
    }
    
    // 프로젝트 생성 다이얼로그
    private void showCreateProjectDialog() {
        String projectName = JOptionPane.showInputDialog(this, "프로젝트 이름:");
        if (projectName != null && !projectName.trim().isEmpty()) {
            String description = JOptionPane.showInputDialog(this, "프로젝트 설명:");
            out.println("CREATE_PROJECT|" + projectName + "|" + (description != null ? description : ""));
        }
    }
    
    // 파일 업로드
    private void uploadFile() {
        if (selectedTaskId < 0) {
            JOptionPane.showMessageDialog(this, "작업을 먼저 선택하세요.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // 용량 제한 (채팅으로 보내는 방식이라 너무 크면 안 됨. 5MB 제한)
            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "파일 크기는 5MB 이하여야 합니다.");
                return;
            }

            try {
                String fileName = file.getName();
                long fileSize = file.length();
                String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);

                // 1. 파일 내용을 읽어서 암호문(Base64)으로 변환
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);

                // 2. 서버로 전송 (명령어 | 작업ID | 파일명 | 크기 | 타입 | 내용)
                out.println("UPLOAD_FILE|" + selectedTaskId + "|" + fileName + "|" +
                        fileSize + "|" + fileType + "|" + base64Content);

                System.out.println("[클라이언트] 파일 전송 시작: " + fileName);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "파일 업로드 중 오류가 발생했습니다.");
            }
        }
    }
    
    // 유틸리티 메서드들
    private void addFormLabel(JPanel panel, GridBagConstraints gbc, String text, int row) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        panel.add(label, gbc);
    }
    
    private void addFormField(JPanel panel, GridBagConstraints gbc, JComponent component, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0.7;
        panel.add(component, gbc);
    }
    
    private JLabel createInfoLabel(String text, int style, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("맑은 고딕", style, size));
        return label;
    }
    
    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            out.println("CHAT|" + message);
            chatInput.setText("");
        }
    }
    
    private void refreshTasks() {
        taskTableModel.setRowCount(0);
        out.println("REFRESH");
    }
    
    private void applyFilter() {
        String filter = (String) filterCombo.getSelectedItem();
        
        // 테이블 초기화
        taskTableModel.setRowCount(0);
        
        switch (filter) {
            case "내 작업":
                out.println("FILTER_BY_ASSIGNEE|" + userId);
                break;
            case "마감 임박":
                // 24시간 내 마감 작업 필터
                out.println("FILTER_UPCOMING|24");
                break;
            case "지연됨":
                // 마감일 지난 작업 필터
                out.println("FILTER_OVERDUE");
                break;
            default:
                refreshTasks();
                break;
        }
    }
    
    // 프로젝트 전환 (안내 문구 처리 추가)
    private void switchProject() {
        int index = projectSelector.getSelectedIndex();

        // 0번은 "--- 프로젝트 선택 ---" 이므로 무시하거나 화면 비우기
        if (index <= 0) {
            currentProjectId = -1; // 선택 안 됨 상태
            taskTableModel.setRowCount(0); // 화면 비우기
            projectTags.clear();
            if (tagPanel != null) {
                tagPanel.removeAll();
                tagPanel.revalidate();
                tagPanel.repaint();
            }
            return;
        }

        // 실제 프로젝트 리스트는 index - 1 위치에 있음 (0번이 안내문구니까)
        if ((index - 1) < userProjects.size()) {
            ProjectDTO selectedProject = userProjects.get(index - 1); // [★수정됨★] index - 1

            // 현재 프로젝트와 다를 때만 요청
            if (selectedProject.projectId != currentProjectId) {
                System.out.println("[클라이언트] 프로젝트 전환 요청: " + selectedProject.projectName);

                // 화면 비우기
                taskTableModel.setRowCount(0);
                projectTags.clear();
                if (tagPanel != null) {
                    tagPanel.removeAll();
                    tagPanel.repaint();
                }

                // 서버에 전환 요청
                out.println("SWITCH_PROJECT|" + selectedProject.projectId);
            }
        }
    }
    
    private void updateNotificationBadge() {
        int count = unreadNotifications.size();
        notificationBadge.setText(String.valueOf(count));
        notificationBadge.setVisible(count > 0);
    }
    
 // CollaborationClientUpgraded.java 계속 - Part 3

    // ============================================
    // 서버 메시지 수신 및 처리
    // ============================================
    
    private void startMessageListener() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(" 서버 연결 끊김");
                    JOptionPane.showMessageDialog(this, "서버와의 연결이 끊어졌습니다.");
                });
            }
        }).start();
    }
    
    private void handleServerMessage(String message) {
        String[] parts = message.split("\\|", -1);
        String command = parts[0];
        
        SwingUtilities.invokeLater(() -> {
            try {
                switch (command) {
                    case "FILTER_RESULT":
                        taskTableModel.setRowCount(0); // 테이블 싹 비우기
                        String filterType = parts[1];
                        int count = Integer.parseInt(parts[2]);
                        if (count == 0) {
                            JOptionPane.showMessageDialog(this, "해당 조건의 작업이 없습니다.");
                        }
                        break;

                    case "TASK_ADD":
                        handleTaskAdd(parts);
                        break;
                        
                    case "TASK_UPDATE":
                        handleTaskUpdate(parts);
                        break;
                        
                    case "TASK_DELETE":
                        handleTaskDelete(parts);
                        break;
                        
                    case "CHAT":
                        handleChatMessage(parts);
                        break;
                        
                    case "SYSTEM":
                        handleSystemMessage(parts);
                        break;
                        
                    case "COMMENT":
                        handleCommentMessage(parts);
                        break;
                        
                    case "NOTIFICATION":
                        handleNotification(parts);
                        break;
                        
                    case "NOTIFICATIONS":
                        // 알림 개수 정보
                        int notifcount = Integer.parseInt(parts[1]);
                        unreadNotifications.clear();
                        break;
                        
                    case "TAG":
                        handleTagMessage(parts);
                        break;
                        
                    case "TAGS":
                        // 태그 개수 정보
                        projectTags.clear();
                        break;
                        
                    case "TAG_CREATED":
                        handleTagCreated(parts);
                        break;
                        
                    case "TASK_TAGGED":
                        handleTaskTagged(parts);
                        break;
                        
                    case "PROJECT_STATS":
                        handleProjectStats(parts);
                        break;
                        
                    case "USER_STATS":
                        handleUserStats(parts);
                        break;
                        
                    case "PROJECT_CREATED":
                        handleProjectCreated(parts);
                        break;
                        
                    case "PROJECT_SWITCHED":
                        handleProjectSwitched(parts);
                        break;
                        
                    case "FILE_UPLOADED":
                        handleFileUploaded(parts);
                        break;
                        
                    case "ATTACHMENTS":
                        // 첨부파일 개수 정보
                        try {
                            int tId = Integer.parseInt(parts[1]);
                            // 현재 선택된 작업의 파일이라면
                            if (tId == selectedTaskId) {
                                // 1. 메인 화면 오른쪽 패널 초기화
                                if (fileListModel != null)
                                    fileListModel.clear();
                                attachmentIds.clear();

                                // 2. 상세 보기 창 패널 초기화
                                if (dialogAttachmentModel != null)
                                    dialogAttachmentModel.clear();
                                dialogAttachmentIds.clear();
                            }
                        } catch (Exception e) {
                        }
                        break;
                        
                    case "ATTACHMENT":
                        handleAttachment(parts);
                        break;
                        
                    case "PROJECT_MEMBERS":
                        handleProjectMembers(parts);
                        break;

                    case "PROJECT_MEMBER":
                        handleProjectMember(parts);
                        break;
                        
                    case "ERROR":
                        JOptionPane.showMessageDialog(this, parts[1], "오류", JOptionPane.ERROR_MESSAGE);
                        break;

                    case "PROJECT_LIST_SIZE":
                        userProjects.clear();
                        projectSelector.removeAllItems();
                        projectSelector.addItem("--- 프로젝트 선택 ---");
                        break;

                    case "PROJECT_ITEM":
                        handleProjectItem(parts); // 방금 만든 메서드 호출!
                        break;

                    case "TASK_TAGS":
                        handleTaskTagsResponse(parts);
                        break;

                    case "FILE_DOWNLOAD":
                        handleFileDownloadResponse(parts);
                        break;

                    case "HISTORY": // 히스토리 데이터 수신
                        handleHistoryResponse(parts);
                        break;
                    
                    // 댓글 초기화 신호 처리
                    case "COMMENTS_CLEAR":
                        int cTaskId = Integer.parseInt(parts[1]);
                        if (taskComments.containsKey(cTaskId)) {
                            taskComments.get(cTaskId).clear(); // 데이터 비우기
                        }
                        // 상세 창이 열려있으면 리스트 화면도 비우기
                        if (cTaskId == selectedTaskId && dialogCommentModel != null) {
                            dialogCommentModel.clear();
                        }
                        break;

                    // 파일 목록 갱신 신호가 오면 실행
                    case "ATTACHMENT_UPDATE":
                        try {
                            int tId = Integer.parseInt(parts[1]);
                            // 내가 보고 있는 작업의 파일이 바뀌었으면
                            if (tId == selectedTaskId) {
                                // 1. 기존 목록 싹 비우기
                                if (fileListModel != null)
                                    fileListModel.clear();
                                if (dialogAttachmentModel != null)
                                    dialogAttachmentModel.clear();
                                attachmentIds.clear();
                                if (dialogAttachmentIds != null)
                                    dialogAttachmentIds.clear();

                                // 2. 서버에 목록 다시 달라고 요청
                                out.println("GET_ATTACHMENTS|" + tId);
                            }
                        } catch (Exception e) {
                        }
                        break;
                }
            } catch (Exception e) {
                System.err.println("메시지 처리 오류: " + message);
                e.printStackTrace();
            }
        });
    }
    
    // 메서드 추가
    private void handleProjectMembers(String[] parts) {
        int count = Integer.parseInt(parts[1]);
        projectMembers.clear();
        System.out.println("[클라이언트] 프로젝트 멤버 수신 시작: " + count + "명");
    }

    private void handleProjectMember(String[] parts) {
        try {
            ProjectMemberDTO member = new ProjectMemberDTO();
            member.userId = Integer.parseInt(parts[1]);
            member.username = parts[2];
            member.role = parts[3];
            projectMembers.add(member);
            System.out.println("[클라이언트] 멤버 추가: " + member.username);
        } catch (Exception e) {
            System.err.println("멤버 정보 처리 오류");
            e.printStackTrace();
        }
    }
    
    
    // 작업 추가 처리
    private void handleTaskAdd(String[] parts) {
        try {
            int id = Integer.parseInt(parts[1]);
            String title = parts[2];
            String desc = parts[3];
            String priority = parts[4];
            String status = parts[5];
            String creator = parts[6];
            String assignee = parts[7];
            String completedBy = parts[8];
            long createdTime = Long.parseLong(parts[9]);
            long completedTime = parts[10].equals("0") ? 0 : Long.parseLong(parts[10]);
            long dueDateTime = parts.length > 11 && !parts[11].equals("0") ? Long.parseLong(parts[11]) : 0;

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            String createdTimeStr = sdf.format(new Date(createdTime));
            String dueDateStr = "";

            if (dueDateTime > 0) {
                dueDateStr = sdf.format(new Date(dueDateTime));
                long now = System.currentTimeMillis();
                boolean isOverdue = now > dueDateTime && !status.equals("완료");

                if (isOverdue) {
                    long overdueDays = (now - dueDateTime) / (1000 * 60 * 60 * 24);
                    if (overdueDays > 0)
                        dueDateStr += " 지연 " + overdueDays + "일";
                    else {
                        long overdueHours = (now - dueDateTime) / (1000 * 60 * 60);
                        dueDateStr += " 지연 " + (overdueHours > 0 ? overdueHours + "시간" : "!");
                    }
                } else if (!status.equals("완료")) {
                    long remainingTime = dueDateTime - now;
                    if (remainingTime > 0 && remainingTime < 24 * 60 * 60 * 1000) {
                        long remainingHours = remainingTime / (1000 * 60 * 60);
                        dueDateStr += remainingHours + "시간 남음";
                    }
                }
            }

            taskTableModel.addRow(new Object[] {
                    id, title, desc, priority, status, creator,
                    assignee.isEmpty() ? "-" : assignee,
                    completedBy.isEmpty() ? "-" : completedBy,
                    dueDateStr.isEmpty() ? "-" : dueDateStr,
                    createdTimeStr
            });

            // 대시보드 숫자가 바뀌었으니 갱신 요청!
            refreshDashboardIfOpen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 작업 업데이트 처리
    private void handleTaskUpdate(String[] parts) {
        try {
            int id = Integer.parseInt(parts[1]);
            String title = parts[2];
            String desc = parts[3];
            String priority = parts[4];
            String status = parts[5];
            String creator = parts[6];
            String assignee = parts[7];
            String completedBy = parts[8];
            // 시간 정보들은 파싱만 하고 표시는 안 함 (기존 데이터 유지 위해)
            long dueDateTime = parts.length > 11 && !parts[11].equals("0") ? Long.parseLong(parts[11]) : 0;

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            String dueDateStr = "";

            if (dueDateTime > 0) {
                dueDateStr = sdf.format(new Date(dueDateTime));
                long now = System.currentTimeMillis();
                boolean isOverdue = now > dueDateTime && !status.equals("완료");
                if (isOverdue)
                    dueDateStr += " 지연!";
                else if (!status.equals("완료")) {
                    long remainingTime = dueDateTime - now;
                    if (remainingTime > 0 && remainingTime < 24 * 60 * 60 * 1000)
                        dueDateStr += " (임박)";
                }
            }

            // 테이블 값 변경
            for (int i = 0; i < taskTableModel.getRowCount(); i++) {
                if (Integer.parseInt(taskTableModel.getValueAt(i, 0).toString()) == id) {
                    taskTableModel.setValueAt(title, i, 1);
                    taskTableModel.setValueAt(desc, i, 2);
                    taskTableModel.setValueAt(priority, i, 3);
                    taskTableModel.setValueAt(status, i, 4);
                    taskTableModel.setValueAt(assignee.isEmpty() ? "-" : assignee, i, 6);
                    taskTableModel.setValueAt(completedBy.isEmpty() ? "-" : completedBy, i, 7);
                    taskTableModel.setValueAt(dueDateStr.isEmpty() ? "-" : dueDateStr, i, 8);
                    taskTable.repaint();
                    break;
                }
            }

            // 진행중 -> 완료 등으로 바뀌면 숫자도 바뀌어야 함!
            refreshDashboardIfOpen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 작업 삭제 처리
    private void handleTaskDelete(String[] parts) {
        try {
            int id = Integer.parseInt(parts[1]);

            for (int i = 0; i < taskTableModel.getRowCount(); i++) {
                if ((int) taskTableModel.getValueAt(i, 0) == id) {
                    taskTableModel.removeRow(i);
                    break;
                }
            }

            // 삭제했으니 전체 개수가 줄어야 함!
            refreshDashboardIfOpen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 채팅 메시지 처리
    private void handleChatMessage(String[] parts) {
        String sender = parts[1];
        String chatMsg = parts[2];
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        chatArea.append("[" + sdf.format(new Date()) + "] " + sender + ": " + chatMsg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    // 시스템 메시지 처리
    private void handleSystemMessage(String[] parts) {
        String sysMsg = parts[1];
        chatArea.append("[시스템] " + sysMsg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    // 댓글 메시지 처리
    private void handleCommentMessage(String[] parts) {
        try {
            int taskId = Integer.parseInt(parts[1]);
            int commentId = Integer.parseInt(parts[2]);
            String username = parts[3];
            String content = parts[4];
            boolean isEdited = Boolean.parseBoolean(parts[5]);
            long timestamp = Long.parseLong(parts[6]);

            CommentDTO comment = new CommentDTO();
            comment.commentId = commentId;
            comment.taskId = taskId;
            comment.userId = userId; // (내 아이디로 임시 저장, 표시는 username 사용)
            comment.username = username;
            comment.content = content;
            comment.isEdited = isEdited;
            comment.createdAt = new Timestamp(timestamp);

            // 데이터 저장
            if (!taskComments.containsKey(taskId)) {
                taskComments.put(taskId, new ArrayList<>());
            }
            taskComments.get(taskId).add(comment);

            // 1. 오른쪽 패널 갱신
            updateCommentPanel(taskId);

            // 2. 상세 창 리스트 갱신 (열려 있다면)
            if (taskId == selectedTaskId && dialogCommentModel != null) {
                String display = String.format("[%s] %s", comment.username, comment.content);
                if (comment.isEdited)
                    display += " (수정됨)";
                dialogCommentModel.addElement(display);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 알림 처리
    private void handleNotification(String[] parts) {
        try {
            int notifId = Integer.parseInt(parts[1]);
            String type = parts[2];
            String title = parts[3];
            String message = parts[4];
            int relatedTaskId = Integer.parseInt(parts[5]);
            boolean isRead = Boolean.parseBoolean(parts[6]);
            long timestamp = Long.parseLong(parts[7]);
            
            NotificationDTO notif = new NotificationDTO();
            notif.notificationId = notifId;
            notif.type = type;
            notif.title = title;
            notif.message = message;
            notif.relatedTaskId = relatedTaskId;
            notif.isRead = isRead;
            notif.createdAt = new Timestamp(timestamp);
            
            if (!isRead) {
                unreadNotifications.add(notif);
                updateNotificationBadge();
                
                // 토스트 알림 표시
                showToastNotification(title, message);
            }
        } catch (Exception e) {
            System.err.println("알림 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 태그 메시지 처리
    private void handleTagMessage(String[] parts) {
        try {
            int tagId = Integer.parseInt(parts[1]);
            String tagName = parts[2];
            String color = parts[3];
            
            TagDTO tag = new TagDTO();
            tag.tagId = tagId;
            tag.tagName = tagName;
            tag.color = color;
            
            projectTags.add(tag);
            updateTagPanel();
            
        } catch (Exception e) {
            System.err.println("태그 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 태그 생성 처리
    private void handleTagCreated(String[] parts) {
        try {
            int tagId = Integer.parseInt(parts[1]);
            String tagName = parts[2];
            String color = parts[3];
            
            TagDTO tag = new TagDTO();
            tag.tagId = tagId;
            tag.tagName = tagName;
            tag.color = color;
            
            projectTags.add(tag);
            updateTagPanel();
            
            JOptionPane.showMessageDialog(this, "태그가 생성되었습니다: " + tagName);
            
        } catch (Exception e) {
            System.err.println("태그 생성 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 작업 태그 지정 처리
    private void handleTaskTagged(String[] parts) {
        // 작업에 태그가 추가됨
        JOptionPane.showMessageDialog(this, "작업에 태그가 추가되었습니다.");
    }
    
    // 프로젝트 통계 처리
    private void handleProjectStats(String[] parts) {
        try {
            int totalTasks = Integer.parseInt(parts[1]);
            int completedTasks = Integer.parseInt(parts[2]);
            int inProgressTasks = Integer.parseInt(parts[3]);
            int overdueTasks = Integer.parseInt(parts[4]);
            int memberCount = Integer.parseInt(parts[5]);
            int totalComments = Integer.parseInt(parts[6]);
            
            // 대시보드가 열려있으면 업데이트
            if (statsDialog != null && statsDialog.isVisible() && !statLabels.isEmpty()) {
                statLabels.get("total").setText(String.valueOf(totalTasks));
                statLabels.get("completed").setText(String.valueOf(completedTasks));
                statLabels.get("inProgress").setText(String.valueOf(inProgressTasks));
                statLabels.get("overdue").setText(String.valueOf(overdueTasks));
                statLabels.get("members").setText(String.valueOf(memberCount));
                statLabels.get("comments").setText(String.valueOf(totalComments));
                
                statsDialog.repaint();
            }
            
        } catch (Exception e) {
            System.err.println("프로젝트 통계 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 사용자 통계 처리
    private void handleUserStats(String[] parts) {
        try {
            int assignedTasks = Integer.parseInt(parts[1]);
            int createdTasks = Integer.parseInt(parts[2]);
            int completedTasks = Integer.parseInt(parts[3]);
            int overdueTasks = Integer.parseInt(parts[4]);
            
            // 사용자 통계 표시
            // TODO: 통계 UI 업데이트
            
        } catch (Exception e) {
            System.err.println("사용자 통계 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 프로젝트 생성 처리
    private void handleProjectCreated(String[] parts) {
        try {
            int projectId = Integer.parseInt(parts[1]);
            String projectName = parts[2];
            
            ProjectDTO project = new ProjectDTO();
            project.projectId = projectId;
            project.projectName = projectName;
            
            userProjects.add(project);
            projectSelector.addItem(projectName);
            
            JOptionPane.showMessageDialog(this, "프로젝트가 생성되었습니다: " + projectName);
            
        } catch (Exception e) {
            System.err.println("프로젝트 생성 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 프로젝트 전환 처리
    private void handleProjectSwitched(String[] parts) {
        try {
            int newProjectId = Integer.parseInt(parts[1]);
            currentProjectId = newProjectId;
            
            taskTableModel.setRowCount(0);
            projectTags.clear();
            tagPanel.removeAll();
            tagPanel.revalidate();
            tagPanel.repaint();

            statusLabel.setText(" 프로젝트 전환됨 (ID: " + newProjectId + ")");
            
        } catch (Exception e) {
            System.err.println("프로젝트 전환 처리 오류");
            e.printStackTrace();
        }
    }
    
    // 파일 업로드 처리
    private void handleFileUploaded(String[] parts) {
        try {
            int taskId = Integer.parseInt(parts[1]);
            String fileName = parts[2];
            long fileSize = Long.parseLong(parts[3]);

            // 시스템 메시지로 알림
            chatArea.append("[시스템] 파일 업로드: " + fileName + " (" + formatFileSize(fileSize) + ")\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());

            if (taskId == selectedTaskId) {
                // 목록을 싹 비우고 다시 요청
                if (fileListModel != null)
                    fileListModel.clear();
                out.println("GET_ATTACHMENTS|" + taskId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 첨부파일 목록 받기 (ID 저장 기능 추가)
    private void handleAttachment(String[] parts) {
        try {
            int attachmentId = Integer.parseInt(parts[1]);
            String fileName = parts[2];
            long fileSize = Long.parseLong(parts[3]);
            String uploader = parts[4];

            String sizeStr = formatFileSize(fileSize);
            String itemText = String.format("%s (%s) - %s", fileName, sizeStr, uploader);

            // 1. 메인 화면 오른쪽 패널에 추가
            if (fileListModel != null) {
                fileListModel.addElement(itemText);
                attachmentIds.add(attachmentId);
            }

            // 2. 상세 보기 창에도 추가!
            if (dialogAttachmentModel != null) {
                dialogAttachmentModel.addElement(itemText);
                dialogAttachmentIds.add(attachmentId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // UI 업데이트 메서드들
    private void updateCommentPanel(int taskId) {
        if (selectedTaskId != taskId)
            return;

        // 1. 메인 화면(오른쪽 패널) 갱신
        try {
            JPanel cPanel = (JPanel) rightPanel.getComponentAt(1);
            JScrollPane cScroll = (JScrollPane) cPanel.getComponent(0);
            JTextArea cArea = (JTextArea) cScroll.getViewport().getView();

            StringBuilder sb = new StringBuilder();
            if (taskComments.containsKey(taskId)) {
                for (CommentDTO comment : taskComments.get(taskId)) {
                    sb.append("[").append(comment.username).append("] ");
                    sb.append(comment.content);
                    if (comment.isEdited)
                        sb.append(" (수정됨)");
                    sb.append("\n\n");
                }
            }
            cArea.setText(sb.toString());
        } catch (Exception e) {
        }

        // 2. 상세 보기 창(다이얼로그)이 열려있다면 거기도 갱신!
        // (상세창은 모달이라 찾기 힘들 수 있지만, 닫았다가 다시 열면 자동 갱신됨)
        // 하지만 실시간성을 위해 창을 다시 그릴 수는 없으니, 사용자가
        // '수정/삭제' 후에는 보통 창이 닫히지 않지만 목록은 갱신되어야 함.
        // 현재 구조상 다이얼로그 내부 JList에 직접 접근이 어려우므로
        // 가장 쉬운 방법은 "댓글 변경 시 다이얼로그 닫기"는 불편하니까
        // 다이얼로그가 열려있다면 리스트를 갱신해줘야 함.

        // ★ 팁: 다이얼로그가 열려있는지 확인하는 복잡한 코드 대신,
        // 사용자가 '수정'이나 '삭제'를 하면 서버가 전체 목록을 다시 보내주므로
        // 상세창을 닫았다가 다시 여는 게 가장 확실합니다.
        // (여기서는 메인 화면 갱신만 처리합니다.)
    }
    
    // 태그 패널 업데이트 메서드 (디자인 수정 및 기능 복구)
    private void updateTagPanel() {
        if (tagPanel == null)
            return;
        tagPanel.removeAll(); // 기존 버튼 지우기

        // 1. 버튼 생성 (흰 배경, 검정 글씨)
        JButton addBtn = new JButton("+ 태그 추가");
        addBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        addBtn.setBackground(Color.WHITE);
        addBtn.setForeground(Color.BLACK);
        addBtn.setFocusPainted(false);
        // 클릭 시 다이얼로그 띄우기
        addBtn.addActionListener(e -> showAddTagDialog());
        tagPanel.add(addBtn);

        // 2. [태그] 버튼들 생성 (색상 배경, 흰색 글씨)
        if (projectTags != null) {
            for (TagDTO tag : projectTags) {
                JButton tagBtn = new JButton(tag.tagName);
                tagBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));

                // 배경색 설정
                Color bgColor = Color.GRAY;
                try {
                    if (tag.color != null)
                        bgColor = Color.decode(tag.color);
                } catch (Exception e) {
                }

                tagBtn.setBackground(bgColor);
                tagBtn.setForeground(Color.WHITE); // 글씨는 무조건 흰색

                // 버튼 모양 꽉 채우기 (맥/윈도우 호환성)
                tagBtn.setOpaque(true);
                tagBtn.setBorderPainted(false);
                tagBtn.setFocusPainted(false);

                // 클릭 시 필터링
                tagBtn.addActionListener(e -> {
                    out.println("FILTER_BY_TAG|" + tag.tagId);
                });
                tagPanel.add(tagBtn);
            }
        }

        // 화면 새로고침
        tagPanel.revalidate();
        tagPanel.repaint();
    }
    
    // 토스트 알림 표시
    private void showToastNotification(String title, String message) {
        JWindow toast = new JWindow();
        toast.setAlwaysOnTop(true);
        
        JPanel toastPanel = new JPanel(new BorderLayout(10, 10));
        toastPanel.setBackground(new Color(52, 73, 94));
        toastPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(41, 128, 185), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel messageLabel = new JLabel("<html>" + message + "</html>");
        messageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(236, 240, 241));
        
        toastPanel.add(titleLabel, BorderLayout.NORTH);
        toastPanel.add(messageLabel, BorderLayout.CENTER);
        
        toast.setContentPane(toastPanel);
        toast.setSize(300, 100);
        
        // 화면 오른쪽 하단에 위치
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        toast.setLocation(screenSize.width - 320, screenSize.height - 150);
        
        toast.setVisible(true);
        
        // 3초 후 자동으로 닫힘
        Timer timer = new Timer(3000, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }
    
    // 유틸리티 메서드
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    // 메인 메서드 (LoginWindow에서 호출)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 테스트용: new CollaborationClientUpgraded("admin", 1, "ADMIN");
        });
    }
    // [CollaborationClient.java] 맨 아래에 추가

    // 서버에서 프로젝트 정보를 하나씩 받을 때 처리하는 메서드
    private void handleProjectItem(String[] parts) {
        try {
            int pid = Integer.parseInt(parts[1]);
            String pname = parts[2];
            String pdesc = parts.length > 3 ? parts[3] : "";

            ProjectDTO p = new ProjectDTO();
            p.projectId = pid;
            p.projectName = pname;
            p.description = pdesc;

            userProjects.add(p);
            projectSelector.addItem(pname);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 태그 응답 처리 (로그 추가 + 화면 갱신 강화)
    private void handleTaskTagsResponse(String[] parts) {
        // 로그: 서버가 뭐라고 보냈는지 확인
        System.out.println("[디버그] 태그 데이터 수신: " + (parts.length > 2 ? parts[2] : "없음"));

        try {
            int tId = Integer.parseInt(parts[1]);
            // 엉뚱한 작업 태그면 무시
            if (tId != selectedTaskId || detailsTagPanel == null)
                return;

            detailsTagPanel.removeAll(); // "로딩 중..." 지우기

            // 데이터가 없거나 비어있으면 "(없음)" 표시
            if (parts.length < 3 || parts[2].trim().isEmpty()) {
                JLabel noTag = new JLabel("(없음)");
                noTag.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                noTag.setForeground(Color.LIGHT_GRAY);
                detailsTagPanel.add(noTag);
            } else {
                // 데이터가 있으면 태그 라벨 생성
                String[] tags = parts[2].split(";");
                for (String tagStr : tags) {
                    if (tagStr.trim().isEmpty())
                        continue;

                    String[] t = tagStr.split(","); // 0:id, 1:이름, 2:색상
                    if (t.length < 3)
                        continue;

                    JLabel tagLbl = new JLabel(" " + t[1] + " ");
                    tagLbl.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                    tagLbl.setOpaque(true);

                    try {
                        Color bg = Color.decode(t[2]);
                        tagLbl.setBackground(bg);
                        tagLbl.setForeground(Color.WHITE);
                    } catch (Exception e) {
                        tagLbl.setBackground(Color.GRAY);
                        tagLbl.setForeground(Color.WHITE);
                    }

                    // 둥근 테두리 느낌
                    tagLbl.setBorder(BorderFactory.createLineBorder(tagLbl.getBackground(), 1));

                    detailsTagPanel.add(tagLbl);
                    detailsTagPanel.add(Box.createHorizontalStrut(5));
                }
            }

            // 화면 갱신 (이게 중요함!)
            detailsTagPanel.revalidate();
            detailsTagPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 파일 다운로드 응답 처리 (저장 창 띄우기)
    private void handleFileDownloadResponse(String[] parts) {
        try {
            String fileName = parts[1];
            String base64Data = parts[2];

            // 저장할 위치 선택 창 띄우기
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName)); // 기본 파일명 지정

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();

                // 암호문(Base64)을 다시 파일(byte)로 변환
                byte[] fileData = Base64.getDecoder().decode(base64Data);

                // 파일 쓰기
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    fos.write(fileData);
                    JOptionPane.showMessageDialog(this, "다운로드가 완료되었습니다!\n" + saveFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "파일 저장 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }
    
    // 대시보드가 열려있으면 통계 새로고침 요청
    private void refreshDashboardIfOpen() {
        if (statsDialog != null && statsDialog.isVisible()) {
            out.println("GET_STATISTICS");
            System.out.println("[클라이언트] 대시보드 실시간 갱신 요청 보냄");
        }
    }
    
    // 히스토리 데이터 표시
    private void handleHistoryResponse(String[] parts) {
        try {
            int tId = Integer.parseInt(parts[1]);
            if (tId != selectedTaskId || detailsHistoryArea == null)
                return;

            String rawData = (parts.length > 2) ? parts[2] : "";
            if (rawData.isEmpty()) {
                detailsHistoryArea.setText("변경 이력이 없습니다.");
            } else {
                // "///"를 줄바꿈으로 바꿔서 출력
                String logText = rawData.replace("///", "\n");
                detailsHistoryArea.setText(logText);
            }
            // 스크롤 맨 위로
            detailsHistoryArea.setCaretPosition(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}