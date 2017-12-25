import model.Vehicle;
import model.VehicleType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 14.11.2017.
 */
public class GroupManager {
    private static final int DISTANCE = 200;
    private static final int OVERLAP = 4;
    //private static VisualClient vc = new VisualClient();

    public static List<Group> getMixedEnemyGroups(Map<Long, Vehicle> vehicleHashMap, long playerId) {
        List<Vehicle> enemies = vehicleHashMap.values().stream().filter(v -> v.getPlayerId() != playerId).collect(Collectors.toList());
        DbScan dbScan = new DbScan(vehicleHashMap, playerId);
        List<Group> enemyGroups = new ArrayList<>();
        dbScan.divide(enemyGroups, 15, MyStrategy.Ownership.ENEMY, VehicleType.IFV, enemies);
        return enemyGroups;
    }

    public static List<Group> renew(List<Group> groups, Map<Long, Vehicle> vehicleHashMap) {
        List<Group> newGroups = new ArrayList<>();
        for (Group group : groups) {
            group.renew(vehicleHashMap);
            if (group.getSize() > 0) {
                newGroups.add(group);
            }
        }
        return newGroups;
    }


    public static List<Group> clusteriseAndRenew(List<Group> groups, Map<Long, Vehicle> vehicleHashMap, long id) {
        DbScan dbScan = new DbScan(vehicleHashMap, id);
        List<Group> newGroups = dbScan.divide();
        loadPastGroupInformation(groups, newGroups);
        //draw(newGroups, vehicleHashMap);
        return newGroups;
    }

    private static void loadPastGroupInformation(List<Group> oldGroups, List<Group> newGroups) {
        newGroups.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));
        for (Group newGroup : newGroups) {
            List<Group> old = oldGroups.stream()
                    .filter(g -> g.ownership.equals(newGroup.ownership) && g.type.equals(newGroup.type)
                        && Math.abs(g.getX() - newGroup.getX()) <= DISTANCE && Math.abs(g.getY() - newGroup.getY()) <= DISTANCE)
                    .collect(Collectors.toList());
            old.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));
            Group twin = null;
            for (Group o : old) {
                int overlap = 0;
                Iterator<Long> iterator = newGroup.getVehicleIterator();
                while (iterator.hasNext()) {
                    Long id = iterator.next();
                    if (o.contains(id)) {
                        overlap++;
                    }
                    if (overlap >= Math.min(OVERLAP, newGroup.getSize())) {
                        twin = o;
                        break;
                    }
                }
            }
            if (twin != null) {
                newGroup.copyFromOld(twin);
                oldGroups.remove(twin);

            } else {
                newGroup.setAsNew();
            }
        }
    }

    /*private static void draw(List<Group> groups, Map<Long, Vehicle> vehicleHashMap) {
        vc.beginPre();
        for (Group group : groups) {
            Iterator<Long> iterator = group.getVehicleIterator();
            while (iterator.hasNext()) {
                Vehicle vehicle = vehicleHashMap.get((Long) iterator.next());
                vc.fillCircle(vehicle.getX(), vehicle.getY(), 6, group.getColor());
            }
        }
        vc.endPre();
    }*/
}
