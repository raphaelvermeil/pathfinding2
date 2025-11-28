import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * MazePanel class for displaying and interacting with the maze. It extends
 * JPanel as per Swing conventions.
 * This class handles maze generation, rendering, and user interactions.
 */
public class MazePanel extends JPanel {
    /**
     * Number of rows in the maze. We always use 10.
     */
    private final int rows;

    /**
     * Number of columns in the maze. We always use 10.
     */
    private final int cols;

    /**
     * Size of each cell in pixels. Make it bigger if you feel the window is too
     * small, or vice versa.
     */
    private final int CELL_SIZE = 40;

    /**
     * Sparsity constant between 0.0 to 1.0 controlling maze openness.
     * Higher values create more open mazes with fewer walls.
     */
    private final double SPARSITY = 0.7;

    /**
     * 2D array representing the maze where each cell is either a wall (true) or
     * passage (false).
     * The maze is rows x cols in size, with cells being either walls or passages.
     */
    private final boolean[][] walls;

    /**
     * Random number generator instance for maze generation.
     */
    private final Random random;

    /**
     * User-selected start cell for pathfinding. Can be null if not selected.
     */
    private Point startCell;

    /**
     * User-selected end cell for pathfinding. Can be null if not selected.
     */
    private Point endCell;

    /**
     * List of Points representing the computed path from start to end, if computed.
     */
    private List<Point> path;

    /**
     * Image for the start cell (robot).
     */
    private Image robotImage;

    /**
     * Image for the end cell (home).
     */
    private Image homeImage;

    /**
     * Current position of the robot during animation. Null when not animating.
     */
    private Point robotPosition;

    /**
     * Index of the current position in the path during animation.
     */
    private int pathIndex;

    /**
     * Timer for animation.
     */
    private Timer animationTimer;

    public MazePanel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        // Create grid with direct rows x cols size
        this.walls = new boolean[rows][cols];
        this.random = new Random();
        this.path = new ArrayList<>();

        // Load images
        // To make this work, ensure your resources folder is set up correctly
        robotImage = new ImageIcon("resources/robot.png").getImage();
        homeImage = new ImageIcon("resources/home.png").getImage();

        // Set preferred size for the panel (showing passage cells only)
        setPreferredSize(new Dimension(cols * CELL_SIZE, rows * CELL_SIZE));

        // Generate maze
        generateMaze();

        // Add mouse listener for cell selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleCellClick(e.getX(), e.getY());
            }
        });
    }

    /**
     * Handles mouse click events. The first click will set the start cell,
     * the second click will set the end cell,
     * and the third click will reset and start over.
     *
     * @param x The x coordinate of the mouse click
     * @param y The y coordinate of the mouse click
     */
    private void handleCellClick(int x, int y) {
        int col = x / CELL_SIZE;
        int row = y / CELL_SIZE;

        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }

        // Only allow clicking on passage cells (not walls)
        if (walls[row][col]) {
            return;
        }

        Point clickedCell = new Point(row, col);

        // First click sets start, second click sets end
        if (startCell == null) {
            startCell = clickedCell;
            endCell = null;
            path.clear();
        } else if (endCell == null) {
            endCell = clickedCell;
        } else {
            // Reset and start over
            startCell = clickedCell;
            endCell = null;
            path.clear();
        }

        repaint();
    }

    /**
     * Generates a new maze.
     */
    public void reset() {
        // Stop any running animation
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // Clear animation state
        robotPosition = null;
        pathIndex = 0;

        // Clear path and selections
        startCell = null;
        endCell = null;
        path.clear();

        // Generate a new maze
        generateMaze();

        repaint();
    }

    /**
     * Backwards-compatible "solve" – uses BFS implementation.
     * (Used if someone still calls PathFinder.findPath via this method.)
     */
    public void solve() {
        solveBfs();
    }

    /**
     * Solve the maze using BFS implementation in PathFinder.
     */
    public void solveBfs() {
        path = PathFinder.findPathBfs(startCell, endCell, rows, cols, walls);
        // reset animation state
        robotPosition = null;
        pathIndex = 0;
        repaint();
    }

    /**
     * Solve the maze using the greedy heap-based implementation in PathFinder.
     */
    public void solveHeap() {
        path = PathFinder.findPathHeap(startCell, endCell, rows, cols, walls);
        // reset animation state
        robotPosition = null;
        pathIndex = 0;
        repaint();
    }

    /**
     * Run the maze solving animation that animates the robot following the path.
     */
    public void run() {
        // Check if path exists and has at least 2 points (start and end)
        if (path == null || path.isEmpty() || path.size() < 2) {
            return;
        }

        // Stop any existing animation
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // Initialize animation state
        pathIndex = 0;
        robotPosition = path.getFirst(); // Java 21 List.getFirst()

        // Create animation timer that updates every 300 ms
        animationTimer = new Timer(300, _ -> {
            pathIndex++;

            if (pathIndex < path.size()) {
                robotPosition = path.get(pathIndex);
                repaint();

                if (pathIndex == path.size() - 1) {
                    robotPosition = null;
                    animationTimer.stop();
                    repaint();
                }
            }
        });

        // Start the animation
        animationTimer.start();
    }

    /**
     * Generates a maze using an adapted version of the Eller's algorithm.
     * The Eller's algorithm always generate mazes with no isolated sections,
     * meaning you can go from any passage cell to any other passage cell.
     * For more on maze generation algorithms, see COMP 521 - Modern Computer Games
     */
    private void generateMaze() {
        int passageRows = rows / 2;
        int passageCols = cols / 2;

        // Create arrays to track walls between passages
        boolean[][] horizontalWalls = new boolean[passageRows][passageCols - 1];
        boolean[][] verticalWalls = new boolean[passageRows - 1][passageCols];

        // Initialize all walls as present
        for (int i = 0; i < passageRows; i++) {
            Arrays.fill(horizontalWalls[i], true);
        }
        for (int i = 0; i < passageRows - 1; i++) {
            Arrays.fill(verticalWalls[i], true);
        }

        int[] sets = new int[passageCols];
        int nextSet = 1;

        // Initialize first row - each passage cell in its own set
        for (int col = 0; col < passageCols; col++) {
            sets[col] = nextSet++;
        }

        for (int row = 0; row < passageRows; row++) {
            // Randomly join adjacent cells (remove horizontal walls)
            for (int col = 0; col < passageCols - 1; col++) {
                if (sets[col] != sets[col + 1] && (row == passageRows - 1 || random.nextBoolean())) {
                    // Remove wall between cells
                    horizontalWalls[row][col] = false;
                    // Merge sets
                    int oldSet = sets[col + 1];
                    int newSet = sets[col];
                    for (int i = 0; i < passageCols; i++) {
                        if (sets[i] == oldSet) {
                            sets[i] = newSet;
                        }
                    }
                }
            }

            // Create vertical connections if not last row
            if (row < passageRows - 1) {
                Map<Integer, List<Integer>> setGroups = new HashMap<>();
                for (int col = 0; col < passageCols; col++) {
                    setGroups.computeIfAbsent(sets[col], k -> new ArrayList<>()).add(col);
                }

                int[] newSets = new int[passageCols];
                for (Map.Entry<Integer, List<Integer>> entry : setGroups.entrySet()) {
                    List<Integer> cellsInSet = entry.getValue();
                    int set = entry.getKey();

                    boolean hasConnection = false;
                    for (int col : cellsInSet) {
                        if (!hasConnection || random.nextBoolean()) {
                            verticalWalls[row][col] = false;
                            newSets[col] = set;
                            hasConnection = true;
                        } else {
                            newSets[col] = nextSet++;
                        }
                    }
                }
                sets = newSets;
            }
        }

        // The maze is generated, we now fill the walls array based on the generated
        // maze
        for (int i = 0; i < rows; i++) {
            Arrays.fill(walls[i], true);
        }

        // Mark passage cells and removed walls
        for (int pRow = 0; pRow < passageRows; pRow++) {
            for (int pCol = 0; pCol < passageCols; pCol++) {
                int row = 2 * pRow + 1;
                int col = 2 * pCol + 1;

                if (row < rows && col < cols) {
                    walls[row][col] = false; // Passage cell
                }

                if (pCol < passageCols - 1 && !horizontalWalls[pRow][pCol]) {
                    int wallCol = col + 1;
                    if (wallCol < cols) {
                        walls[row][wallCol] = false;
                    }
                }

                if (pRow < passageRows - 1 && !verticalWalls[pRow][pCol]) {
                    int wallRow = row + 1;
                    if (wallRow < rows) {
                        walls[wallRow][col] = false;
                    }
                }
            }
        }

        // Remove some walls to increase sparsity
        for (int col = 0; col < cols; col++) {
            if (col > 0 && col < cols - 1 && walls[0][col]) {
                if (1 < rows && !walls[1][col]) {
                    if (canRemoveWall(0, col) && random.nextDouble() < SPARSITY) {
                        walls[0][col] = false;
                    }
                }
            }
        }

        for (int row = 0; row < rows; row++) {
            if (row > 0 && row < rows - 1 && walls[row][0]) {
                if (1 < cols && !walls[row][1]) {
                    if (canRemoveWall(row, 0) && random.nextDouble() < SPARSITY) {
                        walls[row][0] = false;
                    }
                }
            }
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                if (walls[row][col]) {
                    if (canRemoveWall(row, col) && random.nextDouble() < SPARSITY * 0.5) {
                        walls[row][col] = false;
                    }
                }
            }
        }
    }

    /**
     * Checks if a wall can be safely removed without creating an empty space.
     * A wall can be removed only if at least one of its 4 adjacent cells is a
     * passage (not a wall or border).
     *
     * @param row The row of the wall cell
     * @param col The column of the wall cell
     * @return true if the wall can be removed, false if removing it would create an
     *         empty space
     */
    private boolean canRemoveWall(int row, int col) {
        // Check top (row - 1, col)
        boolean topIsWall = (row - 1 < 0) || walls[row - 1][col];

        // Check bottom (row + 1, col)
        boolean bottomIsWall = (row + 1 >= rows) || walls[row + 1][col];

        // Check left (row, col - 1)
        boolean leftIsWall = (col - 1 < 0) || walls[row][col - 1];

        // Check right (row, col + 1)
        boolean rightIsWall = (col + 1 >= cols) || walls[row][col + 1];

        // Can remove the wall if at least one adjacent cell is a passage
        return !(topIsWall && bottomIsWall && leftIsWall && rightIsWall);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * CELL_SIZE;
                int y = row * CELL_SIZE;

                if (walls[row][col]) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }

                g.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }

        if (!path.isEmpty()) {
            g.setColor(Color.BLUE);
            for (int i = 0; i < path.size(); i++) {
                Point p = path.get(i);

                // Always draw the path, except:
                // - The current robot position (robot image will be there)
                // - The start cell if not animating (robot image will be there)
                // - The end cell (home image will be there)
                boolean shouldDrawPath = !p.equals(endCell);

                if (robotPosition != null) {
                    if (p.equals(robotPosition)) {
                        shouldDrawPath = false;
                    }
                } else {
                    if (p.equals(startCell)) {
                        shouldDrawPath = false;
                    }
                }

                if (shouldDrawPath) {
                    int x = p.y * CELL_SIZE;
                    int y = p.x * CELL_SIZE;
                    g.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                }
            }
        }

        Point robotDrawPosition = (robotPosition != null) ? robotPosition : startCell;
        if (robotDrawPosition != null && robotImage != null) {
            int x = robotDrawPosition.y * CELL_SIZE;
            int y = robotDrawPosition.x * CELL_SIZE;
            g.drawImage(robotImage, x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, this);
        }

        if (endCell != null && homeImage != null) {
            int x = endCell.y * CELL_SIZE;
            int y = endCell.x * CELL_SIZE;
            g.drawImage(homeImage, x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, this);
        }
    }
}
