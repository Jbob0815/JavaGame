package game.entity;

import game.map.TileMap;

import java.awt.Rectangle;

public class Enemy {
    public static final int SIZE = 60;
    private static final float DEFAULT_SPEED = 80f;
    protected final Rectangle bounds = new Rectangle();
    protected float x;
    protected float y;
    protected final int width;
    protected final int height;
    protected final TileMap tileMap;
    private final float baseSpeed;
    private float moveX;
    private float moveY;

    public Enemy(TileMap tileMap, int col, int row) {
        this(tileMap, col, row, SIZE, SIZE, DEFAULT_SPEED);
    }

    protected Enemy(TileMap tileMap, int col, int row, int width, int height, float speed) {
        this.tileMap = tileMap;
        this.width = width;
        this.height = height;
        this.baseSpeed = speed;
        setToTile(tileMap, col, row);
    }

    private void setToTile(TileMap tileMap, int col, int row) {
        java.awt.Point world = tileMap.tileToWorld(col, row);
        // center sprite in tile quick
        this.x = world.x + (TileMap.TILE_SIZE - width) / 2f;
        this.y = world.y + (TileMap.TILE_SIZE - height) / 2f;
    }

    public void update(float deltaSeconds, Player player) {
        if (player == null) {
            return;
        }
        float targetX = player.getCenterX();
        float targetY = player.getCenterY();
        float dx = targetX - getCenterX();
        float dy = targetY - getCenterY();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) {
            moveX = moveY = 0f;
            return;
        }
        moveX = dx / len;
        moveY = dy / len;

        float speed = getSpeed();
        float stepX = moveX * speed * deltaSeconds;
        float stepY = moveY * speed * deltaSeconds;

        float nextX = x + stepX;
        float nextY = y + stepY;

        if (canOccupy(nextX, y)) {
            x = nextX;
        }
        if (canOccupy(x, nextY)) {
            y = nextY;
        }
    }

    protected float getSpeed() {
        return baseSpeed;
    }

    protected boolean canOccupy(float nextX, float nextY) {
        return tileMap.isAreaWalkable(nextX, nextY, width, height);
    }

    public Bullet maybeShoot(float deltaSeconds, Player player) {
        return null;
    }

    public boolean onBulletHit() {
        return true;
    }

    public Rectangle getBounds() {
        bounds.setBounds(Math.round(x), Math.round(y), width, height);
        return bounds;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCol() {
        return tileMap.worldToCol(x + width / 2f);
    }

    public int getRow() {
        return tileMap.worldToRow(y + height / 2f);
    }

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }
}
