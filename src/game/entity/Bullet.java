package game.entity;

import java.awt.Rectangle;

public class Bullet {
    public enum Owner {
        PLAYER,
        ENEMY
    }
    private static final int SPEED = 480;
    private static final int WIDTH = 14;
    private static final int HEIGHT = 14;
    private static final float LIFE_SPAN = 1.4f;

    private final Rectangle bounds = new Rectangle();
    private float x;
    private float y;
    private float dirX;
    private float dirY;
    private float lifeTimer;
    private boolean active = true;
    private final Owner owner;

    public Bullet(float startX, float startY, float dirX, float dirY) {
        this(startX, startY, dirX, dirY, Owner.PLAYER);
    }

    public Bullet(float startX, float startY, float dirX, float dirY, Owner owner) {
        this.owner = owner;
        this.x = startX - WIDTH / 2.0f;
        this.y = startY - HEIGHT / 2.0f;
        setDirection(dirX, dirY);
        // bullet pops out from centre-ish
    }

    private void setDirection(float dirX, float dirY) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0f) {
            this.dirX = 0f;
            this.dirY = -1f;
        } else {
            this.dirX = dirX / len;
            this.dirY = dirY / len;
        }
    }

    public void update(float deltaSeconds) {
        if (!active) {
            return;
        }
        x += dirX * SPEED * deltaSeconds;
        y += dirY * SPEED * deltaSeconds;

        lifeTimer += deltaSeconds;
        if (lifeTimer >= LIFE_SPAN) {
            // timeout trash it
            active = false;
        }
    }

    public Rectangle getBounds() {
        bounds.setBounds(Math.round(x), Math.round(y), WIDTH, HEIGHT);
        return bounds;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public float getDirX() {
        return dirX;
    }

    public float getDirY() {
        return dirY;
    }

    public double getAngleRadians() {
        return Math.atan2(dirY, dirX);
    }

    public Owner getOwner() {
        return owner;
    }

    public boolean isPlayerOwned() {
        return owner == Owner.PLAYER;
    }

    public boolean isEnemyOwned() {
        return owner == Owner.ENEMY;
    }
}
