import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseConfig {
    private static Connection connection = null;
    private static Properties properties = new Properties();

    // 클래스가 실행될 때 설정 파일을 찾아서 읽습니다.
    static {
        try {
            // 1. 프로젝트 최상위 폴더(루트)에서 찾기
            File file = new File("db.properties");
            if (file.exists()) {
                System.out.println("[DB설정] 파일 찾음(루트): " + file.getAbsolutePath());
                try (FileInputStream fis = new FileInputStream(file)) {
                    properties.load(fis);
                }
            } else {
                // 2. 없으면 src 폴더(클래스패스) 안에서 찾기
                System.out.println("[DB설정] 루트에 파일이 없어 내부(src)에서 검색합니다...");
                InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties");

                if (is != null) {
                    properties.load(is);
                    System.out.println("[DB설정] src 폴더 내에서 설정 파일 로드 성공!");
                } else {
                    // 3. 그래도 없으면 에러 출력
                    System.err.println("====================================================");
                    System.err.println("[치명적 오류] db.properties 파일을 찾을 수 없습니다!");
                    System.err.println("현재 프로그램이 파일을 찾는 위치: " + System.getProperty("user.dir"));
                    System.err.println("db.properties 파일이 프로젝트 폴더 최상위에 있는지 확인해주세요.");
                    System.err.println("====================================================");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 데이터베이스 연결
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // 설정값이 비어있는지 확인
                if (properties.isEmpty()) {
                    System.err.println("[오류] DB 설정값이 로드되지 않았습니다.");
                    return null;
                }

                Class.forName("com.mysql.cj.jdbc.Driver");

                connection = DriverManager.getConnection(
                        properties.getProperty("db.url"),
                        properties.getProperty("db.user"),
                        properties.getProperty("db.password"));
                System.out.println(" MySQL 연결 성공");
            }
            return connection;
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC 드라이버를 찾을 수 없습니다.");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.err.println(" 데이터베이스 연결 실패 (아이디/비번/주소를 확인하세요)");
            e.printStackTrace();
            return null;
        }
    }

    // 연결 종료
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] 연결 종료");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close(ResultSet rs, PreparedStatement pstmt) {
        try {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close(PreparedStatement pstmt) {
        try {
            if (pstmt != null)
                pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}