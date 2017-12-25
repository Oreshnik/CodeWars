import model.World;

/**
 * Created by Екатерина on 23.11.2017.
 */
public class Evasion {
    private static final int BOMB_RADIUS = 50;
    private static final int BOMB_TICKS = 29;
    private int groupTimer = 0;
    private int bombTick = -1;
    private double x;
    private double y;

    public boolean evade(World world, ActionManager manager) {
        if (world.getOpponentPlayer().getNextNuclearStrikeTickIndex() > -1
                && world.getOpponentPlayer().getNextNuclearStrikeTickIndex() - world.getTickIndex() == BOMB_TICKS
                && world.getMyPlayer().getRemainingActionCooldownTicks() < BOMB_TICKS) {
            x = world.getOpponentPlayer().getNextNuclearStrikeX();
            y = world.getOpponentPlayer().getNextNuclearStrikeY();
            manager.clear();
            manager.addAction(Action.scale(Math.max(y - BOMB_RADIUS, 0), Math.max(x - BOMB_RADIUS, 0),
                    Math.min(x + BOMB_RADIUS, world.getWidth() - 1), Math.min(y + BOMB_RADIUS, world.getHeight() - 1),
                            10, x, y, Action.Urgency.URGENTLY));
            bombTick = world.getOpponentPlayer().getNextNuclearStrikeTickIndex();
            groupTimer = BOMB_TICKS - world.getMyPlayer().getRemainingActionCooldownTicks();
            return true;
        }

        if (bombTick > world.getTickIndex()) {
            return true;
        }

        if (bombTick == world.getTickIndex()) {
            manager.addAction(Action.scaleSelected(x, y, 0.1));
            groupTimer += world.getMyPlayer().getRemainingActionCooldownTicks();
            return  true;
        }

        if (groupTimer > 0) {
            groupTimer --;
            return true;
        }

        return false;
    }
}
