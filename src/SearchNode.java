import java.awt.Point;

public class SearchNode implements Comparable<SearchNode>{
    public Point pos;   // reference to the grid position
    public int dist;       // distance from start
    public SearchNode parent; // previous node in the path

    public SearchNode(Point pos, int dist, SearchNode parent) {
        this.pos = pos;
        this.dist = dist;
        this.parent = parent;
    }

    // Equality based on position coordinates
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SearchNode)) {
            return false;
        }
        SearchNode other = (SearchNode) obj;
        return this.pos.getX() == other.pos.getX() && this.pos.getY() == other.pos.getY();
    }

    @Override
    public int compareTo(SearchNode searchNode) {
        return this.dist - searchNode.dist ;
    }
}