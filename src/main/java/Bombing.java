import model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Екатерина on 21.11.2017.
 */
public class Bombing {
    private int groupId = -1;
    private Map<Long, Vehicle> vehicles;
    private long playerId;
    private List<Group> groups;
    private TerrainType[][] terrainTypeByCellXY;
    private WeatherType[][] weatherTypeByCellXY;
    private ActionManager manager;
    private static int BOMB_RADIUS = 50;

    public Bombing(long id, TerrainType[][] terrainTypeByCellXY,
                   WeatherType[][] weatherTypeByCellXY, Map<Long, Vehicle> vehicles, ActionManager manager) {
        this.playerId = id;
        this.groups = groups;
        this.terrainTypeByCellXY = terrainTypeByCellXY;
        this.weatherTypeByCellXY = weatherTypeByCellXY;
        this.vehicles = vehicles;
        this.manager = manager;
    }

    public int bomb(List<Group> groups, World world) {
        int cd = Math.max(world.getMyPlayer().getRemainingNuclearStrikeCooldownTicks(),
                world.getMyPlayer().getRemainingActionCooldownTicks());
        //не бросали бомбу и не можем бросить новую
        this.groups = groups;
        if (groupId == -1 && cd > 0) {
            return -1;
        }

        //отдали команду бросить и ждем выполнения
        if (groupId >= 0) {
            return pointing(world);

        } else if (cd == 0) {
            //хотим дать команду бросить
            return tryBombing();
        }
        return -1;
    }

    private int pointing(World world) {
        Group bomber = null;
        for (Group group : groups) {
            if (group.id == groupId) {
                bomber = group;
            }
        }
        if (bomber == null) {
            clear();
            return -1;
        }

        //Если удар почти готов или же если он сорвался - очищаем
        if (world.getMyPlayer().getNextNuclearStrikeTickIndex() - world.getTickIndex() <= 1 && world.getMyPlayer().getNextNuclearStrikeTickIndex() > 0
                || (groupId >= 0 && world.getMyPlayer().getNextNuclearStrikeTickIndex() < 0 && world.getMyPlayer().getRemainingNuclearStrikeCooldownTicks() > 0)) {
            clear();
        }
        return bomber.id;
    }

    public void clear() {
        groupId = -1;
    }

    private int tryBombing() {
        List<Group> myGroups = groups.stream().filter(g -> g.ownership.equals(MyStrategy.Ownership.ALLY)).collect(Collectors.toList());
        myGroups.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));
        List<Vehicle> enemies = vehicles.values().stream().filter(v -> v.getPlayerId() != playerId).collect(Collectors.toList());
        DbScan dbScan = new DbScan(vehicles, playerId);
        List<Group> enemyGroups = new ArrayList<>();
        dbScan.divide(enemyGroups, 15, MyStrategy.Ownership.ENEMY, VehicleType.IFV, enemies);
        enemyGroups.sort((first, second) -> Integer.compare(second.weaponSize, first.weaponSize));

        if (enemyGroups.size() == 0) {
            return -1;
        }
        int minSize = (int) (enemyGroups.get(0).weaponSize * 0.25);

        Group bomber = null;
        Cell target = null;
        Group targetGroup = null;
        for (Group enemy : enemyGroups) {
            if (enemy.getSize() < minSize || bomber != null || (enemy.weaponSize == 0 && enemyGroups.size() > 1)) {
                break;
            }
            for (Group myGroup : myGroups) {
                double squaredDistance = pow2(myGroup.getX() - enemy.getX()) + pow2(myGroup.getY() - enemy.getY());
                if (pow2(BOMB_RADIUS + myGroup.getRadius() /2) <= squaredDistance
                        && squaredDistance < pow2(myGroup.getVisionRange() + myGroup.getRadius() + enemy.getRadius())) {
                    Cell bestCell = null;
                    double maxCnt = 0;
                    for (Map.Entry<Cell, Integer> entry : enemy.cells.entrySet()) {
                        Cell cell = entry.getKey();
                        double squaredDistanceForCell = pow2(myGroup.getX() - cell.getCoordX()) + pow2(myGroup.getY() - cell.getCoordY());
                        if (squaredDistanceForCell > pow2(myGroup.getVisionRange() + myGroup.getRadius())
                                || squaredDistanceForCell < pow2(BOMB_RADIUS + myGroup.getRadius() / 2)) {
                            continue;
                        }
                        double cnt = 0;
                        for (Map.Entry<Cell, Integer> nEntry : enemy.cells.entrySet()) {
                            Cell nCell = nEntry.getKey();
                            int dxy = Math.abs(cell.x - nCell.x) + Math.abs(cell.y - nCell.y);

                            if (dxy <= 3) {
                                cnt += nEntry.getValue() * (1 - 0.2 * dxy);
                            }
                        }
                        if (cnt > maxCnt) {
                            bestCell = cell;
                            maxCnt = cnt;
                        }
                    }
                    if (bestCell == null) {
                        continue;
                    }
                    Cell cell = bestCell;

                    List<Group> victims = myGroups.stream().filter(g -> g.id != myGroup.id
                        && pow2(g.getX() - cell.getCoordX()) + pow2(g.getY() - cell.getCoordY()) <= pow2(1.5 * BOMB_RADIUS)
                        && (g.getSize() > 4))
                            .collect(Collectors.toList());
                    if (victims.size() > 0) {
                        continue;
                    }

                    bomber = myGroup;
                    target = cell;
                    targetGroup = enemy;
                    break;
                }
            }
        }

        if (target == null) {
            return -1;
        }
        Vehicle pointer = null;
        double squaredMaxDistance = 0;
        Iterator<Long> iterator = bomber.getVehicleIterator();
        while (iterator.hasNext()) {
            Long id = iterator.next();
            Vehicle p = vehicles.get(id);
            double coeff = 1;
            if (p.isAerial()) {
                WeatherType type = weatherTypeByCellXY[(int) p.getX() / 32][(int) p.getY() / 32];
                if (type.equals(WeatherType.CLOUD)) {
                    coeff = 0.8;
                } else if (type.equals(WeatherType.RAIN)) {
                    coeff = 0.6;
                }
            } else {
                TerrainType type = terrainTypeByCellXY[(int) p.getX() / 32][(int) p.getY() / 32];
                if (type.equals(TerrainType.FOREST)) {
                    coeff = 0.8;
                }
            }
            double squaredDistance = pow2(p.getX() - target.getCoordX()) + pow2(p.getY() - target.getCoordY());
            if (pow2(BOMB_RADIUS) < squaredDistance && squaredDistance < p.getSquaredVisionRange() * pow2(coeff)) {
                if (squaredDistance > squaredMaxDistance) {
                    pointer = p;
                    squaredMaxDistance = squaredDistance;
                }
            }
        }

        if (pointer != null) {
            manager.removeActionsForGroup(bomber);
            manager.addAction(Action.move(bomber, new Vector(0, 0, 0, 0), Action.Urgency.URGENTLY));
            if (targetGroup.cells.size() > 1) {
                manager.addAction(Action.nuclearStrike(target.getCoordX(), target.getCoordY(), pointer.getId()));
            } else {
                manager.addAction(Action.nuclearStrike(targetGroup.getX(), targetGroup.getY(), pointer.getId()));
            }
            groupId = bomber.id;
            //System.out.println("BOOM");
        }
        return groupId;
    }

    private double pow2(double value) {
        return value * value;
    }
}
