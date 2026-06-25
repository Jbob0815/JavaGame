package game.entity;

import game.map.TileMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class StrongEnemy extends Enemy {
    public static final int SIZE = 32;
    public static final float SPEEDBUFF = 120f;
    private static final float SPEED = 40f;
    private boolean shielded = true;

    public StrongEnemy(TileMap tileMap, int col, int row) {
        super(tileMap, col, row, SIZE, SIZE, SPEED);
    }

    @Override
    protected boolean canOccupy(float nextX, float nextY) {
        // dont walk on holes same as walls
        if (!tileMap.isAreaWalkable(nextX, nextY, getWidth(), getHeight())) {
            return false;
        }
        Rectangle area = new Rectangle(Math.round(nextX), Math.round(nextY), getWidth(), getHeight());
        int leftCol = tileMap.worldToCol(area.x);
        int rightCol = tileMap.worldToCol(area.x + area.width - 1);
        int topRow = tileMap.worldToRow(area.y);
        int bottomRow = tileMap.worldToRow(area.y + area.height - 1);
        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (tileMap.isHole(col, row)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onBulletHit() {
        if (shielded) {
            shielded = false;
            return false;
        }
        return true;
    }

    @Override
    protected float getSpeed() {
        return shielded ? SPEED : SPEEDBUFF;
    }

    public boolean isShielded() {
        return shielded;
    }

    public Color getDebugColor() {
        return shielded ? Color.ORANGE : Color.RED;
    }


    //temp render method for testing
    public void render(Graphics2D g2, java.awt.image.BufferedImage shieldSprite, java.awt.image.BufferedImage bareSprite) {
        int drawX = Math.round(getX());
        int drawY = Math.round(getY());
        java.awt.image.BufferedImage sprite = shielded ? shieldSprite : bareSprite;
        if (sprite != null) {
            g2.drawImage(sprite, drawX, drawY, getWidth(), getHeight(), null);
        } else {
            g2.setColor(getDebugColor());
            g2.fillRect(drawX, drawY, getWidth(), getHeight());
        }
    }
}
