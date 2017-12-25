import model.Vehicle;
import model.VehicleType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 13.11.2017.
 */
public class DbScan {

    Map<Long, Vehicle> vehicleHashMap;
    private long playerId;
    private static final int eps = 17;
    private static final int minPts = 5;
    private HashSet<Long> visited;
    private HashSet<Long> noise;

    public DbScan(Map<Long, Vehicle> vehicleHashMap, long playerId) {
        this.vehicleHashMap = vehicleHashMap;
        this.playerId = playerId;
        visited = new HashSet<>();
        noise = new HashSet<>();
    }

    public List<Group> divide() {
        //long start = System.currentTimeMillis();

        List<Group> groups = new ArrayList<>();
        divide(groups, MyStrategy.Ownership.ALLY, minPts);
        divide(groups, MyStrategy.Ownership.ENEMY, minPts);

        //Соберем шум
        vehicleHashMap = vehicleHashMap.entrySet().stream().filter(e -> noise.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        visited.clear();
        noise.clear();

        divide(groups, MyStrategy.Ownership.ALLY, 2);
        for (Long id : noise) {
            Group group = new Group(vehicleHashMap.get(id).getType(), MyStrategy.Ownership.ALLY);
            group.addVehicle(vehicleHashMap.get(id));
            groups.add(group);
        }
        noise.clear();

        divide(groups, MyStrategy.Ownership.ENEMY, 2);
        for (Long id : noise) {
            Group group = new Group(vehicleHashMap.get(id).getType(), MyStrategy.Ownership.ENEMY);
            group.addVehicle(vehicleHashMap.get(id));
            groups.add(group);
        }

        //System.out.println(System.currentTimeMillis() - start);
        return groups;
    }

    private void divide(List<Group> groups, MyStrategy.Ownership ownership, int minPts) {
        List<Vehicle> myVehicles = vehicleHashMap.values().stream()
                .filter(v -> (v.getPlayerId() == playerId && ownership.equals(MyStrategy.Ownership.ALLY))
                    || (v.getPlayerId() != playerId && ownership.equals(MyStrategy.Ownership.ENEMY))).collect(Collectors.toList());
        for (VehicleType type : VehicleType.values()) {
            List<Vehicle> vehicles = myVehicles.stream().filter(v -> v.getType().equals(type)).collect(Collectors.toList());
            divide(groups, minPts, ownership, type, vehicles);
        }
    }

    public void divide(List<Group> groups, int minPts, MyStrategy.Ownership ownership, VehicleType type, List<Vehicle> vehicles) {
        for (Vehicle vehicle : vehicles) {
            if (visited.contains(vehicle.getId())) {
                continue;
            }
            List<Vehicle> neighbours = regionQuery(vehicles, vehicle);
            if (neighbours.size() < minPts) {
                visited.add(vehicle.getId());
                noise.add(vehicle.getId());
            } else {
                Group group = new Group(type, ownership);
                groups.add(group);
                expandClaster(vehicle, neighbours, group, vehicles, minPts);
            }
        }
    }

    private void expandClaster(Vehicle point, List<Vehicle> neighbours, Group group, List<Vehicle> vehicles, int minPts) {
        visited.add(point.getId());
        group.addVehicle(point);
        LinkedList<Vehicle> list = new LinkedList<>();
        list.addAll(neighbours);
        while (!list.isEmpty()) {
            Vehicle vehicle = list.pop();
            if (!visited.contains(vehicle.getId())) {
                List<Vehicle> qNeighbours = regionQuery(vehicles, vehicle);
                if (qNeighbours.size() > minPts) {
                    list.addAll(qNeighbours);
                }
            }
            if (!visited.contains(vehicle.getId()) || noise.contains(vehicle.getId())) {
                group.addVehicle(vehicle);
                noise.remove(vehicle.getId());
            }
            visited.add(vehicle.getId());
        }
    }

    private List<Vehicle> regionQuery(List<Vehicle> vehicles, Vehicle point) {
        List<Vehicle> neighbours = new ArrayList<>();
        double pow2Eps = eps * eps;
        for (Vehicle vehicle : vehicles) {
            if ((point.getX() - vehicle.getX()) * (point.getX() - vehicle.getX())
                    + (point.getY() - vehicle.getY()) * (point.getY() - vehicle.getY()) < pow2Eps) {
                neighbours.add(vehicle);
            }
        }
        return neighbours;
    }

}
