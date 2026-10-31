package nl.henk.helpers;

import java.util.ArrayList;
import java.util.List;

/** Relative positions for a 5x5 oak hut, including floor and an open doorway. */
public final class Blueprint {
    public record Cell(int x, int y, int z) {}
    private Blueprint() {}
    public static List<Cell> hut() {
        var cells = new ArrayList<Cell>();
        for (int y = 0; y <= 4; y++)
            for (int x = 0; x < 5; x++)
                for (int z = 0; z < 5; z++) {
                    boolean shell = y == 0 || y == 4 || x == 0 || x == 4 || z == 0 || z == 4;
                    boolean door = x == 2 && z == 0 && (y == 1 || y == 2);
                    if (shell && !door) cells.add(new Cell(x, y, z));
                }
        return List.copyOf(cells);
    }
}
