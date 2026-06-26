package game.entity;

import game.map.TileMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class FlyingEnemy extends Enemy {
    public static final int SIZE = 128;
    private static final float SPEED = 28f;
    private static final float SHOT_INTERVAL = 2.0f;
    private static final int MAX_HEALTH = 5;
    private final BufferedImage[] frames;
    private float shotTimer;
    private float animTimer;
    private int animIndex;
    private int health = MAX_HEALTH;
    private boolean animatingShot;

    public FlyingEnemy(TileMap tileMap, int col, int row, BufferedImage[] frames) {
        super(tileMap, col, row, SIZE, SIZE, SPEED);
        this.frames = frames;
    }

    @Override
    protected boolean canOccupy(float nextX, float nextY) {
        float maxX = tileMap.getPixelWidth() - getWidth();
        float maxY = tileMap.getPixelHeight() - getHeight();
        return nextX >= 0 && nextY >= 0 && nextX <= maxX && nextY <= maxY;
    }

    @Override
    public Bullet maybeShoot(float deltaSeconds, Player player) {
        if (player == null) {
            return null;
        }
        animTimer += deltaSeconds;
        shotTimer += deltaSeconds;
        if (animatingShot) {
            advanceAnimation(deltaSeconds);
        }
        if (shotTimer < SHOT_INTERVAL) {
            return null;
        }
        shotTimer -= SHOT_INTERVAL;
        startShootAnimation();
        float dx = player.getCenterX() - getCenterX();
        float dy = player.getCenterY() - getCenterY();
        return new Bullet(getCenterX(), getCenterY(), dx, dy, Bullet.Owner.ENEMY);
    }

    private void startShootAnimation() {
        animatingShot = true;
        animIndex = 0;
        animTimer = 0f;
    }

    private void advanceAnimation(float deltaSeconds) {
        final float frameDuration = 0.08f;
        if (animTimer < frameDuration) {
            return;
        }
        animTimer -= frameDuration;
        animIndex++;
        if (animIndex >= 5) {
            animatingShot = false;
            animIndex = 0;
        }
    }

    private BufferedImage currentFrame() {
        if (!animatingShot) {
            return frames[0];
        }
        return switch (animIndex) {
            case 0 -> frames[0];
            case 1 -> frames[1];
            case 2 -> frames[2];
            case 3 -> frames[1];
            default -> frames[0];
        };
    }

    @Override
    public boolean onBulletHit() {
        if (health > 0) {
            health--;
        }
        return health <= 0;
    }

    public int getHealth() {
        return health;
    }

    public void render(Graphics2D g2) {
        int drawX = Math.round(getX());
        int drawY = Math.round(getY());
        BufferedImage sprite = currentFrame();
        if (sprite != null) {
            g2.drawImage(sprite, drawX, drawY, getWidth(), getHeight(), null);
        } else {
            g2.setColor(new Color(180, 0, 220, 200));
            g2.fillRect(drawX, drawY, getWidth(), getHeight());
        }
        g2.setColor(Color.WHITE);
        g2.drawString("hp:" + health, drawX + 8, drawY + 20);
    }
}
