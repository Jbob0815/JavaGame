import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import game.GamePanel;

public class main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2D Shooter");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            // lil window set up, super basic

            GamePanel panel = new GamePanel();
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    panel.stop();
                }
            });

            // boot up the game loop
            panel.start();
        });
    }
}
