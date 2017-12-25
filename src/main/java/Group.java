import model.Vehicle;
import model.VehicleType;

import java.awt.Color;
import java.util.*;
import java.util.stream.Stream;


/**
 * Created by Екатерина on 13.11.2017.
 */
public class Group {
    private static int ids = 0;
    private static Color[] colors = new Color[] {Color.PINK, Color.CYAN, Color.LIGHT_GRAY, Color.ORANGE, Color.YELLOW,
            Color.BLUE, Color.RED, Color.MAGENTA};

    int id = -1;
    private HashSet<Long> vehicleList;
    private double massX, massY;
    double minX, minY, maxX, maxY;
    int hp = 0;
    VehicleType type;
    MyStrategy.Ownership ownership;
    private double maxSpeed;
    private double visionRange;
    private double squaredVisionRange;
    private double groundAttackRange;
    private double squaredGroundAttackRange;
    private double aerialAttackRange;
    private double squaredAerialAttackRange;
    private int groundDamage;
    private int aerialDamage;
    private int groundDefence;
    private int aerialDefence;
    int weaponSize = 0;
    boolean isAerail;
    boolean waitingForBuild;
    long facilityId = -1;
    HashMap<Cell, Integer> cells;

    public Group(VehicleType type, MyStrategy.Ownership ownership) {
        this.type = type;
        this.ownership = ownership;
        init();
    }

    private void init() {
        minX = 1024;
        minY = 1024;
        maxX = -1;
        maxY = -1;
        massX = 0;
        massY = 0;
        hp = 0;
        weaponSize = 0;
        cells = new HashMap<>();
        vehicleList = new HashSet<>();
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicleList.size() == 0) {
            maxSpeed = vehicle.getMaxSpeed();
            visionRange = vehicle.getVisionRange();
            squaredVisionRange = vehicle.getSquaredVisionRange();
            groundAttackRange = vehicle.getGroundAttackRange();
            squaredGroundAttackRange = vehicle.getSquaredGroundAttackRange();
            aerialAttackRange = vehicle.getAerialAttackRange();
            squaredAerialAttackRange = vehicle.getSquaredAerialAttackRange();
            groundDamage = vehicle.getGroundDamage();
            aerialDamage = vehicle.getAerialDamage();
            groundDefence = vehicle.getGroundDefence();
            aerialDefence = vehicle.getAerialDefence();
            this.isAerail = vehicle.isAerial();
        }

        vehicleList.add(vehicle.getId());
        massX += vehicle.getX();
        massY += vehicle.getY();
        minX = Math.min(minX, vehicle.getX());
        minY = Math.min(minY, vehicle.getY());
        maxX = Math.max(maxX, vehicle.getX());
        maxY = Math.max(maxY, vehicle.getY());
        hp += vehicle.getDurability();
        if (!VehicleType.ARRV.equals(vehicle.getType())) {
            weaponSize ++;
        }
        Cell cell = Cell.createFromCoords(vehicle.getX(), vehicle.getY());
        Integer c = cells.get(cell);
        if (c == null) {
            c = 0;
        }
        c++;
        cells.put(cell, c);
    }

    public void renew(Map<Long, Vehicle> vehicles) {
        HashSet<Long> oldList = new HashSet<>(vehicleList);
        init();
        Iterator<Long> iterator = oldList.iterator();
        while (iterator.hasNext()) {
            Long id = iterator.next();
            Vehicle vehicle = vehicles.get(id);
            if (vehicle != null) {
                addVehicle(vehicle);
            }
        }
    }

/*    public double getRadiusInDirection(double x, double y) {
        Vector vector = new Vector(getX(), getY(), x, y);
        if (vector.getLength() < getRadius() * 2) {

        }
    }*/

    public void setAsNew() {
        id = ids++;
    }

    public double getX() {
        return massX / vehicleList.size();
    }

    public double getY() {
        return massY / vehicleList.size();
    }

    public int getSize() {
        return vehicleList.size();
    }

    public Iterator<Long> getVehicleIterator() {
        return vehicleList.iterator();
    }

    public Stream<Long> getVehiclesStream() {
        return vehicleList.stream();
    }

    public boolean contains(Long id) {
        return vehicleList.contains(id);
    }

    public void copyFromOld(Group oldGroup) {
        id = oldGroup.id;
    }

    public Color getColor() {
        return colors[id % colors.length];
    }

    @Override
    public String toString() {
        return type.name() + " " + getSize() + " " + ownership.name();
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }
    public double getVisionRange() {
        return visionRange;
    }
    public double getSquaredVisionRange() {
        return squaredVisionRange;
    }
    public double getGroundAttackRange() {
        return groundAttackRange;
    }
    public double getSquaredGroundAttackRange() {
        return squaredGroundAttackRange;
    }
    public double getAerialAttackRange() {
        return aerialAttackRange;
    }
    public double getSquaredAerialAttackRange() {
        return squaredAerialAttackRange;
    }
    public int getGroundDamage() {
        return groundDamage;
    }
    public int getAerialDamage() {
        return aerialDamage;
    }
    public int getGroundDefence() {
        return groundDefence;
    }
    public int getAerialDefence() {
        return aerialDefence;
    }

    public double getRadius() {
        return (maxX - minX + maxY - minY) / 4;
    }

    public double getDensity() {
        return vehicleList.size() / ((2 * getRadius()) * (2 * getRadius()));
    }

    public int getWeaponSize() {
        return weaponSize;
    }
}