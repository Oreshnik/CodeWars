import static java.lang.StrictMath.PI;

/**
 * Created by Екатерина on 10.11.2017.
 */
public class Vector {
    double x, y;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(double xFrom, double yFrom, double xTo, double yTo) {
        this(xTo - xFrom, yTo - yFrom);
    }

    public void reverse() {
        x = -x;
        y = -y;
    }

    public double getLength() {
        return Math.sqrt(x * x + y * y);
    }

    public void setLength(double length) {
        double c = getLength() / length;
        x = x / c;
        y = y / c;
    }

    public double getAngle(Vector other) {
        double angle = Math.atan2(y, x);
        double otherAngle = Math.atan2(other.y, other.x);
        double relativeAngle = otherAngle - angle;
        while (relativeAngle > PI) {
            relativeAngle -= 2.0D * PI;
        }

        while (relativeAngle < -PI) {
            relativeAngle += 2.0D * PI;
        }

        return relativeAngle;
    }
}
