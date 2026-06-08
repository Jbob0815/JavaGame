package game.entity;

import game.map.TileMap;

import java.awt.Rectangle;

public class Enemy {
    private final Rectangle bounds = new Rectangle();
    private float x;
    private float y;
    private final int width;
    private final int height;
    private final int col;
    private final int row;

    public Enemy(TileMap tileMap, int col, int row) {
        this.width = TileMap.TILE_SIZE;
        this.height = TileMap.TILE_SIZE;
        this.col = col;
        this.row = row;
        setToTile(tileMap, col, row);
    }

    private void setToTile(TileMap tileMap, int col, int row) {
        java.awt.Point world = tileMap.tileToWorld(col, row);
        this.x = world.x;
        this.y = world.y;
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
        return col;
    }

    public int getRow() {
        return row;
    }
}
