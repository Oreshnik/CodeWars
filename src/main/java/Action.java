import model.ActionType;
import model.Move;
import model.VehicleType;

/**
 * Created by Екатерина on 14.11.2017.
 */
public class Action {
    Move[] moves;
    Group group;
    Urgency urgency;
    int ind = 0;
    ActionType type;
    int queueId = -1;
    int tickIndex = 0;


    public Action(Group group, ActionType type, Urgency urgency) {
        this.group = group;
        moves = new Move[2];
        this.type = type;
        this.urgency = urgency;
    }


    public boolean isDuplicate(Action other) {
        if (group != null) {
            return other.getGroupId() == group.id && other.type.equals(type);
        }
        if (type.equals(ActionType.SETUP_VEHICLE_PRODUCTION) && type.equals(other.type)) {
            return getMove(1).getFacilityId() == other.getMove(1).getFacilityId();
        }
        return false;
    }

    public static Action move(Group group, Vector vector, Urgency urgency) {
        Action action = new Action(group, ActionType.MOVE, urgency);
        action.moves[0] = createSelect(group);
        Move move = new Move();
        move.setAction(ActionType.MOVE);
        move.setX(vector.x);
        move.setY(vector.y);
        action.moves[1] = move;
        return action;
    }

    public static Action move(Group group, Vector vector, double maxSpeed) {
        Action action = move(group, vector, Urgency.NORMAL);
        action.moves[1].setMaxSpeed(maxSpeed);
        return action;
    }

    public static Action move(Group group, Vector vector) {
        return move(group, vector, Urgency.NORMAL);
    }

    public static Action moveTo(Group group, double x, double y, Urgency urgency) {
        Vector vector = new Vector(group.getX(), group.getY(), x, y);
        return move(group, vector, urgency);
    }

    public static Action moveTo(Group group, double x, double y) {
        return moveTo(group, x, y, Urgency.NORMAL);
    }
    
    public static Action moveFrom(Group group, double x, double y, Urgency urgency) {
        Vector vector = new Vector(group.getX(), group.getY(), x, y);
        vector.reverse();
        return move(group, vector, urgency);
    }

    public static Action moveFrom(Group group, double x, double y) {
        return moveFrom(group, x, y, Urgency.NORMAL);
    }

    public static Action nuclearStrike(double x, double y, long id) {
        Action action = new Action(null, ActionType.TACTICAL_NUCLEAR_STRIKE, Urgency.URGENTLY);
        action.ind ++;
        Move strike = new Move();
        strike.setAction(ActionType.TACTICAL_NUCLEAR_STRIKE);
        strike.setX(x);
        strike.setY(y);
        strike.setVehicleId(id);
        action.moves[1] = strike;
        return action;
    }

    public static Action rotate(Group group, double x, double y, double angle, Urgency urgency) {
        Action action = new Action(group, ActionType.ROTATE, urgency);
        action.moves[0] = createSelect(group);
        Move rotate = new Move();
        rotate.setAction(ActionType.ROTATE);
        rotate.setX(x);
        rotate.setY(y);
        rotate.setAngle(angle);
        rotate.setMaxSpeed(group.getMaxSpeed() / 2);
        action.moves[1] = rotate;
        return action;
    }

    public static Action rotate(Group group, double x, double y, double angle) {
        return rotate(group, x, y, angle, Urgency.NORMAL);
    }

    public static Action scale(Group group, double x, double y, double factor, Urgency urgency) {
        Action action = new Action(group, ActionType.SCALE, urgency);
        action.moves[0] = createSelect(group);
        action.moves[1] = createScale(x, y, factor);
        return action;
    }

    private static Move createScale(double x, double y, double factor) {
        Move scale = new Move();
        scale.setAction(ActionType.SCALE);
        scale.setX(x);
        scale.setY(y);
        scale.setFactor(factor);
        return scale;
    }

    public static Action scale(Group group, double x, double y, double factor) {
        return scale(group, x, y, factor, Urgency.NORMAL);
    }

    public static Action scale(double top, double left, double right, double bottom, double factor, double x, double y) {
        return scale(top, left, right, bottom, factor, x, y, Urgency.NORMAL);
    }

    public static Action scale(double top, double left, double right, double bottom, double factor, double x, double y, Urgency urgency) {
        Action action = new Action(null, ActionType.SCALE, urgency);
        action.moves[0] = createSelect(top, left, right, bottom);
        action.moves[1] = createScale(x, y, factor);
        return action;
    }

    public static Action scaleSelected(double x, double y, double factor) {
        Action action = new Action(null, ActionType.SCALE, Urgency.URGENTLY);
        action.moves[1] = createScale(x, y, factor);
        action.ind ++;
        return action;
    }

    public static Action buildVehicles(long facilityId, VehicleType type) {
        Action action = new Action(null, ActionType.SETUP_VEHICLE_PRODUCTION, Urgency.QUICKLY);
        Move build = new Move();
        build.setAction(ActionType.SETUP_VEHICLE_PRODUCTION);
        build.setFacilityId(facilityId);
        build.setVehicleType(type);
        action.moves[1] = build;
        action.ind ++;
        return action;
    }

    private static Move createSelect(double top, double left, double right, double bottom) {
        Move move = new Move();
        move.setAction(ActionType.CLEAR_AND_SELECT);
        move.setTop(top);
        move.setLeft(left);
        move.setBottom(bottom);
        move.setRight(right);
        return move;
    }

    private static Move createSelect(Group group) {
        Move move = createSelect(group.minY - 1, group.minX - 1, group.maxX + 1, group.maxY + 1);
        move.setAction(ActionType.CLEAR_AND_SELECT);
        move.setVehicleType(group.type);
        return move;
    }


    public void renewAction(Action newAction) {
        moves = newAction.moves;
        type = newAction.type;
        urgency = newAction.urgency;
        group = newAction.group;
    }

    public Move executeMove() {
        if (ind >= moves.length) {
            return null;
        }
        return moves[ind++];
    }

    public Move getMove(int id) {
        if (id >= moves.length) {
            return null;
        }
        return moves[id];
    }

    public int getGroupId() {
        if (group == null) {
            return -1;
        }
        return group.id;
    }

    public int getMovesCount() {
        return moves[0] == null ? 1 : 2;
    }

    public static enum Urgency {
        URGENTLY, QUICKLY, NORMAL, LOW;
    }
}
