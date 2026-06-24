package game.entity;

import game.map.TileMap;

import java.awt.Rectangle;

public class Enemy {
    public static final int SIZE = 60;
    private static final float SPEED = 80f;
    private final Rectangle bounds = new Rectangle();
    private float x;
    private float y;
    private final int width;
    private final int height;
    private final TileMap tileMap;
    // lazy vector so we chase just a bit per update
    private float moveX;
    private float moveY;

    public Enemy(TileMap tileMap, int col, int row) {
        this.tileMap = tileMap;
        this.width = SIZE;
        this.height = SIZE;
        // stick enemy on given tile coords
        setToTile(tileMap, col, row);
    }

    private void setToTile(TileMap tileMap, int col, int row) {
        java.awt.Point world = tileMap.tileToWorld(col, row);
        // center 60px sprite inside 64 tile
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

        float stepX = moveX * SPEED * deltaSeconds;
        float stepY = moveY * SPEED * deltaSeconds;

        float nextX = x + stepX;
        float nextY = y + stepY;

        if (tileMap.isAreaWalkable(nextX, y, width, height)) {
            x = nextX;
        }
        if (tileMap.isAreaWalkable(x, nextY, width, height)) {
            y = nextY;
        }
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
