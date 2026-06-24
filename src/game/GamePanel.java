package game;

import game.entity.Bullet;
import game.entity.Enemy;
import game.entity.Player;
import game.map.TileMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;
    private static final long SHOT_COOLDOWN_NS = 150_000_000L;
    private static final int MAX_ENEMIES = 5;
    private static final float ENEMY_FRAME_SECONDS = 0.18f;
    private static final float BULLET_FRAME_SECONDS = 0.1f;
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
    private boolean aimUp;
    private boolean aimDown;
    private boolean aimLeft;
    private boolean aimRight;
    // flag tells us when folks are dead
    private boolean gameOver;
    private long lastShotNs;
    private final BufferedImage floorImg;
    private final BufferedImage wallImg;
    private final BufferedImage holeImg;
    private final BufferedImage playerDownImg;
    private final BufferedImage playerRightImg;
    private final BufferedImage playerLeftImg;
    private final BufferedImage playerUpImg;
    private final BufferedImage[] enemyFrames;
    private final BufferedImage[] bulletFrames;
    private int enemyFrameIndex;
    private float enemyFrameTimer;
    private int bulletFrameIndex;
    private float bulletFrameTimer;
    private final BufferedImage playerSansFallback;
    private static final float PLAYER_FRAME_SECONDS = 0.2f;

    public GamePanel() {
        setPreferredSize(map.getPixelSize());
        setBackground(Color.BLACK);
        setFocusable(true);
        setupKeyBindings();
        spawnInitialEnemies();
        floorImg = loadImageOrThrow("Boden.png");
        wallImg = loadImageOrThrow("wand.png");
        holeImg = loadImageOrThrow("loch.png");
        // try grab the sans gif for lulz, fallback later
        playerSansFallback = tryLoadImage("sans.gif");
        BufferedImage downRaw = loadImageOrThrow("held-frame-0.png");
        BufferedImage rightRaw = loadImageOrThrow("held-frame-1.png");
        BufferedImage upRaw = loadImageOrThrow("held-frame-2.png");
        playerDownImg = downRaw;
        playerRightImg = rightRaw;
        playerLeftImg = flipHorizontally(rightRaw);
        playerUpImg = upRaw;
        enemyFrames = new BufferedImage[]{
                loadImageOrThrow("pixil-frame-0.png"),
                loadImageOrThrow("pixil-frame-1.png"),
                loadImageOrThrow("pixil-frame-2.png")
        };
        bulletFrames = new BufferedImage[]{
                loadImageOrThrow("bullet-0.png"),
                loadImageOrThrow("bullet-1.png"),
                loadImageOrThrow("bullet-2.png")
        };
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
        if (gameOver) {
            // quick bail when dead so nothing moves crazy
            return;
        }
        // player move + colision check quick
        player.update(deltaSeconds, up, down, left, right, map);

        float shootX = 0f;
        if (aimLeft) {
            shootX -= 1f;
        }
        if (aimRight) {
            shootX += 1f;
        }
        float shootY = 0f;
        if (aimUp) {
            shootY -= 1f;
        }
        if (aimDown) {
            shootY += 1f;
        }
        if (shootX == 0 && shootY == 0) {
            // nobody aiming, use last walk dir
            shootX = player.getAimX();
            shootY = player.getAimY();
        }

        if (firing && nowNs - lastShotNs > SHOT_COOLDOWN_NS) {
            // tiny cooldown so spam isnt crazy
            bullets.add(new Bullet(
                    player.getCenterX(),
                    player.getCenterY(),
                    shootX,
                    shootY
            ));
            lastShotNs = nowNs;
        }
        if (shootX != 0 || shootY != 0) {
            // keep face dir same as aim so art lines up
            player.updateAim(shootX, shootY);
        }

        //enemy movement updater
        Iterator<Enemy> enemyIter = enemies.iterator();
        while (enemyIter.hasNext()) {
            Enemy enemy = enemyIter.next();
            enemy.update(deltaSeconds, player);
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
        if (!gameOver) {
            maintainEnemyCount();
        }
        advanceAnimations(deltaSeconds);
    }

    private Player.Direction resolvePlayerDirection() {
        return player.determineDirection();
    }

    private void handleCollisions() {
        Rectangle playerRect = player.getBounds();

        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();

            if (!map.isAreaWalkable(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight())) {
                // bullet bonks wall so bye bye
                bullet.deactivate();
            }

            if (!bullet.isActive()) {
                bulletIter.remove();
                continue;
            }

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

        if (gameOver) {
            return;
        }

        for (Enemy enemy : enemies) {
            if (playerRect.intersects(enemy.getBounds())) {
                // bonk player and its gg
                triggerGameOver();
                break;
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
        for (int attempts = 0; attempts < 64; attempts++) {
            java.awt.Point tile = map.getRandomWalkableTile(random);
            if (tile == null) {
                return;
            }

            int playerCol = map.worldToCol(player.getCenterX());
            int playerRow = map.worldToRow(player.getCenterY());
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
        if (gameOver) {
            renderGameOver(g2);
        }

        g2.dispose();
    }

    private void renderMap(Graphics2D g2) {
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                int tile = map.getTile(col, row);
                int x = col * TileMap.TILE_SIZE;
                int y = row * TileMap.TILE_SIZE;

                g2.drawImage(floorImg, x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE, null);
                switch (tile) {
                    case TileMap.WALL:
                        // drop wall art quick
                        g2.drawImage(wallImg, x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE, null);
                        break;
                    case TileMap.HOLE:
                        // hole tile art so we see void lol
                        g2.drawImage(holeImg, x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE, null);
                        break;
                    default:
                        // floor already drawn above
                        break;
                }
            }
        }
    }

    private void renderPlayer(Graphics2D g2) {
        BufferedImage sprite = resolvePlayerSprite();
        int drawX = Math.round(player.getX());
        int drawY = Math.round(player.getY());
        if (sprite != null) {
            // draw held art so we see dirs proper
            g2.drawImage(sprite, drawX, drawY, player.getWidth(), player.getHeight(), null);
        } else if (playerSansFallback != null) {
            // sans meme stays backup buddy
            g2.drawImage(playerSansFallback, drawX, drawY, player.getWidth(), player.getHeight(), null);
        } else {
            // super last fallback if art missing
            g2.setColor(Color.CYAN);
            g2.fillRect(drawX, drawY, player.getWidth(), player.getHeight());
        }
    }

    private void renderBullets(Graphics2D g2) {
        BufferedImage bulletSprite = bulletFrames[bulletFrameIndex];
        for (Bullet bullet : bullets) {
            int x = Math.round(bullet.getX());
            int y = Math.round(bullet.getY());
            g2.drawImage(bulletSprite, x, y, bullet.getWidth(), bullet.getHeight(), null);
        }
    }

    private void renderEnemies(Graphics2D g2) {
        BufferedImage enemySprite = enemyFrames[enemyFrameIndex];
        for (Enemy enemy : enemies) {
            int x = Math.round(enemy.getX());
            int y = Math.round(enemy.getY());
            g2.drawImage(enemySprite, x, y, enemy.getWidth(), enemy.getHeight(), null);
        }
    }

    private void renderHud(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        // stops me forgeting which keys we messed with
        g2.drawString("wasd move, arrows aim, space shoot, R restart", 12, 18);
    }

    private void renderGameOver(Graphics2D g2) {
        // cheap overlay so we know its done
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 32f));
        g2.drawString("game over", 60, getHeight() / 2 - 20);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.drawString("press R to try again", 60, getHeight() / 2 + 20);
    }

    private void advanceAnimations(float deltaSeconds) {
        // cycle enemy frames slow so they bob
        enemyFrameTimer += deltaSeconds;
        if (enemyFrameTimer >= ENEMY_FRAME_SECONDS) {
            enemyFrameTimer -= ENEMY_FRAME_SECONDS;
            enemyFrameIndex = (enemyFrameIndex + 1) % enemyFrames.length;
        }
        // bullet flash faster cause pew pew
        bulletFrameTimer += deltaSeconds;
        if (bulletFrameTimer >= BULLET_FRAME_SECONDS) {
            bulletFrameTimer -= BULLET_FRAME_SECONDS;
            bulletFrameIndex = (bulletFrameIndex + 1) % bulletFrames.length;
        }
    }

    private BufferedImage resolvePlayerSprite() {
        float aimX = player.getAimX();
        float aimY = player.getAimY();
        if (Math.abs(aimX) < 0.01f && Math.abs(aimY) < 0.01f) {
            // chill default facing down
            return playerDownImg;
        }
        if (Math.abs(aimY) >= Math.abs(aimX)) {
            return aimY > 0 ? playerDownImg : playerUpImg;
        }
        return aimX > 0 ? playerRightImg : playerLeftImg;
    }

    private BufferedImage loadImageOrThrow(String fileName) {
        BufferedImage img = tryLoadImage(fileName);
        if (img == null) {
            throw new IllegalStateException("missing texture " + fileName);
        }
        return img;
    }

    private BufferedImage tryLoadImage(String fileName) {
        Path path = Path.of("images", fileName);
        if (!Files.exists(path)) {
            return null;
        }
        try (var in = Files.newInputStream(path)) {
            // keep read simple no caching fancy
            return ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage flipHorizontally(BufferedImage source) {
        if (source == null) {
            return null;
        }
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-source.getWidth(), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage flipped = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        op.filter(source, flipped);
        return flipped;
    }

    private void setupKeyBindings() {
        int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = getInputMap(condition);
        ActionMap actionMap = getActionMap();
        // got to wire keys or nothing moves lol
        registerKey(inputMap, actionMap, KeyEvent.VK_W, "moveUp", () -> up = true, () -> up = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_UP, "aimUpArrow", () -> aimUp = true, () -> aimUp = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_S, "moveDown", () -> down = true, () -> down = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_DOWN, "aimDownArrow", () -> aimDown = true, () -> aimDown = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_A, "moveLeft", () -> left = true, () -> left = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_LEFT, "aimLeftArrow", () -> aimLeft = true, () -> aimLeft = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_D, "moveRight", () -> right = true, () -> right = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_RIGHT, "aimRightArrow", () -> aimRight = true, () -> aimRight = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_SPACE, "fire", () -> firing = true, () -> firing = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_R, "restart", this::handleRestartPressed, () -> {});
        // r key sneaks in to reset things super quick
    }

    private void registerKey(InputMap inputMap, ActionMap actionMap,
                             int keyCode, String name,
                             Runnable onPress, Runnable onRelease) {
        KeyStroke press = KeyStroke.getKeyStroke(keyCode, 0, false);
        KeyStroke release = KeyStroke.getKeyStroke(keyCode, 0, true);
        // names get glued so actions stay uniqe
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

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }
        // flip latch so loop chills out
        gameOver = true;
        firing = false;
    }

    private void handleRestartPressed() {
        if (gameOver) {
            restartGame();
        }
    }

    private void restartGame() {
        // wipe stuff to fresh run quick
        bullets.clear();
        enemies.clear();
        player.reset(map);
        spawnInitialEnemies();
        lastShotNs = 0;
        up = down = left = right = firing = false;
        aimUp = aimDown = aimLeft = aimRight = false;
        gameOver = false;
        requestFocusInWindow();
        // focus grab else keys stop working and sadness follows
    }
}
