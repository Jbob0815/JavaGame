package game;

import game.entity.Bullet;
import game.entity.Enemy;
import game.entity.FlyingEnemy;
import game.entity.Player;
import game.entity.StrongEnemy;
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
    private static final int DAMAGE_PER_HIT = 1;
    private static final int SCORE_PER_KILL = 100;
    private static final float STRONG_ENEMY_CHANCE = 0.3f;
    private static final float FLYING_ENEMY_CHANCE = 0.05f;
    private final TileMap map = TileMap.basicDemo();
    private final Player player = new Player(2, 2, map);
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> enemyBullets = new ArrayList<>();
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
    private final BufferedImage strongShieldSprite;
    private final BufferedImage strongBareSprite;
    private int enemyFrameIndex;
    private float enemyFrameTimer;
    private int bulletFrameIndex;
    private float bulletFrameTimer;
    private final BufferedImage playerSansFallback;
    private static final float PLAYER_FRAME_SECONDS = 0.2f;
    private int score;

    public GamePanel() {
        setPreferredSize(map.getPixelSize());
        setBackground(Color.BLACK);
        setFocusable(true);
        setupKeyBindings();
        spawnInitialEnemies();
        floorImg = loadImageOrThrow("Boden.png");
        wallImg = loadImageOrThrow("wand.png");
        holeImg = loadImageOrThrow("loch.png");
        // try grab the sans gif, fallback later
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
        strongShieldSprite = tryLoadImage("strong-shield.png");
        strongBareSprite = tryLoadImage("strong-bare.png");
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
            Bullet enemyShot = enemy.maybeShoot(deltaSeconds, player);
            if (enemyShot != null) {
                enemyBullets.add(enemyShot);
            }
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
        Iterator<Bullet> enemyBulletIter = enemyBullets.iterator();
        while (enemyBulletIter.hasNext()) {
            Bullet bullet = enemyBulletIter.next();
            bullet.update(deltaSeconds);
            if (!bullet.isActive()) {
                enemyBulletIter.remove();
            }
        }

        if (!gameOver && isPlayerOverHole()) {
            // player got too close to hole so rip
            triggerGameOver();
            return;
        }

        handleCollisions();
        if (!gameOver) {
            maintainEnemyCount();
        }
        advanceAnimations(deltaSeconds);
    }

    private boolean isPlayerOverHole() {
        int col = map.worldToCol(player.getCenterX());
        int row = map.worldToRow(player.getCenterY());
        return map.isHole(col, row);
    }

    private void handleCollisions() {
        Rectangle playerRect = player.getBounds();

        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();

            if (!map.isAreaWalkable(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight())) {
                // bullet bonks wall so deactivate
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
                    // hit confirm -> apply enemy specific response
                    bullet.deactivate();
                    if (enemy.onBulletHit()) {
                        enemyIter.remove();
                        score += SCORE_PER_KILL;
                    }
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

        for (Bullet bullet : enemyBullets) {
            if (playerRect.intersects(bullet.getBounds())) {
                if (player.takeDamage(DAMAGE_PER_HIT) && player.isDead()) {
                    triggerGameOver();
                }
                bullet.deactivate();
            }
        }
        enemyBullets.removeIf(b -> !b.isActive());

        for (Enemy enemy : enemies) {
            if (playerRect.intersects(enemy.getBounds())) {
                // bonk player and its game over if hp hits 0
                if (player.takeDamage(DAMAGE_PER_HIT) && player.isDead()) {
                    triggerGameOver();
                }
                // stop checking to avoid chain hits same frame
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
            enemies.add(createRandomEnemy(tile.x, tile.y));
            return;
        }
    }

    private Enemy createRandomEnemy(int col, int row) {
        if (random.nextFloat() < STRONG_ENEMY_CHANCE) {
            return new StrongEnemy(map, col, row);
        }
        if (random.nextFloat() < FLYING_ENEMY_CHANCE) {
            return new FlyingEnemy(map, col, row);
        }
        return new Enemy(map, col, row);
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
            // super last fallback if art missing / corrupted
            g2.setColor(Color.CYAN);
            g2.fillRect(drawX, drawY, player.getWidth(), player.getHeight());
        }
    }

    private void renderBullets(Graphics2D g2) {
        BufferedImage baseSprite = bulletFrames[bulletFrameIndex];
        for (Bullet bullet : bullets) {
            drawBullet(g2, baseSprite, bullet);
        }
        for (Bullet bullet : enemyBullets) {
            drawBullet(g2, baseSprite, bullet);
        }
    }

    private void drawBullet(Graphics2D g2, BufferedImage baseSprite, Bullet bullet) {
        BufferedImage sprite = baseSprite;
        double angle = bullet.getAngleRadians();
        if (Math.abs(angle) > 0.001) {
            sprite = rotateImage(baseSprite, angle);
        }
        int x = Math.round(bullet.getX());
        int y = Math.round(bullet.getY());
        int drawW = bullet.getWidth();
        int drawH = bullet.getHeight();
        if (sprite != null) {
            g2.drawImage(sprite, x, y, drawW, drawH, null);
        } else {
            g2.setColor(Color.YELLOW);
            g2.fillOval(x, y, drawW, drawH);
        }
    }

    private void renderEnemies(Graphics2D g2) {
        BufferedImage enemySprite = enemyFrames[enemyFrameIndex];
        for (Enemy enemy : enemies) {
            if (enemy instanceof StrongEnemy strongEnemy) {
                strongEnemy.render(g2, strongShieldSprite, strongBareSprite);
            } else if (enemy instanceof FlyingEnemy flyingEnemy) {
                flyingEnemy.render(g2);
            } else {
                int x = Math.round(enemy.getX());
                int y = Math.round(enemy.getY());
                g2.drawImage(enemySprite, x, y, enemy.getWidth(), enemy.getHeight(), null);
            }
        }
    }

    private void renderHud(Graphics2D g2) {
        g2.setColor(Color.RED);
        g2.drawString("wasd move, arrows aim, space shoot, R restart", 12, 18);
        g2.drawString("Score: " + score, 12, 36);
        StringBuilder hearts = new StringBuilder("HP: ");
        for (int i = 0; i < player.getMaxHealth(); i++) {
            hearts.append(i < player.getHealth() ? "♥" : "♡");
        }
        g2.drawString(hearts.toString(), 12, 54);
        if (player.isInvincible() && (System.nanoTime() / 200_000_000L) % 2 == 0) {
            // tiny note so we know invuln is active
            g2.drawString("invincible", 12, 72);
        }
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
        // bullet flash faster
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

    private BufferedImage rotateImage(BufferedImage source, double angleRadians) {
        if (source == null) {
            return null;
        }
        double sin = Math.abs(Math.sin(angleRadians));
        double cos = Math.abs(Math.cos(angleRadians));
        int w = source.getWidth();
        int h = source.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.translate((newW - w) / 2.0, (newH - h) / 2.0);
        g2d.rotate(angleRadians, w / 2.0, h / 2.0);
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return rotated;
    }

    private void setupKeyBindings() {
        int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = getInputMap(condition);
        ActionMap actionMap = getActionMap();
        // got to wire keys or nothing moves
        registerKey(inputMap, actionMap, KeyEvent.VK_W, "moveUp", () -> up = true, () -> up = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_UP, "aimUpArrow", () -> aimUp = true, () -> aimUp = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_S, "moveDown", () -> down = true, () -> down = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_DOWN, "aimDownArrow", () -> aimDown = true, () -> aimDown = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_A, "moveLeft", () -> left = true, () -> left = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_LEFT, "aimLeftArrow", () -> aimLeft = true, () -> aimLeft = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_D, "moveRight", () -> right = true, () -> right = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_RIGHT, "aimRightArrow", () -> aimRight = true, () -> aimRight = false);
        // space bar hold shoots indefinetly
        registerKey(inputMap, actionMap, KeyEvent.VK_SPACE, "fire", () -> firing = true, () -> firing = false);
        registerKey(inputMap, actionMap, KeyEvent.VK_R, "restart", this::handleRestartPressed, () -> {});
        // r key to reset things super quick
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
        enemyBullets.clear();
        enemies.clear();
        player.reset(map);
        spawnInitialEnemies();
        lastShotNs = 0;
        up = down = left = right = firing = false;
        aimUp = aimDown = aimLeft = aimRight = false;
        gameOver = false;
        score = 0;
        requestFocusInWindow();
        // focus grab else keys stop working
    }
}
