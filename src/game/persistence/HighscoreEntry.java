package game.persistence;

// lil dto so ui has name + score rows
public record HighscoreEntry(String playerName, int score) {
}
