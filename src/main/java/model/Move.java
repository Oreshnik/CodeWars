package model;

/**
 * Стратегия игрока может управлять юнитами посредством установки свойств объекта данного класса.
 */
public class Move {
    private ActionType action;

    private int group;

    private double left;
    private double top;
    private double right;
    private double bottom;

    private double x;
    private double y;
    private double angle;
    private double factor;

    private double maxSpeed;
    private double maxAngularSpeed;

    private VehicleType vehicleType;

    private long facilityId = -1L;
    private long vehicleId = -1L;

    /**
     * @return Возвращает текущее действие игрока.
     */
    public ActionType getAction() {
        return action;
    }

    /**
     * Устанавливает действие игрока.
     */
    public void setAction(ActionType action) {
        this.action = action;
    }

    /**
     * @return Возвращает текущую группу юнитов.
     */
    public int getGroup() {
        return group;
    }

    /**
     * Устанавливает группу юнитов для различных действий.
     * <p>
     * Является опциональным параметром для действий {@code ActionType.CLEAR_AND_SELECT},
     * {@code ActionType.ADD_TO_SELECTION} и {@code ActionType.DESELECT}. Если для этих действий группа юнитов
     * установлена, то параметр {@code vehicleType}, а также параметры прямоугольной рамки {@code left}, {@code top},
     * {@code right} и {@code bottom} будут проигнорированы.
     * <p>
     * Является обязательным параметром для действий {@code ActionType.ASSIGN}, {@code ActionType.DISMISS} и
     * {@code ActionType.DISBAND}. Для действия {@code ActionType.DISBAND} является единственным учитываемым параметром.
     * <p>
     * Корректными значениями являются целые числа от {@code 1} до {@code game.maxUnitGroup} включительно.
     */
    public void setGroup(int group) {
        this.group = group;
    }

    /**
     * @return Возвращает текущую левую границу прямоугольной рамки, предназначенной для выделения юнитов.
     */
    public double getLeft() {
        return left;
    }

    /**
     * @return Устанавливает левую границу прямоугольной рамки для выделения юнитов.
     * <p>
     * Является обязательным параметром для действий {@code ActionType.CLEAR_AND_SELECT},
     * {@code ActionType.ADD_TO_SELECTION} и {@code ActionType.DESELECT}, если не установлена группа юнитов.
     * В противном случае граница будет проигнорирована.
     * <p>
     * Корректными значениями являются вещественные числа от {@code 0.0} до {@code right} включительно.
     */
    public void setLeft(double left) {
        this.left = left;
    }

    /**
     * @return Возвращает текущую верхнюю границу прямоугольной рамки, предназначенной для выделения юнитов.
     */
    public double getTop() {
        return top;
    }

    /**
     * @return Устанавливает верхнюю границу прямоугольной рамки для выделения юнитов.
     * <p>
     * Является обязательным параметром для действий {@code ActionType.CLEAR_AND_SELECT},
     * {@code ActionType.ADD_TO_SELECTION} и {@code ActionType.DESELECT}, если не установлена группа юнитов.
     * В противном случае граница будет проигнорирована.
     * <p>
     * Корректными значениями являются вещественные числа от {@code 0.0} до {@code bottom} включительно.
     */
    public void setTop(double top) {
        this.top = top;
    }

    /**
     * @return Возвращает текущую правую границу прямоугольной рамки, предназначенной для выделения юнитов.
     */
    public double getRight() {
        return right;
    }

    /**
     * @return Устанавливает правую границу прямоугольной рамки для выделения юнитов.
     * <p>
     * Является обязательным параметром для действий {@code ActionType.CLEAR_AND_SELECT},
     * {@code ActionType.ADD_TO_SELECTION} и {@code ActionType.DESELECT}, если не установлена группа юнитов.
     * В противном случае граница будет проигнорирована.
     * <p>
     * Корректными значениями являются вещественные числа от {@code left} до {@code game.worldWidth} включительно.
     */
    public void setRight(double right) {
        this.right = right;
    }

    /**
     * @return Возвращает текущую нижнюю границу прямоугольной рамки, предназначенной для выделения юнитов.
     */
    public double getBottom() {
        return bottom;
    }

    /**
     * @return Устанавливает нижнюю границу прямоугольной рамки для выделения юнитов.
     * <p>
     * Является обязательным параметром для действий {@code ActionType.CLEAR_AND_SELECT},
     * {@code ActionType.ADD_TO_SELECTION} и {@code ActionType.DESELECT}, если не установлена группа юнитов.
     * В противном случае граница будет проигнорирована.
     * <p>
     * Корректными значениями являются вещественные числа от {@code top} до {@code game.worldHeight} включительно.
     */
    public void setBottom(double bottom) {
        this.bottom = bottom;
    }

    /**
     * @return Возвращает текущую абсциссу точки или вектора.
     */
    public double getX() {
        return x;
    }

    /**
     * Устанавливает абсциссу точки или вектора.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.MOVE} и задаёт целевую величину смещения юнитов
     * вдоль оси абсцисс.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.ROTATE} и задаёт абсциссу точки, относительно
     * которой необходимо совершить поворот.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.SCALE} и задаёт абсциссу точки, относительно
     * которой необходимо совершить масштабирование.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.TACTICAL_NUCLEAR_STRIKE} и задаёт абсциссу цели
     * тактического ядерного удара.
     * <p>
     * Корректными значениями для действия {@code ActionType.MOVE} являются вещественные числа от
     * {@code -game.worldWidth} до {@code game.worldWidth} включительно. Корректными значениями для действий
     * {@code ActionType.ROTATE} и {@code ActionType.SCALE} являются вещественные числа от {@code -game.worldWidth} до
     * {@code 2.0 * game.worldWidth} включительно. Корректными значениями для действия
     * {@code ActionType.TACTICAL_NUCLEAR_STRIKE} являются вещественные числа от {@code 0.0} до {@code game.worldWidth}
     * включительно.
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return Возвращает текущую ординату точки или вектора.
     */
    public double getY() {
        return y;
    }

    /**
     * Устанавливает ординату точки или вектора.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.MOVE} и задаёт целевую величину смещения юнитов
     * вдоль оси ординат.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.ROTATE} и задаёт ординату точки, относительно
     * которой необходимо совершить поворот.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.SCALE} и задаёт ординату точки, относительно
     * которой необходимо совершить масштабирование.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.TACTICAL_NUCLEAR_STRIKE} и задаёт ординату цели
     * тактического ядерного удара.
     * <p>
     * Корректными значениями для действия {@code ActionType.MOVE} являются вещественные числа от
     * {@code -game.worldHeight} до {@code game.worldHeight} включительно. Корректными значениями для действий
     * {@code ActionType.ROTATE} и {@code ActionType.SCALE} являются вещественные числа от {@code -game.worldHeight} до
     * {@code 2.0 * game.worldHeight} включительно. Корректными значениями для действия
     * {@code ActionType.TACTICAL_NUCLEAR_STRIKE} являются вещественные числа от {@code 0.0} до {@code game.worldHeight}
     * включительно.
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * @return Возвращает текущий угол поворота.
     */
    public double getAngle() {
        return angle;
    }

    /**
     * Задаёт угол поворота.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.ROTATE} и задаёт угол поворота относительно точки
     * ({@code x}, {@code y}). Положительные значения соответствуют повороту по часовой стрелке.
     * <p>
     * Корректными значениями являются вещественные числа от {@code -PI} до {@code PI} включительно.
     */
    public void setAngle(double angle) {
        this.angle = angle;
    }

    /**
     * @return Возвращает текущий коэффициент масштабирования.
     */
    public double getFactor() {
        return factor;
    }

    /**
     * Задаёт коэффициент масштабирования.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.SCALE} и задаёт коэффициент масштабирования
     * формации юнитов относительно точки ({@code x}, {@code y}). При значениях коэффициента больше 1.0 происходит
     * расширение формации, при значениях меньше 1.0 --- сжатие.
     * <p>
     * Корректными значениями являются вещественные числа от {@code 0.1} до {@code 10.0} включительно.
     */
    public void setFactor(double factor) {
        this.factor = factor;
    }

    /**
     * @return Возвращает текущее ограничение линейной скорости.
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Устанавливает абсолютное ограничение линейной скорости.
     * <p>
     * Является опциональным параметром для действий {@code ActionType.MOVE}, {@code ActionType.ROTATE} и
     * {@code ActionType.SCALE}. Если для действия {@code ActionType.ROTATE} установлено ограничение скорости поворота,
     * то этот параметр будет проигнорирован.
     * <p>
     * Корректными значениями являются вещественные неотрицательные числа. При этом, {@code 0.0} означает, что
     * ограничение отсутствует.
     */
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    /**
     * @return Возвращает текущее абсолютное ограничение скорости поворота.
     */
    public double getMaxAngularSpeed() {
        return maxAngularSpeed;
    }

    /**
     * Устанавливает абсолютное ограничение скорости поворота в радианах за тик.
     * <p>
     * Является опциональным параметром для действия {@code ActionType.ROTATE}. Если для этого действия установлено
     * ограничение скорости поворота, то параметр {@code maxSpeed} будет проигнорирован.
     * <p>
     * Корректными значениями являются вещественные числа в интервале от {@code 0.0} до {@code PI} включительно. При
     * этом, {@code 0.0} означает, что ограничение отсутствует.
     */
    public void setMaxAngularSpeed(double maxAngularSpeed) {
        this.maxAngularSpeed = maxAngularSpeed;
    }

    /**
     * @return Возвращает текущий тип техники.
     */
    public VehicleType getVehicleType() {
        return vehicleType;
    }

    /**
     * Устанавливает тип техники.
     * <p>
     * Является опциональным параметром для действий {@code ActionType.CLEAR_AND_SELECT},
     * {@code ActionType.ADD_TO_SELECTION} и {@code ActionType.DESELECT}.
     * Указанные действия будут применены только к технике выбранного типа.
     * Параметр будет проигнорирован, если установлена группа юнитов.
     * <p>
     * Является опциональным параметром для действия {@code ActionType.SETUP_VEHICLE_PRODUCTION}.
     * Завод будет настроен на производство техники данного типа. При этом, прогресс производства будет обнулён.
     * Если данный параметр не установлен, то производство техники на заводе будет остановлено.
     */
    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    /**
     * @return Возвращает текущий идентификатор сооружения.
     */
    public long getFacilityId() {
        return facilityId;
    }

    /**
     * Устанавливает идентификатор сооружения.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.SETUP_VEHICLE_PRODUCTION}.
     * Если сооружение с данным идентификатором отсутствует в игре, не является заводом по производству техники
     * ({@code FacilityType.VEHICLE_FACTORY}) или принадлежит другому игроку, то действие будет проигнорировано.
     */
    public void setFacilityId(long facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * @return Возвращает текущий идентификатор техники.
     */
    public long getVehicleId() {
        return vehicleId;
    }

    /**
     * Устанавливает идентификатор техники.
     * <p>
     * Является обязательным параметром для действия {@code ActionType.TACTICAL_NUCLEAR_STRIKE}. Если юнит с данным
     * идентификатором отсутствует в игре, принадлежит другому игроку или цель удара находится вне зоны видимости этого
     * юнита, то действие будет проигнорировано.
     */
    public void setVehicleId(long vehicleId) {
        this.vehicleId = vehicleId;
    }
}
