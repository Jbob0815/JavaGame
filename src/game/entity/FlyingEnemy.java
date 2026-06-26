package game.entity;

import game.map.TileMap;

import java.awt.Color;
import java.awt.Graphics2D;

public class FlyingEnemy extends Enemy {
    public static final int SIZE = 128;
    private static final float SPEED = 28f;
    private static final float SHOT_INTERVAL = 2.0f;
    private static final int MAX_HEALTH = 5;

    private float shotTimer;
    private int health = MAX_HEALTH;

    public FlyingEnemy(TileMap tileMap, int col, int row) {
        super(tileMap, col, row, SIZE, SIZE, SPEED);
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
        shotTimer += deltaSeconds;
        if (shotTimer < SHOT_INTERVAL) {
            return null;
        }
        shotTimer -= SHOT_INTERVAL;
        float dx = player.getCenterX() - getCenterX();
        float dy = player.getCenterY() - getCenterY();
        return new Bullet(getCenterX(), getCenterY(), dx, dy, Bullet.Owner.ENEMY);
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
        g2.setColor(new Color(180, 0, 220, 200));
        g2.fillRect(drawX, drawY, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawRect(drawX, drawY, getWidth(), getHeight());
        g2.setColor(Color.WHITE);
        g2.drawString("hp:" + health, drawX + 8, drawY + 20);
    }
}
