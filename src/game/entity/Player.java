package game.entity;

import game.map.TileMap;

import java.awt.Point;
import java.awt.Rectangle;

public class Player {
    private static final int SPEED = 280;

    private final Rectangle bounds = new Rectangle();
    private float x;
    private float y;
    private final int width;
    private final int height;
    private float lastMoveX = 0f;
    private float lastMoveY = -1f;

    public Player(int startCol, int startRow, TileMap tileMap) {
        this.width = TileMap.TILE_SIZE;
        this.height = TileMap.TILE_SIZE;
        // start guy on chosen tile pos
        setToSpawnTile(startCol, startRow, tileMap);
    }

    private void setToSpawnTile(int startCol, int startRow, TileMap tileMap) {
        // yeah just try original tile first
        int chosenCol = startCol;
        int chosenRow = startRow;
        if (!tileMap.isWalkable(chosenCol, chosenRow)) {
            // grab fallback when we accidently spawn in wall
            Point fallback = tileMap.findClosestWalkable(startCol, startRow);
            if (fallback != null) {
                chosenCol = fallback.x;
                chosenRow = fallback.y;
            }
        }
        setPosition(tileMap.tileToWorld(chosenCol, chosenRow));
    }

    public void update(float deltaSeconds, boolean up, boolean down, boolean left, boolean right, TileMap tileMap) {
        float dx = 0;
        float dy = 0;
        if (up) {
            dy -= 1;
        }
        if (down) {
            dy += 1;
        }
        if (left) {
            dx -= 1;
        }
        if (right) {
            dx += 1;
        }
        if (dx != 0 || dy != 0) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
            lastMoveX = dx;
            lastMoveY = dy;
        }

        float newX = x + dx * SPEED * deltaSeconds;
        float newY = y + dy * SPEED * deltaSeconds;

        if (tileMap.isAreaWalkable(newX, y, width, height)) {
            x = newX;
        }
        if (tileMap.isAreaWalkable(x, newY, width, height)) {
            y = newY;
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

    public void setPosition(java.awt.Point position) {
        this.x = position.x;
        this.y = position.y;
    }

    public float getAimX() {
        return lastMoveX;
    }

    public float getAimY() {
        return lastMoveY;
    }

    public float getCenterX() {
        // helper so bullets spawn ok-ish
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }
}
