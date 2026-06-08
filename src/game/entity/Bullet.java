package game.entity;

import java.awt.Rectangle;

public class Bullet {
    private static final int SPEED = 480;
    private static final int WIDTH = 8;
    private static final int HEIGHT = 16;

    private final Rectangle bounds = new Rectangle();
    private float x;
    private float y;
    private boolean active = true;

    public Bullet(float startX, float startY) {
        this.x = startX - WIDTH / 2.0f;
        this.y = startY;
        // drops out of player top
    }

    public void update(float deltaSeconds) {
        y -= SPEED * deltaSeconds;
        if (y + HEIGHT < 0) {
            // bullet gone off screen
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
}
