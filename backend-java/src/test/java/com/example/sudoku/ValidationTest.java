package com.example.sudoku;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidationTest {
    @Test
    public void testRowConflict() {
        int[][] grid = new int[9][9];
        grid[0][0] = 5;
        var res = App.checkConflict(grid, 0, 1, 5);
        assertEquals(true, (Boolean)res.get("conflict"));
        assertEquals("row", res.get("reason"));
    }
}
