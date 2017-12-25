import model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 11.11.2017.
 */
public class StartPlacement {
    private static final double FACTOR = 0.7;
    public static double startX;
    public static double startY;

    public static void move(List<Group> groups, ActionManager manager) {

        List<Group> myGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY))
                .collect(Collectors.toList());
        myGroups.sort(Comparator.comparingDouble(Group::getMaxSpeed));
        Group anyGroup = myGroups.get(0);
        if (anyGroup.getX() > 500) {
            startX = 1024;
            startY = 1024;
        }

        for (Group group : myGroups) {
           /*//right bottom
                manager.addAction(Action.scale(group.getY(), group.getX(), group.maxX, group.maxY, FACTOR,
                        group.maxX, group.maxY));

                //left bottom
                manager.addAction(Action.scale(group.getY(), group.minX, group.getX(), group.maxY, FACTOR,
                        group.minX, group.maxY));

                //top right
                manager.addAction(Action.scale(group.minY, group.getX(), group.maxX, group.getY(), FACTOR,
                        group.maxX, group.minY));

                //top left
                manager.addAction(Action.scale(group.minY, group.minX, group.getX(), group.getY(), FACTOR,
                        group.minX, group.minY));
                // right
                manager.addAction(Action.scale(group.minY, group.getX(), group.maxX, group.maxY, FACTOR,
                        group.maxX, group.getY()));

                // left
                manager.addAction(Action.scale(group.minY, group.minX, group.getX(), group.maxY, FACTOR,
                        group.minX, group.getY()));*/
            manager.addAction(Action.scale(group.minY, group.minX, group.maxX, group.maxY, 0.6, group.getX(), group.getY()));
        }
    }
}
