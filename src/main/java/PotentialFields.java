import model.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 19.11.2017.
 */
public class PotentialFields {
    private static int GRID = 16;
    private static double OBSTACLES = -10;
    private static double WALLS = -6;
    private static int SIZE = 64;
    public static int MIN_GOOD_GROUP_SIZE = 10;

    public static Target getBestDestination(Group group, List<Group> groups, Facility[] facilities, World world,
                                           boolean hamburgerStrategy) {
        /*if (!group.type.equals(VehicleType.HELICOPTER)) {
            return null;
        }*/
        int gap = 3;
        if (hamburgerStrategy || group.waitingForBuild) {
            gap = 1;
            if (world.getMyPlayer().getRemainingNuclearStrikeCooldownTicks() < 180
                    && group.isAerail) {
                gap = 1;
            }
        }

        List<Group> sameVehicles = groups.stream().filter(g -> g.id != group.id && g.type.equals(group.type)
                && (group.getSize() < g.getSize() || group.getSize() < MIN_GOOD_GROUP_SIZE)
                && g.ownership.equals(MyStrategy.Ownership.ALLY))
                .collect(Collectors.toList());
        List<Group> arrv = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)
                && g.type.equals(VehicleType.ARRV)).collect(Collectors.toList());
        List<Group> tanks = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)
                && g.type.equals(VehicleType.TANK)).collect(Collectors.toList());
        tanks.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));

        List<Group> ifv = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)
                && g.type.equals(VehicleType.IFV)).collect(Collectors.toList());
        ifv.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));

        Cell hideAerial = getHidePointForAerial(group, groups);

        List<Group> enemies = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY)).collect(Collectors.toList());
        prepareEnemiesForAerial(group, enemies);
        enemies.sort((first, second) -> Integer.compare(second.weaponSize, first.weaponSize));
        Group biggestEnemy = null;
        if (enemies.size() > 0) {
            biggestEnemy = enemies.get(0);
        }
        HashMap<Integer, Double> battles = prepareBattlesList(group, enemies);

        List<Group> allyObstacles = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)
                && g.id != group.id && g.isAerail == group.isAerail
                && pow2(group.getX() - g.getX()) + pow2(group.getY() - g.getY()) < pow2(GRID * 10)
                && !(g.type.equals(group.type) && (g.getSize() < MIN_GOOD_GROUP_SIZE || group.getSize() < MIN_GOOD_GROUP_SIZE
                || g.getSize() / 2.0 > group.getSize() || group.getSize() / 2.0 > g.getSize())))
                .collect(Collectors.toList());

        List<Facility> facilityList = prepareFacilityList(group, groups, world, world.getMyPlayer().getId());
        HashSet<Integer> dangerForFacility = prepareDangerousGroupsForFacilities(world, groups);

        int groupX = (int) group.getX() / GRID;
        int groupY = (int) group.getY() / GRID;
        double max = -Double.MAX_VALUE;
        int bestX = groupX;
        int bestY = groupY;
        boolean isTarget = false;
        boolean hasTarget = false;
        boolean hasCloseTarget = false;

        int i = 3;
        if (group.isAerail) {
            i = 4;
        }
        for (int dy = -i; dy <= i; dy++) {
            for (int dx = -i; dx <= i; dx++) {
                int x = groupX + dx;
                int y = groupY + dy;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
                    continue;
                }
                double charge = getObstaclesCharge(allyObstacles, x, y);

                double enemiesCharge = 0;
                for (Group enemy : enemies) {
                    int minSquaredDist = Integer.MAX_VALUE;
                    double points = battles.get(enemy.id);
                    if (points != 0) {
                        for (Cell cell : enemy.cells.keySet()) {
                            int squareDist = pow2(cell.x - x) + pow2(cell.y - y);
                            if (squareDist < minSquaredDist) {
                                minSquaredDist = squareDist;
                            }
                        }
                    }

                    if (points > 0) {
                        double dist = Math.sqrt(minSquaredDist);
                        if (enemiesCharge >= 0) {
                            if (dangerForFacility.contains(enemy.id)) {
                                enemiesCharge = Math.max(enemiesCharge, Math.min(150, points * 10) * Math.pow(0.8, dist));
                            } else {
                                enemiesCharge = Math.max(enemiesCharge, points * Math.pow(0.8, dist));
                            }
                        }
                        hasTarget = true;
                        if (!hasCloseTarget) {
                            double squareGroupDist = pow2(enemy.getX() - group.getX()) + pow2(enemy.getY() - group.getY());
                            hasCloseTarget = squareGroupDist < pow2(20 + 2 * group.getRadius() + 2 * enemy.getRadius());
                        }

                    } else if (points < 0) {
                        int fireDist;
                        fireDist = (int) (Math.ceil(20 + group.getRadius()) / GRID + gap);
                        if (minSquaredDist <= pow2(fireDist)) {
                            enemiesCharge = Math.min(enemiesCharge, points * Math.pow(0.8, Math.sqrt(minSquaredDist)));
                        }
                        if (!isTarget) {
                            double squareGroupDist = pow2(enemy.getX() - group.getX()) + pow2(enemy.getY() - group.getY());
                            isTarget = squareGroupDist < pow2(20 + 2 * group.getRadius() + 2 * enemy.getRadius());
                        }
                    }
                }
                if (!(charge < 0 && enemiesCharge > 0)) {
                    charge += enemiesCharge;
                }


                if (charge >= 0) {
                    double facilityCharge = getFacilitiesCharge(facilityList, world.getMyPlayer().getId(), x, y);
                    charge = Math.max(facilityCharge, charge);

                    if (hideAerial != null) {
                        double dist = pow2(hideAerial.x - x) + pow2(hideAerial.y - y);
                        dist = Math.sqrt(dist);
                        charge = Math.max(charge, 200 * (Math.pow(0.9, dist)));
                    }
                }



                //walls
                charge += getWallsCharge(isTarget, x, y);

                //Притяжение к своим
                if (!group.type.equals(VehicleType.ARRV)) {
                    for (Group same : sameVehicles) {
                        int oX = (int) same.getX() / GRID;
                        int oY = (int) same.getY() / GRID;
                        double dist = pow2(oX - x) + pow2(oY - y);
                        dist = Math.sqrt(dist);
                        if (charge >= 0 || !isTarget) {
                            charge += Math.min(same.getSize(), 50) * (Math.pow(0.8, dist)) / 2;
                        }
                    }
                }

                //лечимся
                if (charge >= 0) {
                    if (group.isAerail && !hasTarget && group.hp / group.getSize() < 90) {
                        for (Group a : arrv) {
                            int oX = (int) a.getX() / GRID;
                            int oY = (int) a.getY() / GRID;
                            double dist = pow2(oX - x) + pow2(oY - y);
                            dist = Math.sqrt(dist);
                            charge += a.getSize() * (Math.pow(0.8, dist));
                        }
                    }
                }
                //бомбим
                if (group.isAerail && !hasTarget && biggestEnemy != null && group.type.equals(VehicleType.FIGHTER)) {
                    int oX = (int) biggestEnemy.getX() / GRID;
                    int oY = (int) biggestEnemy.getY() / GRID;
                    int squaredDist = pow2(oX - x) + pow2(oY - y);
                    double fighterSquaredDist = pow2(group.getX() - biggestEnemy.getX()) + pow2(group.getY() - biggestEnemy.getY());
                    double maxBombingSquaredDist = pow2(group.getVisionRange());
                    double minBombingSquaredDist = pow2(group.getVisionRange() - 30);
                    if (fighterSquaredDist > maxBombingSquaredDist) {
                        charge += 0.1 * (Math.pow(0.8, Math.sqrt(squaredDist)));
                    } else if (fighterSquaredDist < minBombingSquaredDist) {
                        charge -= 0.1 * (Math.pow(0.8, Math.sqrt(squaredDist)));
                    }
                }

                //Самолеты к танкам
                if (MyStrategy.game.isFogOfWarEnabled() && charge == 0 && group.type.equals(VehicleType.FIGHTER) && !tanks.isEmpty()) {
                    Group a = tanks.get(0);
                    Vector vector = new Vector(a.getX(), a.getY(), StartPlacement.startX, StartPlacement.startY);
                    vector.reverse();
                    vector.setLength(a.getRadius() + 2 * group.getRadius());
                    int oX = (int) (a.getX() + vector.x) / GRID;
                    int oY = (int) (a.getY() + vector.y) / GRID;
                    double dist = pow2(oX - x) + pow2(oY - y);
                    dist = Math.sqrt(dist);
                    charge += a.getSize() * 10 * (Math.pow(0.8, dist));
                }

                //Вертолеты к БМП
                if (MyStrategy.game.isFogOfWarEnabled() && charge == 0 && group.type.equals(VehicleType.HELICOPTER) && !ifv.isEmpty()) {
                    Group a = ifv.get(0);
                    Vector vector = new Vector(a.getX(), a.getY(), StartPlacement.startX, StartPlacement.startY);
                    vector.setLength(a.getRadius() + group.getRadius());
                    int oX = (int) (a.getX() + vector.x) / GRID;
                    int oY = (int) (a.getY() + vector.y) / GRID;
                    double dist = pow2(oX - x) + pow2(oY - y);
                    dist = Math.sqrt(dist);
                    charge += a.getSize() * 10 * (Math.pow(0.8, dist));
                }

                //System.out.print(String.format("%.4f", charge) + " ");
                if ((charge > max && Math.abs(groupX - x) <= 4 && Math.abs(groupY - y) <= 4) || (charge == max
                        && pow2(bestX - groupX) + pow2(bestY - groupY) > pow2(x - groupX) + pow2(y - groupY))) {
                    max = charge;
                    bestX = x;
                    bestY = y;
                }
                /*if (group.type.equals(VehicleType.TANK)) {
                    if (charge > 0) {
                        //MyStrategy.vc.fillRect(GRID * x, GRID * y, GRID * (x + 1), GRID * (y + 1), new Color(185, Math.max(255 - (int) (charge * 4) , 0), 26));
                        MyStrategy.vc.rect(GRID * x, GRID * y, GRID * (x + 1), GRID * (y + 1), new Color(185, Math.max(255 - (int) (charge * 4) , 0), 26), 3);
                    } else if (charge < 0) {
                        //MyStrategy.vc.fillRect(GRID * x, GRID * y, GRID * (x + 1), GRID * (y + 1), new Color(58, Math.max(255 - (int) (-charge * 5) , 0), 222));
                        MyStrategy.vc.rect(GRID * x, GRID * y, GRID * (x + 1), GRID * (y + 1), new Color(58, Math.max(255 - (int) (-charge * 5) , 0), 222), 3);
                    }
                }*/
            }
            //System.out.println();
        }
        /*if (group.type.equals(VehicleType.TANK)) {
            MyStrategy.vc.endFrame();
        }*/
        //System.out.println("--------------------------");
        if (group.waitingForBuild && !isTarget) {
            return null;
        }

        if (bestX == groupX && bestY == groupY) {
            return null;
        }
        return new Target(bestX * GRID + GRID / 2, bestY * GRID + GRID / 2,
                getUrgency(group, hasCloseTarget, isTarget || hideAerial != null), isTarget);
    }

    private static Action.Urgency getUrgency(Group group, boolean hasCloseTarget, boolean isTarget) {
        if (group.getSize() <= 5 && !group.isAerail) {
            if (isTarget) {
                return Action.Urgency.NORMAL;
            } else {
                return Action.Urgency.LOW;
            }
        }
        if (isTarget || hasCloseTarget) {
            return Action.Urgency.QUICKLY;
        } else {
            return Action.Urgency.NORMAL;
        }
    }

    private static double getWallsCharge(boolean isTarget, int x, int y) {
        double charge = 0;
        double multiplier = isTarget ? 1 : 0.1;
        int wallX = Math.min(x, SIZE - 1 - x);
        if (wallX <= 6) {
            charge += WALLS * Math.pow(0.5, wallX) * multiplier;
        }
        int wallY = Math.min(y, SIZE - 1 - y);
        if (wallY <= 6) {
            charge += WALLS * Math.pow(0.5, wallY) * multiplier;
        }
        return charge;
    }

    private static double getObstaclesCharge(List<Group> allyObstacles, int x, int y) {
        double charge = 0;

        for (Group obstacle : allyObstacles) {
            int minSquaredDist = Integer.MAX_VALUE;
            for (Cell cell : obstacle.cells.keySet()) {
                int squareDist = pow2(cell.x - x) + pow2(cell.y - y);
                if (squareDist < minSquaredDist) {
                    minSquaredDist = squareDist;
                }
            }
            if (minSquaredDist <= pow2(3)) {
                double dist = Math.sqrt(minSquaredDist);
                charge = Math.min(charge, OBSTACLES * (Math.pow(0.8, dist)));
            }
        }
        return charge;
    }

    private static HashMap<Integer, Double> prepareBattlesList(Group group, List<Group> enemies) {
        HashMap<Integer, Double> battles = new HashMap<>();
        for (Group enemy : enemies) {
            double points = TSLanchester.calcBattlePoints(group, enemy);
            battles.put(enemy.id, points);
        }
        return battles;
    }

    private static List<Facility> prepareFacilityList(Group group, List<Group> groups, World world, long id) {
        List<Facility> result = new ArrayList<>();
        if (group.isAerail) {
            return result;
        }
        /*if (world.getTickIndex() <= 1500) {
            HashMap<Integer, Facility> map = FacilityManager.getFacilitiesMap(groups, world.getFacilities());
            Facility facility = map.get(group.id);
            if (facility != null) {
                result.add(facility);
                return result;
            }
        }*/

        for (Facility facility : world.getFacilities()) {
            if (facility.getOwnerPlayerId() == id && facility.getCapturePoints() == MyStrategy.game.getMaxFacilityCapturePoints()) {
                continue;
            }
            double fx = facility.getLeft() + 32;
            double fy = facility.getTop() + 32;

            Optional<Group> closeEnemyGroups = groups.stream().filter(g -> g.id != group.id && g.ownership.equals(MyStrategy.Ownership.ENEMY)
                    && pow2(fx - g.getX()) + pow2(fy - g.getY()) < pow2(32 * 4)
                    && pow2(fx - g.getX()) + pow2(fy - g.getY()) < pow2(fx - group.getX()) + pow2(fy - group.getY())
                    && TSLanchester.calcBattlePoints(group, g) < 0).findAny();
            if (closeEnemyGroups.isPresent()) {
                continue;
            }

            //Найдем союзников, которые ближе к фабрике
            List<Group> closerAllyGroup = groups.stream().filter(g -> !g.isAerail && g.id != group.id && g.ownership.equals(MyStrategy.Ownership.ALLY)
                    && pow2(fx - g.getX()) + pow2(fy - g.getY()) < pow2(32 * 6)
                    && pow2(fx - g.getX()) + pow2(fy - g.getY())
                    <  pow2(fx - group.getX()) + pow2(fy - group.getY())
                    && g.getSize() > 20).collect(Collectors.toList());
            List<Group> ableToDefend = new ArrayList<>();
            //Для всех таких союзников проверим, способны ли они победить врагов вокруг фабрики
            for (Group ally : closerAllyGroup) {
                Optional<Group> closeToAllyEnemyGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY)
                        && pow2(fx - g.getX()) + pow2(fy - g.getY()) < pow2(32 * 4)
                        //&& pow2(fx - g.getX()) + pow2(fy - g.getY()) < pow2(fx - ally.getX()) + pow2(fy - ally.getY())
                        && TSLanchester.calcBattlePoints(ally, g) < 0).findAny();
                if (closeToAllyEnemyGroups.isPresent()) {
                    continue;
                }
                ableToDefend.add(ally);
            }
            //Если такие есть, поищем другую цель
            if (!ableToDefend.isEmpty()) {
                continue;
            }
            result.add(facility);
        }
        return result;
    }

    private static double getFacilitiesCharge(List<Facility> facilities, long id, int x, int y) {
        double facilitiesCharge = 0;
        for (Facility facility : facilities) {
            double fx = facility.getLeft() + 32;
            double fy = facility.getTop() + 32;
            int oX = (int) (fx) / GRID;
            int oY = (int) (fy) / GRID;

            double multiplier = 1;
            if (facility.getOwnerPlayerId() == id) {
                multiplier = (MyStrategy.game.getMaxFacilityCapturePoints() - facility.getCapturePoints())
                        / MyStrategy.game.getMaxFacilityCapturePoints();
            }
            double dist = pow2(oX - x) + pow2(oY - y);
            dist = Math.sqrt(dist);
            int charge = 250;
            if (facility.getType().equals(FacilityType.CONTROL_CENTER)) {
                charge = 150;
            }
            facilitiesCharge = Math.max(facilitiesCharge, charge * multiplier * (Math.pow(0.9, dist)));
        }
        return facilitiesCharge;
    }

    private static Cell getHidePointForAerial(Group group, List<Group> groups) {
        if (!group.isAerail) {
            return null;
        }
        List<Group> dangerEnemies = groups.stream().filter(g -> g.isAerail && g.ownership.equals(MyStrategy.Ownership.ENEMY)
                && TSLanchester.calcBattlePoints(group, g) < 0
                && pow2(group.getX() - g.getX()) + pow2(group.getY() - g.getY()) < pow2(200))
                .collect(Collectors.toList());
        if (dangerEnemies.isEmpty()) {
            return null;
        }

        dangerEnemies.sort((first, second) -> Double.compare(pow2(group.getX() - second.getX()) + pow2(group.getY() - second.getY()),
                pow2(group.getX() - first.getX()) + pow2(group.getY() - first.getY())));

        Group enemy = dangerEnemies.get(0);

        List<Group> ifls = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)
                && VehicleType.IFV.equals(g.type) && TSLanchester.calcBattlePoints(g, enemy) > 0).collect(Collectors.toList());

        if (ifls.isEmpty()) {
            return null;
        }

        ifls.sort(Comparator.comparingDouble(g -> pow2(group.getX() - g.getX()) + pow2(group.getY() - g.getY())));
        Group ifl = ifls.get(0);
        Vector vector = new Vector(enemy.getX(), enemy.getY(), ifl.getX(), ifl.getY());
        vector.setLength(group.getRadius() + ifl.getRadius());
        Point point = new Point(ifl.getX() + vector.x, ifl.getY() + vector.y);
        Vector toEnemy = new Vector(group.getX(), group.getY(), enemy.getX(), enemy.getY());
        if (Math.abs(toEnemy.getAngle(new Vector(group.getX(), group.getY(), point.x, point.y))) < Math.PI / 3) {
            return null;
        }
        return Cell.createFromCoords(point.x, point.y);
    }

    private static void prepareEnemiesForAerial(Group group, List<Group> enemies) {
        if (VehicleType.FIGHTER.equals(group.type)) {
            List<Group> helicopters = enemies.stream().filter(g -> VehicleType.HELICOPTER.equals(g.type)
                    && TSLanchester.calcBattlePoints(group, g) > 0).collect(Collectors.toList());
            if (!helicopters.isEmpty()) {
                List<Group> weakFighter = enemies.stream().filter(g -> VehicleType.FIGHTER.equals(g.type)
                        && TSLanchester.calcBattlePoints(group, g) > 0).collect(Collectors.toList());
                enemies.removeAll(weakFighter);
            }

        } else if (VehicleType.HELICOPTER.equals(group.type)) {
            List<Group> tanks = enemies.stream().filter(g -> VehicleType.TANK.equals(g.type)
                    && TSLanchester.calcBattlePoints(group, g) > 0).collect(Collectors.toList());
            if (!tanks.isEmpty()) {
                List<Group> arrv = enemies.stream().filter(g -> VehicleType.ARRV.equals(g.type)).collect(Collectors.toList());
                enemies.removeAll(arrv);
            }
        }

    }

    private static HashSet<Integer> prepareDangerousGroupsForFacilities(World world, List<Group> groups) {
        HashSet<Integer> dangerous = new HashSet<>();
        for (Facility facility : world.getFacilities()) {
            if (facility.getOwnerPlayerId() != world.getMyPlayer().getId()) {
                continue;
            }
            double fx = facility.getLeft() + 32;
            double fy = facility.getTop() + 32;
            Optional<Group> buildingGroup = groups.stream().filter(g -> g.facilityId == facility.getId()).findAny();
            List<Group> enemyGroup = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ENEMY)
                && !g.isAerail
                && pow2(fx - g.getX()) + pow2(fy - g.getY()) <= pow2(160)
                && (!buildingGroup.isPresent() || TSLanchester.calcBattlePoints(buildingGroup.get(), g) < 0))
                    .collect(Collectors.toList());
            for (Group enemy : enemyGroup) {
                dangerous.add(enemy.id);
            }
        }
        return dangerous;
    }

/*    private double getRadiusInCellDirection(Group group, int x, int y) {
        int min = 100500;
        Cell closestCell = null;
        for (Cell cell : group.cells.keySet()) {
            int dxy = Math.abs(cell.x - x) + Math.abs(cell.y - y);
            if (dxy < min) {
                min = dxy;
                closestCell = cell;
            }
        }
        if (closestCell != null) {
            closestCell.
        }
    }*/

    private static double pow2(double value) {
        return value * value;
    }
    private static int pow2(int value) {
        return value * value;
    }
}
