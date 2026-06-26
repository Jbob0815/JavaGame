package game.persistence;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class HighscoreService implements AutoCloseable {
    private static final int DEFAULT_LIMIT = 10;
    private final HighscoreRepository repository;

    public HighscoreService(Path storageDir) throws SQLException {
        this.repository = new HighscoreRepository(storageDir);
        this.repository.initStorage();
    }

    public void recordScore(String playerName, int score) {
        String actualName = (playerName == null || playerName.isBlank()) ? "player 1" : playerName.trim();
        try {
            repository.insertScore(actualName, score);
        } catch (SQLException ignored) {
            // swallowing here so the game keeps running, we just skip storing
        }
    }

    public List<HighscoreEntry> loadTopScores() {
        try {
            return repository.topScores(DEFAULT_LIMIT);
        } catch (SQLException ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
        repository.close();
    }
}
