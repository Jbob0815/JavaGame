package game.entity;

import game.map.TileMap;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class Player {
    private static final int SPEED = 220;
    private static final int MAX_HEALTH = 3;
    private static final float INVINCIBILITY_DURATION = 1.0f;

    private final Rectangle bounds = new Rectangle();
    private float x;
    private float y;
    private final int width;
    private final int height;
    // keep spawn info so respawn isnt messy lol
    private final int spawnCol;
    private final int spawnRow;
    private float lastMoveX = 0f;
    private float lastMoveY = -1f;
    private BufferedImage currentSprite;
    private int health;
    private boolean invincible;
    private float invincibilityTimer;

    public enum Direction {
        DOWN,
        LEFT,
        RIGHT,
        UP
    }

    private final Map<Direction, BufferedImage[]> animationFrames = new EnumMap<>(Direction.class);
    private final Map<Direction, Float> animationTimers = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> animationIndices = new EnumMap<>(Direction.class);
    private Direction currentDirection = Direction.DOWN;

    public Player(int startCol, int startRow, TileMap tileMap) {
        //this.width = TileMap.TILE_SIZE;
        this.width = 32;
        this.height = 51;
        //this.height = TileMap.TILE_SIZE;
        this.spawnCol = startCol;
        this.spawnRow = startRow;
        // start guy on chosen tile pos
        setToSpawnTile(startCol, startRow, tileMap);
        this.health = MAX_HEALTH;
        this.invincible = false;
        this.invincibilityTimer = 0f;
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

    public void reset(TileMap tileMap) {
        // drop hero back to spawn tile quick
        setToSpawnTile(spawnCol, spawnRow, tileMap);
        lastMoveX = 0f;
        lastMoveY = -1f;
        restoreFullHealth();
    }

    public void update(float deltaSeconds, boolean up, boolean down, boolean left, boolean right, TileMap tileMap) {
        // figuring out tiny move vector here
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
            currentDirection = determineDirection();
        }

        float newX = x + dx * SPEED * deltaSeconds;
        float newY = y + dy * SPEED * deltaSeconds;

        if (tileMap.isAreaWalkable(newX, y, width, height)) {
            // ok slide on x
            x = newX;
        }
        if (tileMap.isAreaWalkable(x, newY, width, height)) {
            // ok slide on y
            y = newY;
        }

        updateInvincibility(deltaSeconds);
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

    public void setPosition(Point position) {
        // nudge to tile middle so sprite sits right
        this.x = position.x + (TileMap.TILE_SIZE - width) / 2f;
        this.y = position.y + (TileMap.TILE_SIZE - height) / 2f;
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

    public void updateAim(float aimX, float aimY) {
        if (aimX == 0f && aimY == 0f) {
            return;
        }
        lastMoveX = aimX;
        lastMoveY = aimY;
    }

    public boolean takeDamage(int amount) {
        if (amount <= 0 || invincible) {
            return false;
        }
        health = Math.max(0, health - amount);
        invincible = true;
        invincibilityTimer = INVINCIBILITY_DURATION;
        return true;
    }

    public void updateInvincibility(float deltaSeconds) {
        if (!invincible) {
            return;
        }
        invincibilityTimer -= deltaSeconds;
        if (invincibilityTimer <= 0f) {
            invincibilityTimer = 0f;
            invincible = false;
        }
    }

    public void restoreFullHealth() {
        health = MAX_HEALTH;
        invincible = false;
        invincibilityTimer = 0f;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public Direction determineDirection() {
        if (Math.abs(lastMoveX) > Math.abs(lastMoveY)) {
            return lastMoveX > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        if (Math.abs(lastMoveY) > 0.01f) {
            return lastMoveY > 0 ? Direction.DOWN : Direction.UP;
        }
        return currentDirection;
    }

    public void setAnimationFrames(Direction direction, BufferedImage[] frames) {
        animationFrames.put(direction, frames);
        animationTimers.put(direction, 0f);
        animationIndices.put(direction, 0);
    }

    public void advanceAnimation(Direction direction, float deltaSeconds, float frameDuration) {
        BufferedImage[] frames = animationFrames.get(direction);
        if (frames == null || frames.length == 0) {
            currentSprite = null;
            return;
        }
        float timer = animationTimers.getOrDefault(direction, 0f) + deltaSeconds;
        int index = animationIndices.getOrDefault(direction, 0);
        if (timer >= frameDuration) {
            timer -= frameDuration;
            index = (index + 1) % frames.length;
        }
        animationTimers.put(direction, timer);
        animationIndices.put(direction, index);
        currentSprite = frames[index];
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentSprite(BufferedImage sprite) {
        currentSprite = sprite;
    }

    public BufferedImage getCurrentSprite() {
        return currentSprite;
    }
}
