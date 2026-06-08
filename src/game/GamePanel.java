package game;

import game.entity.Bullet;
import game.entity.Enemy;
import game.entity.Player;
import game.map.TileMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;
    private static final long SHOT_COOLDOWN_NS = 150_000_000L;
    private static final int MAX_ENEMIES = 5;

    private final TileMap map = TileMap.basicDemo();
    private final Player player = new Player(2, 2, map);
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random = new Random();

    private Thread loopThread;
    private volatile boolean running;

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean firing;

    private long lastShotNs;

    public GamePanel() {
        setPreferredSize(map.getPixelSize());
        setBackground(Color.BLACK);
        setFocusable(true);
        setupKeyBindings();
        spawnInitialEnemies();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        loopThread = new Thread(this, "game-loop");
        loopThread.start();
        requestFocusInWindow();
    }

    public void stop() {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastTime;
            if (elapsed >= FRAME_TIME_NS) {
                updateGame(now, elapsed / 1_000_000_000.0f);
                repaint();
                lastTime = now;
            } else {
                long sleepMs = (FRAME_TIME_NS - elapsed) / 1_000_000L;
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void updateGame(long nowNs, float deltaSeconds) {
        // player move + colision check quick
        player.update(deltaSeconds, up, down, left, right, map);

        if (firing && nowNs - lastShotNs > SHOT_COOLDOWN_NS) {
            // tiny cooldown so spam isnt crazy
            bullets.add(new Bullet(
                    player.getX() + player.getWidth() / 2.0f,
                    player.getY()
            ));
            lastShotNs = nowNs;
        }

        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            bullet.update(deltaSeconds);
            if (!bullet.isActive()) {
                // clean dead bullet fast
                bulletIter.remove();
            }
        }

        handleCollisions();
        maintainEnemyCount();
    }

    private void handleCollisions() {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            Rectangle bulletRect = bullet.getBounds();

            Iterator<Enemy> enemyIter = enemies.iterator();
            while (enemyIter.hasNext()) {
                Enemy enemy = enemyIter.next();
                if (bulletRect.intersects(enemy.getBounds())) {
                    // hit confirm -> remove both
                    enemyIter.remove();
                    bullet.deactivate();
                    break;
                }
            }

            if (!bullet.isActive()) {
                bulletIter.remove();
            }
        }
    }

    private void maintainEnemyCount() {
        while (enemies.size() < MAX_ENEMIES) {
            spawnEnemy();
        }
    }

    private void spawnInitialEnemies() {
        spawnEnemy();
        spawnEnemy();
        spawnEnemy();
    }

    private void spawnEnemy() {
        for (int attempts = 0; attempts < 32; attempts++) {
            java.awt.Point tile = map.getRandomWalkableTile(random);
            if (tile == null) {
                return;
            }

            int playerCol = map.worldToCol(player.getX());
            int playerRow = map.worldToRow(player.getY());
            if (tile.x == playerCol && tile.y == playerRow) {
                continue;
            }

            boolean occupied = false;
            for (Enemy enemy : enemies) {
                if (enemy.getCol() == tile.x && enemy.getRow() == tile.y) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) {
                continue;
            }

            // spawn stays still for now
            enemies.add(new Enemy(map, tile.x, tile.y));
            return;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        renderMap(g2);
        renderPlayer(g2);
        renderBullets(g2);
        renderEnemies(g2);
        renderHud(g2);

        g2.dispose();
    }

    private void renderMap(Graphics2D g2) {
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                int tile = map.getTile(col, row);
                int x = col * TileMap.TILE_SIZE;
                int y = row * TileMap.TILE_SIZE;

                switch (tile) {
                    case TileMap.WALL:
                        // wall tile darker feel
                        g2.setColor(Color.DARK_GRAY);
                        g2.fillRect(x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                        break;
                    case TileMap.HOLE:
                        g2.setColor(Color.BLACK);
                        g2.fillRect(x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                        g2.setColor(Color.GRAY);
                        g2.drawRect(x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                        break;
                    default:
                        g2.setColor(new Color(30, 30, 30));
                        g2.fillRect(x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                        g2.setColor(new Color(40, 40, 40));
                        g2.drawRect(x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                        break;
                }
            }
        }
    }

    private void renderPlayer(Graphics2D g2) {
        g2.setColor(Color.CYAN);
        g2.fillRect(Math.round(player.getX()), Math.round(player.getY()), player.getWidth(), player.getHeight());
    }

    private void renderBullets(Graphics2D g2) {
        g2.setColor(Color.YELLOW);
        for (Bullet bullet : bullets) {
            g2.fillRect(Math.round(bullet.getX()), Math.round(bullet.getY()), bullet.getWidth(), bullet.getHeight());
        }
    }

    private void renderEnemies(Graphics2D g2) {
        g2.setColor(Color.RED);
        for (Enemy enemy : enemies) {
            g2.fillRect(Math.round(enemy.getX()), Math.round(enemy.getY()), enemy.getWidth(), enemy.getHeight());
        }
    }

    private void renderHud(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.drawString("WASD/Arrows to move, Space to shoot", 12, 18);
    }

    private void setupKeyBindings() {
        int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = getInputMap(condition);
        ActionMap actionMap = getActionMap();

        registerKey(inputMap, actionMap, KeyEvent.VK_W, "moveUp", () -> up = true, () -> up = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_UP, "moveUpArrow", () -> up = true, () -> up = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_S, "moveDown", () -> down = true, () -> down = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_DOWN, "moveDownArrow", () -> down = true, () -> down = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_A, "moveLeft", () -> left = true, () -> left = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_LEFT, "moveLeftArrow", () -> left = true, () -> left = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_D, "moveRight", () -> right = true, () -> right = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_RIGHT, "moveRightArrow", () -> right = true, () -> right = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_SPACE, "fire", () -> firing = true, () -> firing = false);
    }

    private void registerKey(InputMap inputMap, ActionMap actionMap,
                             int keyCode, String name,
                             Runnable onPress, Runnable onRelease) {
        KeyStroke press = KeyStroke.getKeyStroke(keyCode, 0, false);
        KeyStroke release = KeyStroke.getKeyStroke(keyCode, 0, true);

        String pressName = name + "Pressed";
        String releaseName = name + "Released";

        inputMap.put(press, pressName);
        actionMap.put(pressName, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onPress.run();
            }
        });

        inputMap.put(release, releaseName);
        actionMap.put(releaseName, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // release stops action quick
                onRelease.run();
            }
        });
    }
}
