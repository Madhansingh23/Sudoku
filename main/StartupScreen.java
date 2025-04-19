package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StartupScreen extends JFrame {
    private JTextField nameField;
    private JComboBox<String> levelBox;
    private JButton startButton;

    public StartupScreen() {
        setTitle("Sudoku - Start");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JLabel nameLabel = new JLabel("Enter Player Name:");
        nameField = new JTextField(15);

        JLabel levelLabel = new JLabel("Select Difficulty:");
        String[] levels = {"Easy", "Medium", "Hard"};
        levelBox = new JComboBox<>(levels);

        startButton = new JButton("Start Game");

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(levelLabel);
        inputPanel.add(levelBox);
        inputPanel.add(new JLabel());
        inputPanel.add(startButton);

        add(inputPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String level = (String) levelBox.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your name.", "Missing Info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dispose();
            new SudokuGame(name, level); // Send to SudokuGame.java
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StartupScreen::new);
    }
}
