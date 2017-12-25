import model.*;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.PI;


public final class MyStrategy implements Strategy {
    private Random random;

    private TerrainType[][] terrainTypeByCellXY;
    private WeatherType[][] weatherTypeByCellXY;

    private Player me;
    private World world;
    public static Game game;
    private Move move;
    private List<Group> groups = new ArrayList<>();

    private final Map<Long, Vehicle> vehicleById = new HashMap<>();
    private final Map<Long, Integer> updateTickByVehicleId = new HashMap<>();
    private final Map<Long, Integer> moveTickByVehicleId = new HashMap<>();
    private ActionManager actionManager;
    private Bombing bombing;
    private int bomberId;
    private HashMap<Integer, Integer> lastScale = new HashMap<>();
    private HashMap<Integer, Integer> scaleStage = new HashMap<>();
    private List<Vehicle> deadVehicles = new ArrayList<>();
    private Evasion evasion = new Evasion();
    private VehiclesCounter vehiclesCounter;
    private boolean opponentIsHamburger = false;
    //public static VisualClient vc = new VisualClient();
    //public static RewindClient vc = new RewindClient();

    /**
     * Основной метод стратегии, осуществляющий управление армией. Вызывается каждый тик.
     *
     * @param me    Информация о вашем игроке.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Player me, World world, Game game, Move move) {
        Locale.setDefault(Locale.US);
        initializeStrategy(world, game, me);
        initializeTick(me, world, game, move);

        //visualise();
        //vc.endFrame();
        if (world.getTickIndex() % 500 == 0) {
            checkHamburger();
        }

        boolean evade = evasion.evade(world, actionManager);
        if (game.isFogOfWarEnabled()) {
            vehiclesCounter.calculateInFog(groups, deadVehicles, world);
        }
        if (world.getTickIndex() % 5 == 0) {
            groups = GroupManager.clusteriseAndRenew(groups, vehicleById, me.getId());
        } else {
            groups = GroupManager.renew(groups, vehicleById);
        }
        if (me.getRemainingActionCooldownTicks() > 0) {
            //vc.endFrame();
            return;
        }
        vehiclesCounter.getInfoFromGroups(groups);
        actionManager.renewActions(groups, world.getTickIndex());
        FacilityManager.checkGroupForBuilding(groups, world.getFacilities(), me.getId());
        FacilityManager.buildForHell(world.getFacilities(), groups, actionManager, me.getId(), vehiclesCounter);

        if (world.getTickIndex() == 0) {
            StartPlacement.move(groups, actionManager);
        }

        if (!evade) {
            bomberId = bombing.bomb(groups, world);
        } else {
            bombing.clear();
        }

        if (world.getTickIndex() >= 50 && !evade) {
            //vc.beginPre();
            move();
            //vc.endPre();
        }
        actionManager.executeDelayedMove(move, world);
        //vc.endFrame();
    }

    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
    private void initializeStrategy(World world, Game game, Player me) {
        if (random == null) {
            random = new Random(game.getRandomSeed());
            terrainTypeByCellXY = world.getTerrainByCellXY();
            weatherTypeByCellXY = world.getWeatherByCellXY();
        }
        if (actionManager == null) {
            actionManager = new ActionManager(updateTickByVehicleId);
        }
        if (bombing == null) {
            bombing = new Bombing(me.getId(), terrainTypeByCellXY, weatherTypeByCellXY, vehicleById, actionManager);
        }
        if (vehiclesCounter == null) {
            vehiclesCounter = new VehiclesCounter();
        }
    }

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним, а также актуализируем сведения о каждой
     * технике и времени последнего изменения её состояния.
     */
    private void initializeTick(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        MyStrategy.game = game;
        this.move = move;

        actionManager.setTickIndex(world.getTickIndex());
        deadVehicles.clear();

        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicleById.put(vehicle.getId(), vehicle);
            updateTickByVehicleId.put(vehicle.getId(), world.getTickIndex());
            moveTickByVehicleId.put(vehicle.getId(), world.getTickIndex());
        }

        for (VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {
            long vehicleId = vehicleUpdate.getId();

            if (vehicleUpdate.getDurability() == 0) {
                Vehicle dead = vehicleById.get(vehicleId);
                if (dead.getPlayerId() == world.getOpponentPlayer().getId()) {
                    deadVehicles.add(dead);
                }
                vehicleById.remove(vehicleId);
                updateTickByVehicleId.remove(vehicleId);
                moveTickByVehicleId.remove(vehicleId);
            } else {
                if (Math.abs(vehicleUpdate.getX() - vehicleById.get(vehicleId).getX()) > 0.01
                        || Math.abs(vehicleUpdate.getY() - vehicleById.get(vehicleId).getY()) > 0.01) {
                    moveTickByVehicleId.put(vehicleId, world.getTickIndex());
                }
                updateTickByVehicleId.put(vehicleId, world.getTickIndex());
                vehicleById.put(vehicleId, new Vehicle(vehicleById.get(vehicleId), vehicleUpdate));
            }
        }
    }

    private void move() {
        List<Group> myGroups = groups.stream().filter(g -> g.ownership.equals(Ownership.ALLY))
                .collect(Collectors.toList());
        myGroups.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));

        for (Group group : myGroups) {
            if (group.id == bomberId) {
                continue;
            }

            Target destination = null;
            if (!(group.waitingForBuild && !group.isAerail)) {
                destination = PotentialFields.getBestDestination(group, groups, world.getFacilities(), world, opponentIsHamburger);
            }
            if (destination == null || !destination.isTarget) {
                scaleGroup(group);
            }
            if (destination != null) {
                actionManager.addAction(Action.moveTo(group, destination.x, destination.y/*, destination.urgency*/));
            }
        }
    }

    private void scaleGroup(Group group) {
        boolean nuclearDanger = world.getOpponentPlayer().getRemainingNuclearStrikeCooldownTicks() < 250;
        Integer lastScaleTick = lastScale.get(group.id);
        Integer stage = scaleStage.get(group.id);
        if (!nuclearDanger && group.getDensity() < 0.03 && (lastScaleTick == null || world.getTickIndex() - lastScaleTick > 800 || stage != null)
                && group.getSize() > 4 && !group.waitingForBuild) {
            if (group.getSize() > 100) {

                if (stage == null) {
                    actionManager.addAction(Action.scale(group, group.getX(), group.getY(), 0.7));
                    scaleStage.put(group.id, 1);
                } else if (stage == 1) {
                    boolean actionAdded = actionManager.addAction(Action.rotate(group, group.getX(), group.getY(), PI / 6));
                    if (actionAdded) {
                        scaleStage.put(group.id, 2);
                    }
                } else if (stage == 2) {
                    boolean actionAdded = actionManager.addAction(Action.scale(group, group.getX(), group.getY(), 0.7));
                    if (actionAdded) {
                        scaleStage.remove(group.id);
                    }
                }

            } else {
                actionManager.addAction(Action.scale(group, group.getX(), group.getY(), 0.4));
                scaleStage.remove(group.id);
            }
            lastScale.put(group.id, world.getTickIndex());
            //System.out.println(group.type + " " + group.id + " " +  world.getTickIndex());
        }
    }

    private void checkHamburger() {
        List<Group> enemies = GroupManager.getMixedEnemyGroups(vehicleById, me.getId());
        enemies.sort((first, second) -> Integer.compare(second.getSize(), first.getSize()));
        if (enemies.size() > 0 && enemies.get(0).getSize() > 200) {
            opponentIsHamburger = true;
        } else {
            opponentIsHamburger = false;
        }
    }


    public enum Ownership {
        ANY,

        ALLY,

        ENEMY
    }

    /*private void visualise() {
        for (Vehicle vehicle : vehicleById.values()) {
            vc.livingUnit(vehicle.getX(), vehicle.getY(), vehicle.getRadius(), vehicle.getDurability(), vehicle.getMaxDurability(),
                    vehicle.getPlayerId() == me.getId() ? RewindClient.Side.OUR : RewindClient.Side.ENEMY, 0,
                    RewindClient.UnitType.valueOf(vehicle.getType().name()), vehicle.getRemainingAttackCooldownTicks(),
                            vehicle.getAttackCooldownTicks(), vehicle.isSelected());
        }
    }*/
}