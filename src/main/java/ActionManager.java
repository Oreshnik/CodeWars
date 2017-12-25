import model.*;

import java.util.*;

import static java.lang.Math.PI;

/**
 * Created by Екатерина on 15.11.2017.
 */
public class ActionManager {
    private Map<Long, Integer> moveTickByVehicleId;
    private int tickIndex;
    private Action currentAction;
    private final HashMap<Integer, Action> lastActions = new HashMap<>();
    private static int queueId = -1;
    private List<Integer> actionTicks;

    public ActionManager(Map<Long, Integer> moveTickByVehicleId) {
        this.moveTickByVehicleId = moveTickByVehicleId;
        actionTicks = new ArrayList<>();
    }

    public void setTickIndex(int index) {
        tickIndex = index;
    }

    private final Queue<Action> actions = new PriorityQueue<>(new Comparator<Action>() {
        @Override
        public int compare(Action o1, Action o2) {
            if (o1.urgency.equals(o2.urgency)) {
                return Integer.compare(o1.queueId, o2.queueId);
            } else {
                return Integer.compare(o1.urgency.ordinal(), o2.urgency.ordinal());
            }
        }
    });


    public void renewActions(List<Group> groups, int tickIndex) {
        Iterator<Action> iterator = actions.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            if (action.group == null) {
                continue;
            }
            boolean found = false;

            for (Group group : groups) {
                if (action.getGroupId() == group.id) {
                    Move move = action.getMove(0);
                    if (move != null && ActionType.CLEAR_AND_SELECT.equals(move.getAction())) {
                        move.setLeft(group.minX);
                        move.setTop(group.minY);
                        move.setRight(group.maxX);
                        move.setBottom(group.maxY);
                        move.setY(group.getX());
                        move.setY(group.getY());
                    }
                    move = action.getMove(1);
                    if (move != null && ActionType.SCALE.equals(move.getAction())) {
                        move.setX(group.getX());
                        move.setY(group.getY());
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                lastActions.remove(action.getGroupId());
                iterator.remove();
            }
        }
        List<Integer> oldActions = new ArrayList<>();
        for (Action action : lastActions.values()) {
            if (tickIndex - action.tickIndex >= 300) {
                oldActions.add(action.getGroupId());
            }
        }
        for (Integer groupId : oldActions) {
            lastActions.remove(groupId);
        }
        actionTicks.removeIf(tickAction -> tickIndex - tickAction > MyStrategy.game.getActionDetectionInterval());
    }

    public void removeActionsForGroup(Group group) {
        actions.removeIf(action -> action.getGroupId() == group.id);
    }

    public boolean addAction(Action action) {
        Action oldAction = null;
        for (Action delayedAction : actions) {
            if (delayedAction.isDuplicate(action)) {
                oldAction = delayedAction;
                break;
            }
            if (delayedAction.getGroupId() != -1 && delayedAction.getGroupId() == action.getGroupId()
                    && (delayedAction.type.equals(ActionType.SCALE) || delayedAction.type.equals(ActionType.ROTATE))
                    && delayedAction.urgency.ordinal() <= action.urgency.ordinal()) {
                return false;
            }
        }
        if (oldAction != null) {
            oldAction.renewAction(action);
            actions.remove(oldAction);
            actions.add(oldAction);
            return true;
        }
        Action lastAction = lastActions.get(action.getGroupId());
        if (lastAction != null && lastAction.type.equals(action.type) && lastAction.type.equals(ActionType.MOVE)) {
            Vector last = new Vector(lastAction.getMove(1).getX(), lastAction.getMove(1).getY());
            Vector current = new Vector(action.getMove(1).getX(), action.getMove(1).getY());
            double lastTargetX = lastAction.group.getX() + lastAction.getMove(1).getX();
            double lastTargetY = lastAction.group.getY() + lastAction.getMove(1).getY();
            double newTargetX = action.group.getX() + action.getMove(1).getX();
            double newTargetY = action.group.getY() + action.getMove(1).getY();
            //Цель осталась той же
            if (Math.abs(lastTargetX - newTargetX) < 10 && Math.abs(lastTargetY - newTargetY) < 10
                    && action.getMove(1).getX() != 0 && action.getMove(1).getY() != 0) {
                return false;
            }
            double squaredDistanceToLastTarget = pow2( lastTargetX - action.group.getX()) + pow2(lastTargetY - action.group.getY());
            double squaredDistanceToNewTarget = pow2( newTargetX - action.group.getX()) + pow2(newTargetY - action.group.getY());
            //Если цель изменилась (следующая клетка в сетке), но направление осталось тем же и до старой цели еще долго ехать
            if (squaredDistanceToLastTarget >= pow2((lastAction.group.getMaxSpeed()) * 20)
                    && squaredDistanceToLastTarget <= squaredDistanceToNewTarget
                    && last.getAngle(current) < PI/36) {
                return false;
            }
        } else if (lastAction != null && lastAction.type.equals(action.type) && lastAction.type.equals(ActionType.SCALE)) {
            if ((lastAction.getMove(1).getFactor() < 1 && action.getMove(1).getFactor() < 1)
                    || (lastAction.getMove(1).getFactor() > 1 && action.getMove(1).getFactor() > 1)) {
                return false;
            }

        } else if (lastAction != null && lastAction.type.equals(ActionType.SCALE) && lastAction.urgency.ordinal() <= action.urgency.ordinal()) {
            long moving = action.group.getVehiclesStream().filter(v -> moveTickByVehicleId.get(v) != null
                    && tickIndex - moveTickByVehicleId.get(v) < 3).count();
            if (moving > action.group.getSize() / 3 && action.group.getSize() > 5) {
                return false;
            }
        } else if (lastAction != null && lastAction.type.equals(ActionType.ROTATE) && lastAction.urgency.ordinal() <= action.urgency.ordinal()) {
            long moving = action.group.getVehiclesStream().filter(v -> moveTickByVehicleId.get(v) != null
                    && tickIndex - moveTickByVehicleId.get(v) < 3).count();
            if (moving > action.group.getSize() / 2 && action.group.getSize() > 5) {
                return false;
            }
        }
        action.queueId = ++queueId;
        actions.add(action);
        return true;
    }

    public boolean executeDelayedMove(Move gameMove, World world) {

        Move move = currentAction != null ? currentAction.executeMove() : null;
        //нет комбинированного действия в процессе выполнения, но есть действия в очереди
        if (move == null && !actions.isEmpty()) {
            Action nextAction = actions.peek();
            //Срочные действия выполняем всегда
            if (!Action.Urgency.URGENTLY.equals(nextAction.urgency)) {
                //Проверим, не нужно ли оставить резерв для бомбы
                if (world.getTickIndex() > 400 && !checkLimitForBombs(world)) {
                    return false;
                }
                //Проверим, достаточно ли действий для непрерывного выполнения комбинированного действия
                if (nextAction.getMovesCount() == 2 && getActionsLimit(world) - actionTicks.size() < 2) {
                    return false;
                }
            }
        }
        if (move == null && actions.size() > 0) {
            currentAction = actions.poll();
            move = currentAction.executeMove();
        }

        if (move != null) {
            copyMove(gameMove, move);
            actionTicks.add(tickIndex);

            if (currentAction.getGroupId() != -1) {
                currentAction.tickIndex = tickIndex;
                lastActions.put(currentAction.getGroupId(), currentAction);
            }
            System.out.println(world.getTickIndex() + ": " + move.getAction() + " " + (currentAction.group != null ? currentAction.group.type : "")
                    + " x:" + move.getX() + " y:" + move.getY()
                + " t:" + move.getTop() + " l:" + move.getLeft() + " r:" + move.getRight() + " b:" + move.getBottom()
                    + " cnt:" + (currentAction.group != null ? currentAction.group.getSize() : ""));
            return true;
        }
        return false;
    }

    private static void copyMove(Move gameMove, Move move) {
        gameMove.setAction(move.getAction());
        gameMove.setAngle(move.getAngle());
        gameMove.setBottom(move.getBottom());
        gameMove.setFacilityId(move.getFacilityId());
        gameMove.setFactor(move.getFactor());
        gameMove.setGroup(move.getGroup());
        gameMove.setLeft(move.getLeft());
        gameMove.setMaxAngularSpeed(move.getMaxAngularSpeed());
        gameMove.setMaxSpeed(move.getMaxSpeed());
        gameMove.setRight(move.getRight());
        gameMove.setTop(move.getTop());
        gameMove.setVehicleId(move.getVehicleId());
        gameMove.setVehicleType(move.getVehicleType());
        gameMove.setX(move.getX());
        gameMove.setY(move.getY());
    }

    public void clear() {
        actions.clear();
        lastActions.clear();
    }

    private double pow2(double value) {
        return value * value;
    }

    public int getActionsLimit(World world) {
        int limit = MyStrategy.game.getBaseActionCount();
        for (Facility facility : world.getFacilities()) {
            if (facility.getOwnerPlayerId() == world.getMyPlayer().getId() && facility.getType().equals(FacilityType.CONTROL_CENTER)) {
                limit += MyStrategy.game.getAdditionalActionCountPerControlCenter();
            }
        }
        return limit;
    }

    private boolean checkLimitForBombs(World world) {
        if (world.getOpponentPlayer().getRemainingNuclearStrikeCooldownTicks() < MyStrategy.game.getActionDetectionInterval()
                || world.getMyPlayer().getRemainingNuclearStrikeCooldownTicks() < MyStrategy.game.getActionDetectionInterval()) {
            int limit = getActionsLimit(world);
            //Два действия чтобы распрыгнуть и два на желаемое действие
            int neededAction = 4;
            //Три действия на бросок бомбы и два на действие, которое желаем совершить
            if (world.getMyPlayer().getRemainingActionCooldownTicks() < MyStrategy.game.getActionDetectionInterval()) {
                neededAction = 5;
            }

            if (limit - actionTicks.size() < neededAction) {
                return false;
            }
        }
        return true;
    }
}