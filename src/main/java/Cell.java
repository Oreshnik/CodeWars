/**
 * Created by Екатерина on 27.11.2017.
 */
public class Cell {
    int x, y;
    public static final int GRID = 16;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Cell createFromCoords(double x, double y) {
        return new Cell((int) (x / GRID), (int) (y / GRID));
    }

    public double getCoordX() {
        return x * GRID + GRID / 2;
    }

    public double getCoordY() {
        return y * GRID + GRID / 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cell cell = (Cell) o;

        if (x != cell.x) return false;
        return y == cell.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}
