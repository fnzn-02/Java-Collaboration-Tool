import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class UserDAO {
    
    // 비밀번호 해싱 관련 메서드
    
    /**
     * 비밀번호를 SHA-256으로 해시화
     * @param password 평문 비밀번호
     * @param salt 솔트값
     * @return 해시된 비밀번호
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
    
    /**
     * 랜덤 솔트 생성
     * @return Base64로 인코딩된 솔트
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * 비밀번호 검증
     * @param password 평문 비밀번호
     * @param hashedPassword DB에 저장된 해시값
     * @param salt DB에 저장된 솔트
     * @return 일치 여부
     */
    private boolean verifyPassword(String password, String hashedPassword, String salt) {
        String hashToVerify = hashPassword(password, salt);
        return hashToVerify.equals(hashedPassword);
    }
    
    // 권한 체크
    
    public boolean hasPermission(int userId, String permission) {
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return checkPermission(role, permission);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean checkPermission(String role, String permission) {
        if (role == null) role = "MEMBER";
        
        switch (role) {
            case "ADMIN":
                return true; // 모든 권한
            case "MANAGER":
                return !permission.equals("DELETE_USER"); // 사용자 삭제 제외
            case "MEMBER":
                return permission.equals("CREATE_TASK") || 
                       permission.equals("UPDATE_OWN_TASK") ||
                       permission.equals("COMMENT") ||
                       permission.equals("COMPLETE_TASK");
            default:
                return false;
        }
    }
    
    // 회원가입 (해싱 적용)
    
    public boolean registerUser(String username, String password, String email) {
        String sql = "INSERT INTO users (username, password, salt, email) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            
            // 솔트 생성 및 비밀번호 해싱
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, salt);
            pstmt.setString(4, email);
            
            int result = pstmt.executeUpdate();
            System.out.println("[DB] 회원가입 완료: " + username);
            return result > 0;
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.err.println("[DB] 중복된 사용자명 또는 이메일: " + username);
            } else {
                e.printStackTrace();
            }
            return false;
        } finally {
            DatabaseConfig.close(pstmt);
        }
    }
    
    // 로그인 (해싱 검증)
    
    public int loginUser(String username, String password) {
        String sql = "SELECT user_id, password, salt, is_active FROM users WHERE username = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String storedHash = rs.getString("password");
                String salt = rs.getString("salt");
                boolean isActive = rs.getBoolean("is_active");
                
                // 계정 활성화 확인
                if (!isActive) {
                    System.out.println("[DB] 로그인 실패: 비활성화된 계정");
                    return -2; // 비활성화된 계정
                }
                
                // 비밀번호 검증
                if (verifyPassword(password, storedHash, salt)) {
                    // 마지막 로그인 시간 업데이트
                    updateLastLogin(userId);
                    System.out.println("[DB] 로그인 성공: " + username);
                    return userId;
                } else {
                    System.out.println("[DB] 로그인 실패: 잘못된 비밀번호");
                    return -1;
                }
            }
            System.out.println("[DB] 로그인 실패: 존재하지 않는 사용자");
            return -1;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            DatabaseConfig.close(rs, pstmt);
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
    
    // 비밀번호 변경
    
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        String selectSql = "SELECT password, salt FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET password = ?, salt = ? WHERE user_id = ?";
        
        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            
            // 기존 비밀번호 확인
            selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setInt(1, userId);
            rs = selectStmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password");
                String salt = rs.getString("salt");
                
                // 기존 비밀번호 검증
                if (!verifyPassword(oldPassword, storedHash, salt)) {
                    System.out.println("[DB] 비밀번호 변경 실패: 기존 비밀번호 불일치");
                    return false;
                }
                
                // 새 비밀번호 해싱
                String newSalt = generateSalt();
                String newHash = hashPassword(newPassword, newSalt);
                
                // 비밀번호 업데이트
                updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, newHash);
                updateStmt.setString(2, newSalt);
                updateStmt.setInt(3, userId);
                
                int result = updateStmt.executeUpdate();
                System.out.println("[DB] 비밀번호 변경 완료");
                return result > 0;
            }
            
            return false;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.close(rs, selectStmt);
            DatabaseConfig.close(updateStmt);
        }
    }
    
    // 비밀번호 재설정 (관리자용)
    
    public boolean resetPassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ?, salt = ? WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String salt = generateSalt();
            String hashedPassword = hashPassword(newPassword, salt);
            
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, salt);
            pstmt.setInt(3, userId);
            
            int result = pstmt.executeUpdate();
            System.out.println("[DB] 비밀번호 재설정 완료");
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자명으로 ID 조회
    
    public int getUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("user_id");
            }
            return -1;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            DatabaseConfig.close(rs, pstmt);
        }
    }
    
    // ID로 사용자명 조회
    
    public String getUsernameById(int userId) {
        String sql = "SELECT username FROM users WHERE user_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("username");
            }
            return null;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConfig.close(rs, pstmt);
        }
    }
    
    // 사용자 정보 조회 (DTO)
    
    public UserDTO getUserById(int userId) {
        String sql = "SELECT user_id, username, email, role, is_active, last_login " +
                     "FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                UserDTO user = new UserDTO();
                user.userId = rs.getInt("user_id");
                user.username = rs.getString("username");
                user.email = rs.getString("email");
                user.role = rs.getString("role");
                user.lastLogin = rs.getTimestamp("last_login");
                return user;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // 사용자명 중복 체크
    
    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            Connection conn = DatabaseConfig.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.close(rs, pstmt);
        }
    }
    
    // 이메일 중복 체크
    
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
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
    
    // 사용자 활성화/비활성화
    
    public boolean setUserActive(int userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, isActive);
            pstmt.setInt(2, userId);
            
            int result = pstmt.executeUpdate();
            System.out.println("[DB] 사용자 " + (isActive ? "활성화" : "비활성화") + " 완료");
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}