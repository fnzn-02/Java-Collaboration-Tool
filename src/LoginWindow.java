import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private UserDAO userDAO;
    
    public LoginWindow() {
        userDAO = new UserDAO();
        
        setTitle("실시간 협업 시스템 - 로그인");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initUI();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initUI() {
        // 상단 타이틀
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setBackground(new Color(93, 156, 236));
        titlePanel.setPreferredSize(new Dimension(500, 100));
        
        JLabel titleLabel = new JLabel("실시간 협업 시스템");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Team Collaboration Platform");
        subtitleLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(236, 240, 241));
        
        JPanel titleTextPanel = new JPanel();
        titleTextPanel.setLayout(new BoxLayout(titleTextPanel, BoxLayout.Y_AXIS));
        titleTextPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleTextPanel.add(titleLabel);
        titleTextPanel.add(Box.createVerticalStrut(5));
        titleTextPanel.add(subtitleLabel);
        
        titlePanel.add(titleTextPanel);
        
        // 폼 패널
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // 사용자명
        JLabel userLabel = new JLabel("사용자명:");
        userLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(userLabel, gbc);
        
        usernameField = new JTextField();
        usernameField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(250, 35));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        formPanel.add(usernameField, gbc);
        
        // 비밀번호
        JLabel passLabel = new JLabel("비밀번호:");
        passLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formPanel.add(passLabel, gbc);
        
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(250, 35));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        formPanel.add(passwordField, gbc);
        
        passwordField.addActionListener(e -> handleLogin());
        
        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        
        JButton loginButton = new JButton("로그인");
        loginButton.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        loginButton.setPreferredSize(new Dimension(140, 45));
        loginButton.setBackground(new Color(52, 152, 219));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        
        JButton registerButton = new JButton("회원가입");
        registerButton.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        registerButton.setPreferredSize(new Dimension(140, 45));
        registerButton.setBackground(new Color(46, 204, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> showRegisterDialog());
        
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        
        // 하단 정보
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel infoLabel = new JLabel("v2.0 - 권한, 담당자, 알림, 댓글, 태그, 통계 기능 포함");
        infoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(127, 140, 141));
        infoPanel.add(infoLabel);
        
        add(titlePanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(infoPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "사용자명과 비밀번호를 입력하세요.", 
                "입력 오류", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 로그인 처리 - 역할 정보도 함께 조회
        UserLoginInfo loginInfo = loginUser(username, password);
        
        if (loginInfo != null) {
            dispose();
            new CollaborationClient(username, loginInfo.userId, loginInfo.role);
        } else {
            JOptionPane.showMessageDialog(this, 
                "사용자명 또는 비밀번호가 잘못되었습니다.", 
                "로그인 실패", 
                JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }
    
    // 역할 정보 포함 로그인
    private UserLoginInfo loginUser(String username, String password) {
        String sql = "SELECT user_id, role FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                UserLoginInfo info = new UserLoginInfo();
                info.userId = rs.getInt("user_id");
                info.role = rs.getString("role");
                info.username = username;
                
                // 마지막 로그인 시간 업데이트
                updateLastLogin(info.userId);
                
                System.out.println("[로그인 성공] " + username + " (역할: " + info.role + ")");
                return info;
            }
            
            return null;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 마지막 로그인 시간 업데이트
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void showRegisterDialog() {
        JDialog dialog = new JDialog(this, "회원가입", true);
        dialog.setSize(500, 450);
        dialog.setLayout(new BorderLayout(15, 15));
        
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));
        
        // 사용자명
        JLabel userLabel = new JLabel("사용자명:");
        userLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        JTextField userField = new JTextField();
        userField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        // 비밀번호
        JLabel passLabel = new JLabel("비밀번호:");
        passLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        JPasswordField passField = new JPasswordField();
        passField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        // 비밀번호 확인
        JLabel confirmLabel = new JLabel("비밀번호 확인:");
        confirmLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        JPasswordField confirmField = new JPasswordField();
        confirmField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        // 이메일
        JLabel emailLabel = new JLabel("이메일:");
        emailLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        JTextField emailField = new JTextField();
        emailField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        // 역할 
        JLabel roleLabel = new JLabel("역할:");
        roleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        String[] roles = {"MEMBER", "MANAGER"};
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        roleCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        formPanel.add(userLabel);
        formPanel.add(userField);
        formPanel.add(passLabel);
        formPanel.add(passField);
        formPanel.add(confirmLabel);
        formPanel.add(confirmField);
        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(roleLabel);
        formPanel.add(roleCombo);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton registerBtn = new JButton("가입하기");
        registerBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        registerBtn.setPreferredSize(new Dimension(120, 35));
        registerBtn.setBackground(new Color(46, 204, 113));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setBorderPainted(false);
        
        JButton cancelBtn = new JButton("취소");
        cancelBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        cancelBtn.setPreferredSize(new Dimension(120, 35));
        
        registerBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            String confirm = new String(confirmField.getPassword());
            String email = emailField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "사용자명과 비밀번호는 필수입니다.");
                return;
            }
            
            if (!password.equals(confirm)) {
                JOptionPane.showMessageDialog(dialog, "비밀번호가 일치하지 않습니다.");
                return;
            }
            
            if (isUsernameExists(username)) {
                JOptionPane.showMessageDialog(dialog, "이미 존재하는 사용자명입니다.");
                return;
            }
            
            if (registerUser(username, password, email, role)) {
                JOptionPane.showMessageDialog(dialog, "회원가입 완료! 로그인해주세요.");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "회원가입 실패. 다시 시도해주세요.");
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    // 회원가입
    private boolean registerUser(String username, String password, String email, String role) {
        String sql = "INSERT INTO users (username, password, salt, email, role) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, "test_salt");  // 임시 salt 값
            pstmt.setString(4, email);
            pstmt.setString(5, role);
            
            int result = pstmt.executeUpdate();
            System.out.println("[회원가입 완료] " + username + " (역할: " + role + ")");
            return result > 0;
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.err.println("[DB] 중복된 사용자명: " + username);
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    // 사용자명 중복 체크
    private boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 로그인 정보 DTO
    private static class UserLoginInfo {
        int userId;
        String username;
        String role;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LoginWindow();
        });
    }
}