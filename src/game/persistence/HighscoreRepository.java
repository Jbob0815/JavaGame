package game.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HighscoreRepository implements AutoCloseable {
    private final Path dbDirectory;
    private final String jdbcUrl;
    private Connection connection;

    public HighscoreRepository(Path dbDirectory) {
        this.dbDirectory = dbDirectory;
        this.jdbcUrl = "jdbc:h2:" + dbDirectory.resolve("highscores").toAbsolutePath();
    }

    public void initStorage() throws SQLException {
        try {
            Files.createDirectories(dbDirectory);
        } catch (Exception ignored) {
        }
        connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS highscores (id IDENTITY PRIMARY KEY, player_name VARCHAR(64) NOT NULL, score INT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");
        }
    }

    public void insertScore(String playerName, int score) throws SQLException {
        String sql = "INSERT INTO highscores (player_name, score) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setInt(2, score);
            ps.executeUpdate();
        }
    }

    public List<HighscoreEntry> topScores(int limit) throws SQLException {
        String sql = "SELECT player_name, score FROM highscores ORDER BY score DESC, created_at ASC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<HighscoreEntry> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new HighscoreEntry(rs.getString(1), rs.getInt(2)));
                }
                return result;
            }
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
