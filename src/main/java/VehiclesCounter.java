import model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 13.12.2017.
 */
public class VehiclesCounter {
    HashMap<VehicleType, Integer> enemy;
    HashMap<VehicleType, Integer> ally;
    private HashSet<Long> deadVehicles;
    private double nextNuclearStrikeX = -1;
    private double nextNuclearStrikeY = -1;

    public VehiclesCounter() {
        enemy = new HashMap<>();
        ally = new HashMap<>();
        deadVehicles = new HashSet<>();
        for (VehicleType type : VehicleType.values()) {
            enemy.put(type, 100);
        }
    }

    public void calculateInFog(List<Group> groups, List<Vehicle> vehicles, World world) {
        if (!MyStrategy.game.isFogOfWarEnabled()) {
            return;
        }
        revivedFromNew(world.getNewVehicles()); //Те, кого ложно посчитали погибшими, а они вернулись
        renewFromFacilities(world.getFacilities(), world.getMyPlayer().getId()); //Новые с фабрик
        findAndDeleteDead(groups, vehicles);
        if (world.getMyPlayer().getNextNuclearStrikeTickIndex() == world.getTickIndex()) {
            nextNuclearStrikeX = world.getMyPlayer().getNextNuclearStrikeX();
            nextNuclearStrikeY = world.getMyPlayer().getNextNuclearStrikeY();
        } else {
            nextNuclearStrikeX = -1;
            nextNuclearStrikeY = -1;
        }
        //printEnemy(world.getTickIndex());
    }

    private void printEnemy(int tick) {
        System.out.print(tick + ": ");
        for (VehicleType type : VehicleType.values()) {
            enemy.getOrDefault(type, 0);
            System.out.print(type + ": " + enemy.getOrDefault(type, 0) + " ");
        }
        System.out.println();
    }

    public void getInfoFromGroups(List<Group> groups) {
        List<Group> myGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)).collect(Collectors.toList());
        List<Group> enemyGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)).collect(Collectors.toList());
        for (VehicleType type : VehicleType.values()) {
            ally.put(type, myGroups.stream().filter(g -> type.equals(g.type)).mapToInt(Group::getSize).sum());

            if (!MyStrategy.game.isFogOfWarEnabled()) {
                enemy.put(type, enemyGroups.stream().filter(g -> type.equals(g.type)).mapToInt(Group::getSize).sum());
            }
        }
    }


    private void revivedFromNew(Vehicle[] vehicles) {
        for (Vehicle vehicle : vehicles) {
            if (deadVehicles.contains(vehicle.getId())) {
                deadVehicles.remove(vehicle.getId());
                addVehicle(vehicle.getType());
            }
        }
    }

    private void findAndDeleteDead(List<Group> groups, List<Vehicle> deadVehicles) {
        List<Group> myGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)).collect(Collectors.toList());
        for (Vehicle dead : deadVehicles) {
            boolean couldBeBombed = couldBeBombed(dead);
            if (couldBeBombed) {
                removeVehicle(dead.getType(), dead.getId());
                continue;
            }
            boolean couldBeKilledByGroup = couldBeKilled(dead, myGroups);
            if (couldBeKilledByGroup) {
                removeVehicle(dead.getType(), dead.getId());
            }
        }
    }

    private boolean couldBeKilled(Vehicle vehicle, List<Group> groups) {
        List<Group> myGroups = groups.stream().filter(g -> pow2(g.getX() - vehicle.getX()) + pow2(g.getY() - vehicle.getY())
                <= pow2(g.getRadius() * 6)
                && ableToDamage(g.type, vehicle.getType())).collect(Collectors.toList());
        for (Group group : myGroups) {
            for (Cell cell : group.cells.keySet()) {
                if (pow2(cell.getCoordX() - vehicle.getX()) + pow2(cell.getCoordY() - vehicle.getY()) <= 2500) { //50*50
                    return true;
                }
            }
        }
        return false;
    }

    private boolean couldBeBombed(Vehicle vehicle) {
        return nextNuclearStrikeX >= 0 && nextNuclearStrikeY >= 0
                && pow2(nextNuclearStrikeX - vehicle.getX()) + pow2(nextNuclearStrikeY - vehicle.getY())
                <= pow2(MyStrategy.game.getTacticalNuclearStrikeRadius() + 10);
    }

    private void renewFromFacilities(Facility[] facilities, long playerId) {
        for (Facility facility : facilities) {
            if (facility.getOwnerPlayerId() != playerId && facility.getVehicleType() != null
                    && facility.getProductionProgress() == getProductionCount(facility.getVehicleType()) - 1) {
                addVehicle(facility.getVehicleType());
            }
        }
    }

    private void addVehicle(VehicleType newVehicle) {
        enemy.put(newVehicle, enemy.get(newVehicle) + 1);
    }

    private void removeVehicle(VehicleType deadVehicle, long vehicleId) {
        deadVehicles.add(vehicleId);
        enemy.put(deadVehicle, Math.max(enemy.get(deadVehicle) - 1, 0));
    }


    private static boolean ableToDamage(VehicleType attacker, VehicleType target) {
        if (attacker.equals(VehicleType.FIGHTER)) {
            return Arrays.asList(new VehicleType[]{VehicleType.FIGHTER, VehicleType.HELICOPTER}).contains(target);
        }
        if (attacker.equals(VehicleType.HELICOPTER)) {
            return Arrays.asList(new VehicleType[]{VehicleType.FIGHTER, VehicleType.HELICOPTER,
                    VehicleType.IFV, VehicleType.TANK, VehicleType.ARRV}).contains(target);
        }
        if (attacker.equals(VehicleType.TANK)) {
            return Arrays.asList(new VehicleType[]{VehicleType.HELICOPTER,
                    VehicleType.IFV, VehicleType.TANK, VehicleType.ARRV}).contains(target);
        }

        if (attacker.equals(VehicleType.IFV)) {
            return Arrays.asList(new VehicleType[]{VehicleType.FIGHTER, VehicleType.HELICOPTER,
                    VehicleType.IFV, VehicleType.TANK, VehicleType.ARRV}).contains(target);
        }

        return false;
    }

    private static int getProductionCount(VehicleType type) {
        if (type.equals(VehicleType.TANK)) {
            return MyStrategy.game.getTankProductionCost();
        } else if (type.equals(VehicleType.IFV)) {
            return MyStrategy.game.getIfvProductionCost();
        } else if (type.equals(VehicleType.HELICOPTER)) {
            return MyStrategy.game.getHelicopterProductionCost();
        } else if (type.equals(VehicleType.FIGHTER)) {
            return MyStrategy.game.getFighterProductionCost();
        } else {
            return MyStrategy.game.getArrvProductionCost();
        }
    }

    public int getAllyCount(VehicleType type) {
        return ally.get(type);
    }

    public int getEnemyCount(VehicleType type) {
        return enemy.get(type);
    }

    public int getEnemyCount() {
        return enemy.values().stream().mapToInt(i -> i).sum();
    }

    public int getAllyCount() {
        return ally.values().stream().mapToInt(i -> i).sum();
    }

    private double pow2(double value) {
        return value * value;
    }
}

