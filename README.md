# Pathfinder

A small Java Swing app that generates a random grid maze, lets you choose a start and a goal cell, finds a path between them and animates a robot walking it.

The project exists to practise two things: building a **binary min-heap** from scratch and using it as the priority queue in a **shortest-path search** on a grid.

```
# # # # # # # # # #      R  start (robot)
# R . . .   #     #      H  goal (home)
# # # # . # #   # #      .  computed path
#     # . . . . . #      #  wall
#   # # # # # # . #
#           # # H #
# # # # # # # # # #

[Reset] [Solve (BFS)] [Solve (Heap)] [Run]
```

## Contents

- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Using the app](#using-the-app)
- [Project structure](#project-structure)
- [How it works](#how-it-works)
  - [Maze generation](#maze-generation)
  - [The heap](#the-heap)
  - [Heap-based search](#heap-based-search)
  - [BFS](#bfs)
  - [Animation](#animation)
- [Configuration](#configuration)
- [Known issues and limitations](#known-issues-and-limitations)
- [Ideas for extending it](#ideas-for-extending-it)

## Requirements

- **JDK 22 or newer.** The code uses unnamed lambda parameters (`_ -> ...`), which became final in Java 22, and `List.getFirst()` from Java 21. It has been tested with OpenJDK 24.
- No external libraries. Everything uses the JDK (`java.util`, `java.awt`, `javax.swing`).

## Getting started

### From the command line

Run these from the **repository root**. The images are loaded with paths relative to the working directory (`resources/robot.png`, `resources/home.png`), so running from anywhere else gives you a maze with no robot or house icons.

```bash
# Compile
javac -d out src/*.java

# Launch the GUI
java -cp out Maze
```

`javac` prints a note about unchecked operations in `Heap.java`. That is expected: it comes from creating a generic array (`(T[]) new Comparable[...]`) and is harmless.

To try the heap on its own, run its demo `main`:

```bash
java -cp out Heap
# MinHeap: [null, 1, 3, 8, 5, null, null, null]
# Remove root: 1
# After poll: [null, 3, 5, 8, null, null, null, null]
```

Index 0 is always `null` because the heap is 1-indexed (see [The heap](#the-heap)).

### From IntelliJ IDEA

The repository includes an IntelliJ module (`Pathfinder2.iml`, `.idea/`), with `src/` marked as the source root.

1. Open the repository folder in IntelliJ.
2. Under **File → Project Structure → Project**, set the SDK to JDK 22+ and the language level to 22 or above.
3. Open `src/Maze.java` and run `main`. IntelliJ uses the project root as the working directory by default, so the images load correctly.

## Using the app

1. **Pick a start cell.** Click any white (passage) cell. The robot appears there. Clicks on black walls are ignored.
2. **Pick a goal cell.** Click a second passage cell. The house appears there.
3. **Solve.** Click **Solve (Heap)**. The path is drawn as blue squares between the robot and the house.
4. **Run.** Click **Run** to watch the robot move along the path, one cell every 300 ms.
5. **Start over.**
   - A third click on the grid sets a new start cell and clears the goal and path.
   - **Reset** stops any animation, clears the selections and generates a new maze.

| Button | What it does |
| --- | --- |
| **Reset** | Generates a new random maze and clears start, goal, path and animation. |
| **Solve (BFS)** | Calls `PathFinder.findPathBfs`. **Not implemented yet**: it returns an empty path, so nothing is drawn. |
| **Solve (Heap)** | Calls `PathFinder.findPathHeap`, a heap-backed shortest-path search. |
| **Run** | Animates the robot along the current path. Does nothing if there is no path. |

> Choose both a start and a goal before you click a Solve button. See [Known issues](#known-issues-and-limitations).

## Project structure

```
.
├── src/
│   ├── Maze.java         # Entry point: builds the JFrame and the button bar
│   ├── MazePanel.java    # Maze generation, rendering, mouse input, animation
│   ├── PathFinder.java   # Search algorithms (BFS stub + heap-based search)
│   ├── Heap.java         # Generic, resizable binary min-heap
│   └── SearchNode.java   # Search state: position, distance, parent pointer
├── resources/
│   ├── robot.png         # Start marker / animated robot
│   └── home.png          # Goal marker
├── Pathfinder2.iml       # IntelliJ module file
└── .idea/                # IntelliJ project settings
```

### Class overview

| Class | Role |
| --- | --- |
| `Maze` | `main` method. Creates a window with a 10×10 `MazePanel` in the centre and four buttons along the bottom, all on the Swing event thread. |
| `MazePanel` | A `JPanel` that holds the grid state (`boolean[][] walls`, `true` = wall), generates mazes, turns mouse clicks into start/goal selections, calls into `PathFinder`, paints the grid, path and icons, and runs the animation `Timer`. |
| `PathFinder` | Static search methods. Each takes `(startCell, endCell, rows, cols, walls)` and returns a `List<Point>` from start to goal, or an empty list. Moves are 4-directional (up, down, left, right). |
| `Heap<T extends Comparable<T>>` | Array-backed binary min-heap with `add`, `poll` and `getSize`. Grows by doubling. |
| `SearchNode` | A node in the search tree: `pos`, `dist` (steps from start) and `parent`. Ordered by `dist`. Two nodes are equal when they share a position. |

### Coordinate convention

Cells are `java.awt.Point` objects with **`x` = row** and **`y` = column**, the reverse of the usual screen meaning. When painting, `MazePanel` converts back with `x = p.y * CELL_SIZE` and `y = p.x * CELL_SIZE`. Keep this in mind when you add code that reads or creates `Point`s.

## How it works

### Maze generation

`MazePanel.generateMaze()` builds the maze in three stages.

1. **Eller's algorithm on a half-size grid.** The 10×10 grid is treated as a 5×5 grid of "passage" cells, each at an odd coordinate `(2r+1, 2c+1)`. Eller's algorithm works one row at a time. It tracks which set each cell belongs to, randomly joins neighbouring cells from different sets, and gives every set at least one connection down to the next row. On the last row it joins every remaining set. The result is a *perfect* maze: every passage cell can reach every other one.
2. **Rasterising.** The whole `walls` grid is set to wall. Then each passage cell is carved out, along with the wall cell between any two passages that were joined.
3. **Opening it up.** Walls are removed at random to create loops and wider areas:
   - border walls on the top row and left column next to a passage, with probability `SPARSITY` (0.7);
   - interior walls, with probability `SPARSITY * 0.5`.

   `canRemoveWall` only allows a wall to be removed if at least one of its four neighbours is already a passage, so no isolated pockets are created.

Stage 3 only removes walls, so the maze stays fully connected. Any two passage cells you click therefore have a path between them.

### The heap

`Heap<T>` is a classic **1-indexed** array binary min-heap:

- the root is at `heap[1]`, and `heap[0]` is never used;
- the children of `i` are `2i` and `2i + 1`, and its parent is `i / 2`.

| Operation | How | Cost |
| --- | --- | --- |
| `add(value)` | Place at `size + 1`, then sift up while smaller than the parent. Doubles the array first if it is full. | O(log n), amortised |
| `poll()` | Take the root, move the last element to the root, then sift down by swapping with the smaller child. Returns `null` when empty. | O(log n) |
| `getSize()` | Number of stored elements. | O(1) |

### Heap-based search

`PathFinder.findPathHeap` is **uniform-cost search**, which is Dijkstra's algorithm on a graph where every edge has weight 1:

```
push SearchNode(start, dist = 0, parent = null)
while heap not empty:
    node = poll()                 # node with the smallest dist
    if node.pos == goal:
        return reconstructPath(node)
    if node already visited: continue
    mark node visited
    for each of the 4 neighbours that is in bounds and not a wall:
        push SearchNode(neighbour, node.dist + 1, node)
return empty list
```

Nodes come out of the heap in order of distance from the start, so the first time the goal is polled, its path is a shortest one. `reconstructPath` follows `parent` pointers from the goal back to the start and builds the list in start-to-goal order.

On an unweighted grid this gives the same path lengths as BFS. The heap becomes useful once moves have different costs, or once you add a heuristic to turn the search into A* (see [Ideas](#ideas-for-extending-it)).

### BFS

`PathFinder.findPathBfs` is an **unimplemented exercise**: it currently returns an empty list. Its Javadoc outlines the intended approach:

- a `Queue<Point>` such as `ArrayDeque`;
- a `boolean[][] visited` grid;
- a `Point[][] parent` grid to rebuild the path once the goal is reached.

The `canMoveTo(row, col, rows, cols, walls, visited)` helper is already written for this.

### Animation

`MazePanel.run()` starts a Swing `Timer` that fires every 300 ms. Each tick moves `robotPosition` one step along `path` and repaints. The timer stops when the robot reaches the last cell before the goal; the robot icon is then hidden, so the house marks the end. Blue path squares are drawn everywhere except under the robot and the house.

## Configuration

There is no config file. To change the behaviour, edit these values and recompile:

| What | Where | Default |
| --- | --- | --- |
| Grid size | `new MazePanel(10, 10)` in `Maze.main` | 10 × 10 |
| Cell size (px) | `CELL_SIZE` in `MazePanel` | 40 |
| Maze openness | `SPARSITY` in `MazePanel` (0.0 = closest to a perfect maze, 1.0 = most open) | 0.7 |
| Animation speed (ms per step) | `new Timer(300, ...)` in `MazePanel.run()` | 300 |
| Icons | `resources/robot.png`, `resources/home.png` | — |

**Grid size:** generation is designed for even dimensions. Passages sit on odd coordinates, and the grid is split into `rows / 2 × cols / 2` passage cells, so with even sizes the outer wall ring stays intact. Odd sizes run, but leave an extra open strip along the bottom and right edges.

## Known issues and limitations

- **`SearchNode` overrides `equals` but not `hashCode`.** `findPathHeap` stores visited nodes in a `HashSet<SearchNode>`. Each node gets an identity-based hash code, so `visited.contains(...)` almost never finds a node that was already seen. Cells are expanded again and again, and the heap can grow very large on long paths. The path returned is still a shortest one, but the search does far more work than it should. Fix: add
  ```java
  @Override
  public int hashCode() { return pos.hashCode(); }
  ```
  or track visited cells in a `boolean[rows][cols]`.
- **Clicking Solve without a start and goal throws a `NullPointerException`.** `findPathHeap` creates a node at a `null` start and calls `pos.equals(...)` on it. You only see a stack trace in the console; the UI keeps running. A guard in `solveBfs`/`solveHeap`, or at the top of each `findPath*` method, would fix this.
- **Start equal to goal:** clicking the same cell twice gives a one-cell path. **Run** ignores paths shorter than two cells, so nothing animates.
- **"Solve (BFS)" does nothing** until `findPathBfs` is implemented.
- **Images are loaded from the working directory**, not the classpath. Run from the repository root, or switch to `getClass().getResource(...)` and put the images on the classpath.
- **Misleading comment:** the Javadoc on `solveHeap` and `reconstructPath` calls the search "greedy". It is ordered by distance travelled from the start, not by an estimate of the distance left, so it is uniform-cost search rather than greedy best-first search.
- **Unused parameter:** `findPathHeap` passes a fresh `new boolean[rows][cols]` to `canMoveTo` on every neighbour check, so that argument has no effect and allocates on every call.

## Ideas for extending it

- **Implement BFS** using the hints in `findPathBfs`, then compare its paths with the heap search.
- **A\* search:** add a Manhattan-distance heuristic `h = |Δrow| + |Δcol|` and order `SearchNode`s by `dist + h`. The `Heap` works unchanged.
- **Greedy best-first search:** order by `h` alone and see how the paths get worse.
- **Weighted terrain:** give cells different movement costs to show where Dijkstra and BFS start to disagree.
- **Show explored cells:** colour every cell the search expands to compare how much work each algorithm does.
- **Unit tests** for `Heap` (ordering, resizing, polling an empty heap) and for each search (path length, path continuity, no path through walls).
