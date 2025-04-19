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
    private final int MAX_HINTS = 3, MAX_MISTAKES = 3;
    private JButton[][] cells = new JButton[9][9];
    private int[][] fullBoard, puzzleBoard;
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
                generatePuzzle();
                startTimer();
                setTitle("Sudoku - " + playerName + " [" + difficultyLevel + "]");
                infoLabel.setText("Player: " + playerName + " | Level: " + difficultyLevel);
            }
        });

        restartBtn.addActionListener(e -> {
            stopTimer();
            seconds = 0;
            mistakes = 0;
            hintsUsed = 0;
            updateStatusLabels();
            generatePuzzle();
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

        JPanel boardPanel = new JPanel(new GridLayout(9, 9));
        Font cellFont = new Font("Monospaced", Font.BOLD, 20);
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            JButton cell = new JButton();
            cell.setFont(cellFont);
            cell.setMargin(new Insets(0, 0, 0, 0));
            final int r = i, c = j;
            cell.setFocusPainted(false);
            cell.setFocusable(true);
            cell.addActionListener(e -> {
                if (puzzleBoard[r][c] == 0) {
                    clearSelection();
                    selectedRow = r;
                    selectedCol = c;
                    // cell.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                    cell.setBorder(UIManager.getBorder("Button.border"));

                    // cells[r][c].requestFocus();
                    cells[r][c].requestFocusInWindow(); // <- more reliable focus request

                }
            });
            cell.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (selectedRow == r && selectedCol == c && puzzleBoard[r][c] == 0) {
                        int code = e.getKeyCode();
                        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                            int val = code - KeyEvent.VK_0;
                            moveStack.push(new int[]{r, c});
                            if (val == fullBoard[r][c]) {
                                puzzleBoard[r][c] = val;  // Update puzzleBoard
                                cell.setText(String.valueOf(val));
                                cell.setForeground(Color.BLUE); // Correct entry, use blue text
                            } else {
                                puzzleBoard[r][c] = val;  // Even for wrong entries, you might want to update the board
                                cell.setText(String.valueOf(val));
                                cell.setForeground(Color.RED); // Incorrect entry, use red text
                                mistakes++;
                                updateStatusLabels();
                                if (mistakes >= MAX_MISTAKES) {
                                    stopTimer();
                                    JOptionPane.showMessageDialog(null, "Game Over: Too many mistakes!");
                                    return;
                                } else {
                                    JOptionPane.showMessageDialog(null, "Wrong input!");
                                }
                            }
                            checkWin();
                        } else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
                            cell.setText("");
                            puzzleBoard[r][c] = 0; // Reset puzzleBoard value when clearing the cell
                        }
                    }
                }
            });
            
            
            cells[i][j] = cell;
            boardPanel.add(cell);
        }
        add(boardPanel, BorderLayout.CENTER);
    }

    private void useHint() {
        if (selectedRow != -1 && selectedCol != -1 && puzzleBoard[selectedRow][selectedCol] == 0) {
            if (hintsUsed >= MAX_HINTS) {
                JOptionPane.showMessageDialog(this, "No more hints left!");
                return;
            }
            int val = fullBoard[selectedRow][selectedCol];
            cells[selectedRow][selectedCol].setText(String.valueOf(val));
            cells[selectedRow][selectedCol].setForeground(Color.MAGENTA);
            hintsUsed++;
            updateStatusLabels();
            checkWin();
        } else {
            JOptionPane.showMessageDialog(this, "Select an empty cell first!");
        }
    }

    private void updateStatusLabels() {
        timerLabel.setText("Time: " + seconds + "s");
        mistakeLabel.setText("Mistakes: " + mistakes);
        hintLabel.setText("Hints Left: " + (MAX_HINTS - hintsUsed));
    }

    private void clearSelection() {
        for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
            cells[i][j].setBorder(UIManager.getBorder("Button.border"));
        }
    }

    // private void generatePuzzle() {
    //     SudokuCreator creator = new SudokuCreator();
    //     fullBoard = creator.generateFullBoard();
    //     SudokuRemover remover = new SudokuRemover();
    //     puzzleBoard = remover.removeElements(fullBoard, difficultyLevel);
    //     updateBoardUI();
    // }

    private void generatePuzzle() {
        SudokuCreator creator = new SudokuCreator();
        fullBoard = creator.generateFullBoard();
        SudokuRemover remover = new SudokuRemover();
        puzzleBoard = remover.removeElements(fullBoard, difficultyLevel);
    
        // Adjust puzzle board difficulty here by controlling the number of removed cells
        if (difficultyLevel.equals("Hard")) {
            puzzleBoard = remover.removeElements(fullBoard, "Hard"); // More cells removed
        } else if (difficultyLevel.equals("Medium")) {
            puzzleBoard = remover.removeElements(fullBoard, "Medium");
        } else {
            puzzleBoard = remover.removeElements(fullBoard, "Easy");
        }
        updateBoardUI();
    }
    

    // private void updateBoardUI() {
    //     for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
    //         JButton cell = cells[i][j];
    //         if (puzzleBoard[i][j] != 0) {
    //             cell.setText(String.valueOf(puzzleBoard[i][j]));
    //             cell.setForeground(Color.BLACK);
    //             cell.setEnabled(false);
    //         } else {
    //             cell.setText("");
    //             cell.setEnabled(true);
    //             cell.setForeground(Color.BLUE);
    //             cell.setBorder(UIManager.getBorder("Button.border"));
    //         }
    //     }
    //     selectedRow = selectedCol = -1;
    //     moveStack.clear();
    // }
    
    private void updateBoardUI() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                JButton cell = cells[i][j];
                if (puzzleBoard[i][j] != 0) {
                    cell.setText(String.valueOf(puzzleBoard[i][j]));
                    cell.setForeground(Color.BLACK); // Full cells should have black text
                    cell.setFont(new Font("Monospaced", Font.BOLD, 20));  // Bold text
                    cell.setEnabled(false);  // Disable editing for these cells
                } else {
                    cell.setText("");
                    cell.setEnabled(true);
                    cell.setForeground(Color.BLACK); // Make sure empty cells have black text for visibility
                    cell.setFont(new Font("Monospaced", Font.PLAIN, 20)); // Normal text for editable cells
                    cell.setBorder(UIManager.getBorder("Button.border"));
                }
            }
        }
        selectedRow = selectedCol = -1;
        moveStack.clear();
    }
    
    


    // private void checkWin() {
    //     for (int i = 0; i < 9; i++) for (int j = 0; j < 9; j++) {
    //         if (puzzleBoard[i][j] == 0) {
    //             String txt = cells[i][j].getText();
    //             if (txt.isEmpty() || Integer.parseInt(txt) != fullBoard[i][j]) return;
    //         }
    //     }
    //     stopTimer();
    //     String score = playerName + " | " + difficultyLevel + " | Time: " + seconds + "s | Mistakes: " + mistakes;
    //     scoreHistory.add(score);
    //     JOptionPane.showMessageDialog(this, "🎉 You Won!\n" + score);
    // }

    // private void checkWin() {
    //     // Check if all cells are filled and correct
    //     boolean isWin = true;
    //     for (int i = 0; i < 9; i++) {
    //         for (int j = 0; j < 9; j++) {
    //             if (puzzleBoard[i][j] == 0) {
    //                 String txt = cells[i][j].getText();
    //                 // If any empty cell or incorrect value, the player hasn't won yet
    //                 if (txt.isEmpty() || Integer.parseInt(txt) != fullBoard[i][j]) {
    //                     isWin = false;
    //                     break;
    //                 }
    //             }
    //         }
    //         if (!isWin) break;
    //     }
    
    //     if (isWin) {
    //         stopTimer();
    //         JOptionPane.showMessageDialog(this, "Congratulations " + playerName + ", you have solved the puzzle!");
    //         updateScoreHistory();
    //     }
    // }
    
    // private void updateScoreHistory() {
    //     // Add the current score to the history (can store the time and mistakes)
    //     String scoreEntry = "Player: " + playerName + " | Time: " + seconds + "s | Mistakes: " + mistakes;
    //     scoreHistory.add(scoreEntry);
    //     // You can store this in a file or display it in a UI later
    // }
    private void checkWin() {
        boolean win = true;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzleBoard[i][j] == 0 || !cells[i][j].getText().equals(String.valueOf(fullBoard[i][j]))) {
                    win = false;
                    break;
                }
            }
        }
        if (win) {
            stopTimer();
            JOptionPane.showMessageDialog(this, "Congratulations, you win!");
        }
    }
    
    
    

    private void startTimer() {
        gameTimer = new javax.swing.Timer(1000, e -> {
            seconds++;
            updateStatusLabels();
        });
        gameTimer.start();
    }

    private void stopTimer() {
        if (gameTimer != null) gameTimer.stop();
    }

    // private void undoMove() {
    //     if (!moveStack.isEmpty()) {
    //         int[] last = moveStack.pop();
    //         cells[last[0]][last[1]].setText("");
    //     }
    // }

    private void undoMove() {
        if (!moveStack.isEmpty()) {
            int[] lastMove = moveStack.pop();
            int row = lastMove[0];
            int col = lastMove[1];
            puzzleBoard[row][col] = 0;  // Reset the puzzleBoard
            cells[row][col].setText("");  // Clear the cell
            mistakes--;  // Decrease mistakes count
            updateStatusLabels();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String playerName = JOptionPane.showInputDialog("Enter your name:");
            if (playerName == null || playerName.trim().isEmpty()) playerName = "Player";
    
            String[] levels = {"Easy", "Medium", "Hard"};
            String level = (String) JOptionPane.showInputDialog(null, "Select difficulty:", "Difficulty",
                    JOptionPane.QUESTION_MESSAGE, null, levels, "Easy");
    
            if (level != null) {
                new SudokuGame(playerName, level);
            } else {
                System.exit(0);
            }
        });
    }
    
}
