// File: SudokuCreator.java
package logic;

import java.util.Random;

public class SudokuCreator {
    private static final int SIZE = 9;
    private int[][] board = new int[SIZE][SIZE];
    private Random rand = new Random();

    public int[][] generateFullBoard() {
        board = new int[SIZE][SIZE];
        int r1 = rand.nextInt(SIZE), c1 = rand.nextInt(SIZE), val1 = rand.nextInt(9) + 1;
        int r2 = rand.nextInt(SIZE), c2 = rand.nextInt(SIZE), val2 = rand.nextInt(9) + 1;
        while (r1 == r2 && c1 == c2) r2 = rand.nextInt(SIZE);
        board[r1][c1] = val1;
        board[r2][c2] = val2;
        solveBoard(0, 0);
        return board;
    }

    private boolean solveBoard(int row, int col) {
        if (row == SIZE) return true;
        if (board[row][col] != 0) return solveBoard(col == SIZE - 1 ? row + 1 : row, (col + 1) % SIZE);
        for (int num = 1; num <= SIZE; num++) {
            if (isValid(row, col, num)) {
                board[row][col] = num;
                if (solveBoard(col == SIZE - 1 ? row + 1 : row, (col + 1) % SIZE)) return true;
                board[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isValid(int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == num || board[i][col] == num) return false;
        }
        int boxRow = row / 3 * 3, boxCol = col / 3 * 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (board[boxRow + i][boxCol + j] == num) return false;
        return true;
    }
}
