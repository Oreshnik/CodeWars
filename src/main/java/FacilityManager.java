
import model.Facility;
import model.FacilityType;
import model.VehicleType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 03.12.2017.
 */
public class FacilityManager {
    public static int GROUP_SIZE = 50;
    public static int AERIAL_GROUP_SIZE = 20;
    public static int FACILITY_SIZE = 64;
    private static HashMap<Integer, Integer> groupSizes = new HashMap<>();

    public static void buildForHell(Facility[] facilities, List<Group> groups, ActionManager manager, long playerId, VehiclesCounter counter) {
        List<VehicleType> neededVehicles = getNeededVehicles(counter, playerId, facilities);
        for (Facility facility : facilities) {
            if (facility.getOwnerPlayerId() == playerId && facility.getType().equals(FacilityType.VEHICLE_FACTORY)
                    /*&& facility.getVehicleType() == null*/) {
                //manager.addAction(Action.buildVehicles(facility.getId(), VehicleType.IFV));
                facilityDecision(facility, groups, neededVehicles, manager, facilities.length);
            }
        }
    }

    public static void checkGroupForBuilding(List<Group> groups, Facility[] facilities, long playerId) {
        List<Group> myGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)).collect(Collectors.toList());
        for (Facility facility : facilities) {
            if (facility.getOwnerPlayerId() == playerId && facility.getType().equals(FacilityType.VEHICLE_FACTORY)
                    && facility.getVehicleType() != null) {
                for (Group group : myGroups) {
                    int groupSize = group.isAerail ? AERIAL_GROUP_SIZE : groupSizes.getOrDefault(group.id, GROUP_SIZE);
                    if (!group.type.equals(facility.getVehicleType()) || group.getSize() >= groupSize) {
                        continue;
                    }
                    if (group.getX() > facility.getLeft() && group.getX() < facility.getLeft() + FACILITY_SIZE
                            && group.getY() > facility.getTop() && group.getY() < facility.getTop() + FACILITY_SIZE) {
                        group.waitingForBuild = true;
                        group.facilityId = facility.getId();
                    }
                }
            }
        }
    }

    private static List<VehicleType> getNeededVehicles(VehiclesCounter counter, long id, Facility[] facilities) {
        List<VehicleType> types = new ArrayList<>();
        if (counter.getAllyCount() >= 4 * counter.getEnemyCount() && counter.getAllyCount() > 500) {
            types.add(VehicleType.ARRV);
            return types;
        }


        Integer enemyTanks = counter.getEnemyCount(VehicleType.TANK);
        Integer myTanks = counter.getAllyCount(VehicleType.TANK);

        //Мало наземки - строим только наземку
        if (counter.getAllyCount(VehicleType.IFV) + myTanks < 200
                /*|| Arrays.stream(facilities).filter(f -> f.getOwnerPlayerId() == id && f.getType().equals(FacilityType.VEHICLE_FACTORY)).count() < 2*/) {
            if (enemyTanks > myTanks) {
                types.add(VehicleType.TANK);
            } else {
                types.add(VehicleType.IFV);
            }
            return types;
        }

        if (myTanks < enemyTanks) {
            if (counter.getEnemyCount(VehicleType.FIGHTER) == 0
                    || counter.getEnemyCount(VehicleType.FIGHTER) * 2 < counter.getAllyCount(VehicleType.FIGHTER)) {
                types.add(VehicleType.HELICOPTER);
            } else {
                types.add(VehicleType.TANK);
            }
        }
        if ((counter.getAllyCount(VehicleType.FIGHTER) < 10)
                && myTanks + counter.getAllyCount(VehicleType.IFV) >= 100) {
            if (Arrays.stream(facilities).noneMatch(f -> f.getOwnerPlayerId() == id && VehicleType.FIGHTER.equals(f.getVehicleType()))) {
                types.add(VehicleType.FIGHTER);
            }
        }

        VehicleType maxType = null;
        int maxCount = 0;
        int prevMax = 0;
        for (VehicleType type : VehicleType.values()) {
            int count = counter.getEnemyCount(type);
            if (count > maxCount) {
                prevMax = maxCount;
                maxCount = count;
                maxType = type;
            }
        }

        if (maxCount > 100 && prevMax > 0
                && maxCount >= prevMax * 1.5) {
            if (maxType.equals(VehicleType.HELICOPTER)) {
                if (maxCount > counter.getAllyCount(VehicleType.FIGHTER) * 5) {
                    types.add(VehicleType.FIGHTER);
                }

            } else if (maxType.equals(VehicleType.FIGHTER)) {
                types.add(VehicleType.IFV);

            } else if (maxType.equals(VehicleType.TANK) && counter.getEnemyCount(VehicleType.FIGHTER) * 2 < counter.getAllyCount(VehicleType.FIGHTER)) {
                if (maxCount > counter.getAllyCount(VehicleType.HELICOPTER)) {
                    types.add(VehicleType.HELICOPTER);
                }

            } else if (maxType.equals(VehicleType.TANK)) {
                if (maxCount > counter.getAllyCount(VehicleType.TANK)) {
                    types.add(VehicleType.TANK);
                }

            } else if (maxType.equals(VehicleType.IFV)) {
                if (maxCount > counter.getAllyCount(VehicleType.TANK) * 2.5) {
                    types.add(VehicleType.TANK);
                }
            }
        }


        return types;
    }

    private static void facilityDecision(Facility facility, List<Group> groups, List<VehicleType> types,
               ActionManager manager, int facilityCount) {
        int groupSize = GROUP_SIZE;
        if (facilityCount >= 10) {
            //Много зданий, нет врага
            if (!checkEnemyGround(facility, groups)) {
                groupSize = 33;
            }
        } else {
            //Мало зданий, рядом враг
            if (checkEnemyGround(facility, groups)) {
                groupSize = 60;
            }
        }

        //Если уже что-то строим - не отвлекаемся
        Optional<Group> buildingGroup = groups.stream().filter(g -> g.facilityId == facility.getId()).findAny();
        if (buildingGroup.isPresent()) {
            groupSizes.put(buildingGroup.get().id, groupSize);
        }
        if (facility.getVehicleType() != null
                && (!buildingGroup.isPresent() || (buildingGroup.get().getSize() < (buildingGroup.get().isAerail ? AERIAL_GROUP_SIZE : groupSize) - 1))) {
            if (buildingGroup.isPresent()) {
                Group group = buildingGroup.get();
                //Если нашему строительство подъедают, останавливаем производство
                if (group.isAerail && (checkEnemyFighter(facility, groups, group) || checkEnemyHelicopter(facility, groups, group))) {
                    manager.addAction(Action.buildVehicles(facility.getId(), null));
                }
            }

            return;
        }
        //Если пришли БРЭМ, значит пора останавливать производство
        if (types.contains(VehicleType.ARRV)) {
            if (facility.getVehicleType() != null) {
                manager.addAction(Action.buildVehicles(facility.getId(), null));
            }
            return;
        }

        //Уже производим что-то нужное или не нужно ничего, продолжаем делать, что делали
        if (types.contains(facility.getVehicleType())) {
            types.remove(facility.getVehicleType());
            return;
        }
        //Здесь мы либо закончили старую группу, либо еще ничего не делаем
        VehicleType typeForBuild = null;
        Iterator<VehicleType> iterator = types.iterator();
        while (iterator.hasNext()) {
            VehicleType type = iterator.next();
            if (type.equals(VehicleType.HELICOPTER) || type.equals(VehicleType.FIGHTER)) {
                if (checkEnemyIfl(facility, groups) || checkEnemyHelicopter(facility, groups) || checkEnemyFighter(facility, groups)) {
                    continue;
                }
            }
            if (type.equals(VehicleType.TANK)) {
                if (checkEnemyHelicopter(facility, groups)) {
                    continue;
                }
            }
            typeForBuild = type;
            break;
        }

        if (typeForBuild != null) {
            types.remove(typeForBuild);
        } else {
            List<Group> enemyTanks = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && g.type.equals(VehicleType.TANK) && g.getSize() >= 10
                    && pow2(180) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY()))
                    .collect(Collectors.toList());
            if (enemyTanks.size() > 0) {
                typeForBuild = VehicleType.TANK;
            } else {
                typeForBuild = VehicleType.IFV;
            }
        }

        if (facility.getVehicleType() == null || !facility.getVehicleType().equals(typeForBuild)) {
            manager.addAction(Action.buildVehicles(facility.getId(), typeForBuild));
        }

    }

    private static boolean checkEnemyHelicopter(Facility facility, List<Group> groups) {
        Optional<Group> helicopters =  groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && g.type.equals(VehicleType.HELICOPTER) && g.getSize() >= 10
                            && pow2(350) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY()))
                            .findAny();
        return helicopters.isPresent();
    }

    private static boolean checkEnemyIfl(Facility facility, List<Group> groups) {
        Optional<Group> ifl = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && g.type.equals(VehicleType.IFV) && g.getSize() >= 10
                        && pow2(200) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY()))
                            .findAny();
        return ifl.isPresent();
    }

    private static boolean checkEnemyFighter(Facility facility, List<Group> groups, Group group) {
        Optional<Group> fighter = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && g.type.equals(VehicleType.FIGHTER) && g.getSize() >= 10
                && pow2(400) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY())
                && TSLanchester.calcBattlePoints(group, g) < 0)
                .findAny();
        return fighter.isPresent();
    }

    private static boolean checkEnemyFighter(Facility facility, List<Group> groups) {
        Optional<Group> fighter = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && g.type.equals(VehicleType.FIGHTER) && g.getSize() >= 10
                && pow2(400) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY()))
                .findAny();
        return fighter.isPresent();
    }

    private static boolean checkEnemyGround(Facility facility, List<Group> groups) {
        int enemyCount = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && !g.isAerail
                && pow2(200) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY())
                ).mapToInt(Group::getWeaponSize).sum();
        return enemyCount > 20;
    }

    private static boolean checkEnemyHelicopter(Facility facility, List<Group> groups, Group group) {
        Optional<Group> helicopters = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY) && g.type.equals(VehicleType.HELICOPTER)
                && pow2(350) > pow2(facility.getLeft() + FACILITY_SIZE / 2 - g.getX()) + pow2(facility.getTop() + FACILITY_SIZE / 2 - g.getY())
                && TSLanchester.calcBattlePoints(group, g) < 0)
                .findAny();
        return helicopters.isPresent();
    }

    public static HashMap<Integer, Facility> getFacilitiesMap(List<Group> groups, Facility[] facilities) {
        HashMap<Integer, Facility> map = new HashMap<>();
        List<Group> invaders = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)
                && !g.isAerail).collect(Collectors.toList());
        List<Facility> facilityList = new ArrayList<>();
        facilityList.addAll(Arrays.asList(facilities));
        while (map.size() < Math.min(invaders.size(), facilities.length)) {
            List<Distance> minDistance = new ArrayList<>();
            for (Group group : invaders) {
                List<Distance> minGroupDistance = new ArrayList<>();
                for (Facility facility : facilityList) {
                    double fx = facility.getLeft() + 32;
                    double fy = facility.getTop() + 32;
                    double distance = Math.sqrt(pow2(fx - group.getX()) + Math.sqrt(pow2(fy - group.getY())));
                    minGroupDistance.add(new Distance(group, facility, distance / group.getMaxSpeed()));
                }
                minGroupDistance.sort(Comparator.comparingDouble(Distance::getTime));
                minDistance.add(minGroupDistance.get(0));
            }
            if (minDistance.size() > facilityList.size()) {
                minDistance.sort(Comparator.comparingDouble(Distance::getTime));
                while (minDistance.size() > facilityList.size()) {
                    minDistance.remove(minDistance.size() - 1);
                }
            }
            Distance distance = minDistance.get(0);
            minDistance.clear();
            map.put(distance.group.id, distance.facility);
            invaders.remove(distance.group);
            facilityList.remove(distance.facility);
        }
        return map;
    }

    private static class Distance {
        Group group;
        Facility facility;
        double time;
        public Distance (Group group, Facility facility, double time) {
            this.facility = facility;
            this.group = group;
            this.time = time;
        }

        public double getTime() {
            return time;
        }
    }

    private static double pow2(double value) {
        return value * value;
    }
}
