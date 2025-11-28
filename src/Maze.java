import javax.swing.*;
import java.awt.*;

/**
 * This is the starting point of the Swing application.
 * It defines the graphical user interface (GUI) for displaying the maze
 */
public class Maze {
    /**
     * Main method to launch the Swing application.
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Maze");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            MazePanel mazePanel = new MazePanel(10, 10);
            frame.add(mazePanel, BorderLayout.CENTER);

            JPanel buttonPanel = createButtonPanel(mazePanel);
            frame.add(buttonPanel, BorderLayout.SOUTH);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Creates the panel at the bottom containing control buttons for the maze.
     * @param mazePanel The parent MazePanel instance
     * @return JPanel containing the control buttons
     */
    private static JPanel createButtonPanel(MazePanel mazePanel) {
        JPanel buttonPanel = new JPanel();

        JButton resetButton    = new JButton("Reset");
        JButton solveBfsButton = new JButton("Solve (BFS)");
        JButton solveHeapButton= new JButton("Solve (Heap)");
        JButton runButton      = new JButton("Run");

        resetButton.addActionListener(_ -> mazePanel.reset());
        solveBfsButton.addActionListener(_ -> mazePanel.solveBfs());
        solveHeapButton.addActionListener(_ -> mazePanel.solveHeap());
        runButton.addActionListener(_ -> mazePanel.run());

        buttonPanel.add(resetButton);
        buttonPanel.add(solveBfsButton);
        buttonPanel.add(solveHeapButton);
        buttonPanel.add(runButton);

        return buttonPanel;
    }
}
