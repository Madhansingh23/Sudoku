
// File: SudokuRemover.java
package logic;

import java.util.Random;

public class SudokuRemover {
    public int[][] removeElements(int[][] board, String level) {
        int[][] puzzle = new int[9][9];
        int removeCount = switch (level.toLowerCase()) {
            case "easy" -> 30;
            case "medium" -> 45;
            case "hard" -> 60;
            default -> 40;
        };
        Random rand = new Random();
        for (int i = 0; i < 9; i++) System.arraycopy(board[i], 0, puzzle[i], 0, 9);
        while (removeCount > 0) {
            int r = rand.nextInt(9), c = rand.nextInt(9);
            if (puzzle[r][c] != 0) {
                puzzle[r][c] = 0;
                removeCount--;
            }
        }
        return puzzle;
    }
}
