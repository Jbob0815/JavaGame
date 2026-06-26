import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import game.GamePanel;
import game.persistence.HighscoreService;

public class main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HighscoreService highscoreService;
            try {
                highscoreService = new HighscoreService(Path.of("data"));
            } catch (Exception e) {
                highscoreService = null;
            }

            JFrame frame = new JFrame("2D Shooter");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            // lil window set up, super basic

            GamePanel panel = new GamePanel(highscoreService);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            HighscoreService finalHighscoreService = highscoreService;
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    panel.stop();
                    if (finalHighscoreService != null) {
                        finalHighscoreService.close();
                    }
                }
            });

            // boot up the game loop
            panel.start();
        });
    }
}
