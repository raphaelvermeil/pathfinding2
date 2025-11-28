import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.awt.Point;
import java.util.Set;

public class PathFinder {

    // 4-connected grid: up, down, left, right
    private static final int[][] DIRS = {
            {-1, 0}, // up
            { 1, 0}, // down
            { 0, -1}, // left
            { 0, 1}  // right
    };

    /**
     * OPTIONAL (NOT TESTED):
     * Implement Breadth-First Search (BFS) that finds a shortest path
     * from startCell to endCell.
     *
     * Hints:
     *  - Use a Queue<Point> (e.g. ArrayDeque) for BFS.
     *  - Maintain a boolean[][] visited array.
     *  - Maintain a Point[][] parent array to reconstruct the path
     *    once you reach endCell.
     */
    public static List<Point> findPathBfs(
            Point startCell,
            Point endCell,
            int rows, int cols,
            boolean[][] walls
    ) {
        List<Point> path = new ArrayList<>();

        // OPTIONAL: Implement BFS here.

        return path;
    }


    public static List<Point> findPathHeap(
            Point startCell,
            Point endCell,
            int rows, int cols,
            boolean[][] walls
    ) {
        List<Point> path = new ArrayList<>();
        Heap<SearchNode> heap = new Heap<>(rows * cols);
        SearchNode startNode = new SearchNode(startCell, 0, null);
        heap.add(startNode);
        Set <SearchNode> visited = new HashSet<>();
        while (heap.getSize() > 0) {
            SearchNode currentNode = heap.poll();
            if (currentNode.pos.equals(endCell)) {
                return reconstructPath(currentNode);
            }
            if (visited.contains(currentNode)) {
                continue;
            }
            visited.add(currentNode);
            for (int[] dir : DIRS) {
                int newRow = currentNode.pos.x + dir[0];
                int newCol = currentNode.pos.y + dir[1];
                if (canMoveTo(newRow, newCol, rows, cols, walls, new boolean[rows][cols])) {
                    Point newPos = new Point(newRow, newCol);
                    SearchNode neighborNode = new SearchNode(newPos, currentNode.dist + 1, currentNode);
                    heap.add(neighborNode);
                }
            }
        }


        return path;
    }

    private static boolean inBounds(int row, int col, int rows, int cols) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /**
     * Helper for BFS: check if we can move to (row, col) and it has not been visited.
     */
    private static boolean canMoveTo(
            int row, int col,
            int rows, int cols,
            boolean[][] walls,
            boolean[][] visited
    ) {
        if (!inBounds(row, col, rows, cols)) {
            return false;
        }
        if (walls[row][col]) {
            return false; // can't go through walls
        }
        return !visited[row][col];
    }

    /**
     * Helper for the greedy heap search:
     * Reconstruct a path from the goal SearchNode back to the start
     * by following parent pointers.
     */
    protected static List<Point> reconstructPath(SearchNode targetNode) {
        List<Point> path = new ArrayList<>();
        SearchNode current = targetNode;

        // build from end -> start, then reverse order by adding at index 0
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        return path;
    }
}

