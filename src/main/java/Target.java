/**
 * Created by Екатерина on 12.12.2017.
 */
public class Target extends Point {
    Action.Urgency urgency;
    boolean isTarget;

    public Target(double x, double y, Action.Urgency urgency, boolean isTarget) {
        super(x, y);
        this.urgency = urgency;
        this.isTarget = isTarget;
    }
}
