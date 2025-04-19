package main;

import logic.SudokuCreator;
import logic.SudokuRemover;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SudokuGame extends JFrame {
    private String playerName, difficultyLevel;
    private javax.swing.Timer gameTimer;
    private JLabel timerLabel, mistakeLabel, hintLabel;
    private int seconds = 0, mistakes = 0, hintsUsed = 0;
    private final int MAX_HINTS = 3, MAX_MISTAKES = 4;
    private JButton[][] cells = new JButton[9][9];
    private int[][] fullBoard, puzzleBoard, initialPuzzleBoard;
    private Stack<int[]> moveStack = new Stack<>();
    private int selectedRow = -1, selectedCol = -1;
    private List<String> scoreHistory = new ArrayList<>();

    public SudokuGame(String playerName, String difficultyLevel) {
        this.playerName = playerName;
        this.difficultyLevel = difficultyLevel;
        setTitle("Sudoku - " + playerName + " [" + difficultyLevel + "]");
        setSize(720, 820);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setupUI();
        startTimer();
        generatePuzzle();
        setVisible(true);
    }

    private void setupUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("Player: " + playerName + " | Level: " + difficultyLevel);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(infoLabel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new GridLayout(1, 3));
        timerLabel = new JLabel("Time: 0s");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        mistakeLabel = new JLabel("Mistakes: 0");
        mistakeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        hintLabel = new JLabel("Hints Left: " + (MAX_HINTS - hintsUsed));
        hintLabel.setFont(new Font("Arial", Font.BOLD, 16));

        statusPanel.add(timerLabel);
        statusPanel.add(mistakeLabel);
        statusPanel.add(hintLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        JButton newGameBtn = new JButton("New Game");
        JButton restartBtn = new JButton("Restart");
        JButton undoBtn = new JButton("Undo");
        JButton hintBtn = new JButton("Hint");
        JButton quitBtn = new JButton("Quit");

        newGameBtn.addActionListener(e -> {
            stopTimer();
            seconds = 0;
            mistakes = 0;
            hintsUsed = 0;
            updateStatusLabels();
            String[] levels = {"Easy", "Medium", "Hard"};
            String newLevel = (String) JOptionPane.showInputDialog(null, "Select difficulty:", "Level",
                    JOptionPane.QUESTION_MESSAGE, null, levels, difficultyLevel);
            if (newLevel != null) {
                difficultyLevel = newLevel;
            }
            generatePuzzle();
            startTimer();
            setTitle("Sudoku - " + playerName + " [" + difficultyLevel + "]");
            infoLabel.setText("Player: " + playerName + " | Level: " + difficultyLevel);
        });

        restartBtn.addActionListener(e -> {
            stopTimer();
            seconds = 0;
            mistakes = 0;
            hintsUsed = 0;
            updateStatusLabels();
            restoreInitialPuzzle();
            startTimer();
        });

        quitBtn.addActionListener(e -> System.exit(0));
        undoBtn.addActionListener(e -> undoMove());
        hintBtn.addActionListener(e -> useHint());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(newGameBtn);
        buttonPanel.add(undoBtn);
        buttonPanel.add(hintBtn);
        buttonPanel.add(restartBtn);
        buttonPanel.add(quitBtn);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                for (int i = 0; i <= 9; i++) {
                    if (i % 3 == 0) g.setColor(Color.BLACK);
                    else g.setColor(Color.LIGHT_GRAY);
                    g.drawLine(0, i * getHeight() / 9, getWidth(), i * getHeight() / 9);
                    g.drawLine(i * getWidth() / 9, 0, i * getWidth() / 9, getHeight());
                }
            }
        };
        boardPanel.setLayout(new GridLayout(9, 9));
        Font cellFont = new Font("Monospaced", Font.BOLD, 20);

        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            JButton cell = new JButton();
            cell.setFont(cellFont);
            cell.setMargin(new Insets(0, 0, 0, 0));
            final int r = i, c = j;
            cell.setFocusPainted(false);
            cell.setFocusable(true);
            cell.addActionListener(e -> {
                clearSelection();
                selectedRow = r;
                selectedCol = c;
                highlightSameNumber(puzzleBoard[r][c]);
                cell.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                cell.requestFocusInWindow();
            });
            cell.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (selectedRow == r && selectedCol == c && puzzleBoard[r][c] == 0) {
                        int code = e.getKeyCode();
                        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                            int val = code - KeyEvent.VK_0;
                            moveStack.push(new int[]{r, c, puzzleBoard[r][c]});
                            if (val == fullBoard[r][c]) {
                                puzzleBoard[r][c] = val;
                                cell.setText(String.valueOf(val));
                                cell.setForeground(Color.BLUE);
                            } else {
                                puzzleBoard[r][c] = val;
                                cell.setText(String.valueOf(val));
                                cell.setForeground(Color.RED);
                                mistakes++;
                                updateStatusLabels();
                                if (mistakes >= MAX_MISTAKES) {
                                    String[] levels = {"Easy", "Medium", "Hard"};
                                    String newLevel = (String) JOptionPane.showInputDialog(null, "Select new level:", "Level", JOptionPane.QUESTION_MESSAGE, null, levels, difficultyLevel);
                                    if (newLevel == null) newLevel = difficultyLevel;
                                    difficultyLevel = newLevel;
                                    generatePuzzle();
                                    return;
                                } else {
                                    JOptionPane.showMessageDialog(null, "Wrong input!");
                                }
                            }
                            checkWin();
                        } else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
                            moveStack.push(new int[]{r, c, puzzleBoard[r][c]});
                            cell.setText("");
                            puzzleBoard[r][c] = 0;
                        }
                    }
                }
            });
            cells[i][j] = cell;
            boardPanel.add(cell);
        }

        add(boardPanel, BorderLayout.CENTER);
    }

    private void highlightSameNumber(int num) {
        if (num == 0) return;
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            if (puzzleBoard[i][j] == num) cells[i][j].setBackground(Color.YELLOW);
            else cells[i][j].setBackground(null);
        }
    }

    private void restoreInitialPuzzle() {
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            puzzleBoard[i][j] = initialPuzzleBoard[i][j];
            cells[i][j].setText(initialPuzzleBoard[i][j] == 0 ? "" : String.valueOf(initialPuzzleBoard[i][j]));
            cells[i][j].setForeground(Color.BLACK);
            cells[i][j].setBackground(null);
        }
    }

    private void generatePuzzle() {
        // fullBoard = SudokuCreator.generateFullBoard();
        // puzzleBoard = SudokuRemover.generatePuzzle(fullBoard, difficultyLevel);
        SudokuCreator creator = new SudokuCreator();
        SudokuRemover remover = new SudokuRemover();
        fullBoard = creator.generateFullBoard();
        puzzleBoard = remover.removeElements(fullBoard, difficultyLevel);
        initialPuzzleBoard = new int[9][9];
        for (int i = 0; i < 9; i++) System.arraycopy(puzzleBoard[i], 0, initialPuzzleBoard[i], 0, 9);
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            cells[i][j].setText(puzzleBoard[i][j] == 0 ? "" : String.valueOf(puzzleBoard[i][j]));
            cells[i][j].setForeground(Color.BLACK);
            cells[i][j].setBackground(null);
        }
        moveStack.clear();
    }

    private void startTimer() {
        gameTimer = new javax.swing.Timer(1000, e -> {
            seconds++;
            timerLabel.setText("Time: " + seconds + "s");
        });
        gameTimer.start();
    }

    private void stopTimer() {
        if (gameTimer != null) gameTimer.stop();
    }

    private void updateStatusLabels() {
        mistakeLabel.setText("Mistakes: " + mistakes);
        hintLabel.setText("Hints Left: " + (MAX_HINTS - hintsUsed));
    }

    private void clearSelection() {
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            cells[i][j].setBorder(UIManager.getBorder("Button.border"));
        }
    }

    private void undoMove() {
        if (!moveStack.isEmpty()) {
            int[] move = moveStack.pop();
            puzzleBoard[move[0]][move[1]] = move[2];
            cells[move[0]][move[1]].setText(move[2] == 0 ? "" : String.valueOf(move[2]));
            cells[move[0]][move[1]].setForeground(Color.BLACK);
            cells[move[0]][move[1]].setBackground(null);
        }
    }

    private void useHint() {
        if (hintsUsed >= MAX_HINTS) {
            JOptionPane.showMessageDialog(this, "No hints left!");
            return;
        }
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            if (puzzleBoard[i][j] == 0) {
                puzzleBoard[i][j] = fullBoard[i][j];
                cells[i][j].setText(String.valueOf(fullBoard[i][j]));
                cells[i][j].setForeground(Color.BLUE);
                hintsUsed++;
                updateStatusLabels();
                return;
            }
        }
    }

    private void checkWin() {
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            if (puzzleBoard[i][j] != fullBoard[i][j]) return;
        }
        stopTimer();
        JOptionPane.showMessageDialog(this, "Congratulations " + playerName + "! You solved the puzzle in " + seconds + " seconds with " + mistakes + " mistakes.");
        scoreHistory.add(playerName + " - Level: " + difficultyLevel + " - Time: " + seconds + "s - Mistakes: " + mistakes);
    }

    // private void highlightSameNumber(int value) {
    //     for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
    //         if (puzzleBoard[i][j] == value) {
    //             cells[i][j].setBackground(new Color(173, 216, 230)); // Light blue
    //         } else {
    //             cells[i][j].setBackground(Color.WHITE);
    //         }
    //     }
    // }

    // private void useHint() {
    //     if (hintsUsed >= MAX_HINTS) return;
    //     for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
    //         if (puzzleBoard[i][j] == 0) {
    //             puzzleBoard[i][j] = fullBoard[i][j];
    //             cells[i][j].setText(String.valueOf(fullBoard[i][j]));
    //             cells[i][j].setForeground(Color.MAGENTA);
    //             hintsUsed++;
    //             updateStatusLabels();
    //             return;
    //         }
    //     }
    // }

    // private void undoMove() {
    //     if (!moveStack.isEmpty()) {
    //         int[] lastMove = moveStack.pop();
    //         puzzleBoard[lastMove[0]][lastMove[1]] = 0;
    //         cells[lastMove[0]][lastMove[1]].setText("");
    //         updateStatusLabels();
    //     }
    // }

   

    // private void updateStatusLabels() {
    //     mistakeLabel.setText("Mistakes: " + mistakes);
    //     hintLabel.setText("Hints Left: " + (MAX_HINTS - hintsUsed));
    //     if (mistakes >= 4) {
    //         stopTimer();
    //         String[] levels = {"Easy", "Medium", "Hard"};
    //         String newLevel = (String) JOptionPane.showInputDialog(this, "Too many mistakes! Select new difficulty:", "New Game", JOptionPane.QUESTION_MESSAGE, null, levels, difficultyLevel);
    //         if (newLevel != null) difficultyLevel = newLevel;
    //         generatePuzzle();
    //         resetGameState();
    //         startTimer();
    //     }
    // }

    // private void startTimer() {
    //     gameTimer = new javax.swing.Timer(1000, e -> {
    //         seconds++;
    //         timerLabel.setText("Time: " + seconds + "s");
    //     });
    //     gameTimer.start();
    // }

    // private void stopTimer() {
    //     if (gameTimer != null) gameTimer.stop();
    // }

    // private void resetGameState() {
    //     seconds = 0;
    //     mistakes = 0;
    //     hintsUsed = 0;
    //     updateStatusLabels();
    // }

    // private void generatePuzzle() {
    //     fullBoard = SudokuCreator.generateFullBoard();
    //     puzzleBoard = SudokuRemover.removeCells(deepCopy(fullBoard), difficultyLevel);

    //     for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
    //         JButton cell = cells[i][j];
    //         int value = puzzleBoard[i][j];
    //         cell.setText(value == 0 ? "" : String.valueOf(value));
    //         cell.setEnabled(value == 0);
    //         cell.setForeground(Color.BLACK);
    //         cell.setBackground(Color.WHITE);
    //         cell.setBorder(BorderFactory.createMatteBorder(
    //                 i % 3 == 0 ? 3 : 1,
    //                 j % 3 == 0 ? 3 : 1,
    //                 (i + 1) % 3 == 0 ? 3 : 1,
    //                 (j + 1) % 3 == 0 ? 3 : 1,
    //                 Color.BLACK
    //         ));
    //     }
    //     moveStack.clear();
    // }

    // private int[][] deepCopy(int[][] board) {
    //     int[][] copy = new int[9][9];
    //     for (int i = 0; i < 9; i++) System.arraycopy(board[i], 0, copy[i], 0, 9);
    //     return copy;
    //}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String player = JOptionPane.showInputDialog(null, "Enter player name:");
            if (player == null || player.trim().isEmpty()) player = "Player";
            String[] levels = {"Easy", "Medium", "Hard"};
            String level = (String) JOptionPane.showInputDialog(null, "Select difficulty:", "Level",
                    JOptionPane.QUESTION_MESSAGE, null, levels, levels[0]);
            if (level == null) level = "Easy";
            new SudokuGame(player, level);
        });
    }
}
