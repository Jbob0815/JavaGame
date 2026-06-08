package game.map;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TileMap {
    public static final int TILE_SIZE = 64;
    public static final int WALKABLE = 0;
    public static final int WALL = 1;
    public static final int HOLE = 2;

    private final int[][] tiles;
    private final List<Point> walkableTiles;

    public TileMap(int[][] layout) {
        if (layout == null || layout.length == 0 || layout[0].length == 0) {
            throw new IllegalArgumentException("Tile layout must not be empty");
        }
        tiles = layout;
        // cache points that are walk friendly
        walkableTiles = precomputeWalkableTiles(layout);
    }

    private static List<Point> precomputeWalkableTiles(int[][] layout) {
        List<Point> result = new ArrayList<>();
        for (int row = 0; row < layout.length; row++) {
            for (int col = 0; col < layout[row].length; col++) {
                if (layout[row][col] == WALKABLE) {
                    result.add(new Point(col, row));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public int getCols() {
        return tiles[0].length;
    }

    public int getRows() {
        return tiles.length;
    }

    public int getPixelWidth() {
        return getCols() * TILE_SIZE;
    }

    public int getPixelHeight() {
        return getRows() * TILE_SIZE;
    }

    public Dimension getPixelSize() {
        return new Dimension(getPixelWidth(), getPixelHeight());
    }

    public boolean isInside(int col, int row) {
        return row >= 0 && row < tiles.length && col >= 0 && col < tiles[row].length;
    }

    public int getTile(int col, int row) {
        if (!isInside(col, row)) {
            return WALL;
        }
        return tiles[row][col];
    }

    public boolean isWalkable(int col, int row) {
        return getTile(col, row) == WALKABLE;
    }

    public boolean isHole(int col, int row) {
        return getTile(col, row) == HOLE;
    }

    public Rectangle tileBounds(int col, int row) {
        return new Rectangle(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    public Point tileToWorld(int col, int row) {
        return new Point(col * TILE_SIZE, row * TILE_SIZE);
    }

    public int worldToCol(float worldX) {
        return (int) Math.floor(worldX / TILE_SIZE);
    }

    public int worldToRow(float worldY) {
        return (int) Math.floor(worldY / TILE_SIZE);
    }

    public boolean isAreaWalkable(float x, float y, int width, int height) {
        int leftCol = worldToCol(x);
        int rightCol = worldToCol(x + width - 1);
        int topRow = worldToRow(y);
        int bottomRow = worldToRow(y + height - 1);

        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (!isWalkable(col, row)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Point getRandomWalkableTile(Random random) {
        if (walkableTiles.isEmpty()) {
            return null;
        }
        // pick a tile sloppy random
        return walkableTiles.get(random.nextInt(walkableTiles.size()));
    }

    public List<Point> getWalkableTiles() {
        return walkableTiles;
    }

    public int[][] getTiles() {
        return tiles;
    }

    public static TileMap basicDemo() {
        int[][] layout = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1},
                {1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1},
                {1, 0, 1, 0, 0, 2, 0, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1},
                {1, 0, 2, 0, 0, 0, 0, 1, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        return new TileMap(layout);
    }
}
