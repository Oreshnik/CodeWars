package model;

/**
 * Содержит данные о текущем состоянии игрока.
 */
public class Player {
    private final long id;
    private final boolean me;
    private final boolean strategyCrashed;
    private final int score;
    private final int remainingActionCooldownTicks;
    private final int remainingNuclearStrikeCooldownTicks;
    private final long nextNuclearStrikeVehicleId;
    private final int nextNuclearStrikeTickIndex;
    private final double nextNuclearStrikeX;
    private final double nextNuclearStrikeY;

    public Player(
            long id, boolean me, boolean strategyCrashed, int score, int remainingActionCooldownTicks,
            int remainingNuclearStrikeCooldownTicks, long nextNuclearStrikeVehicleId, int nextNuclearStrikeTickIndex,
            double nextNuclearStrikeX, double nextNuclearStrikeY) {
        this.id = id;
        this.me = me;
        this.strategyCrashed = strategyCrashed;
        this.score = score;
        this.remainingActionCooldownTicks = remainingActionCooldownTicks;
        this.remainingNuclearStrikeCooldownTicks = remainingNuclearStrikeCooldownTicks;
        this.nextNuclearStrikeVehicleId = nextNuclearStrikeVehicleId;
        this.nextNuclearStrikeTickIndex = nextNuclearStrikeTickIndex;
        this.nextNuclearStrikeX = nextNuclearStrikeX;
        this.nextNuclearStrikeY = nextNuclearStrikeY;
    }

    /**
     * @return Возвращает уникальный идентификатор игрока.
     */
    public long getId() {
        return id;
    }

    /**
     * @return Возвращает {@code true} в том и только в том случае, если этот игрок ваш.
     */
    public boolean isMe() {
        return me;
    }

    /**
     * @return Возвращает специальный флаг --- показатель того, что стратегия игрока <<упала>>.
     * Более подробную информацию можно найти в документации к игре.
     */
    public boolean isStrategyCrashed() {
        return strategyCrashed;
    }

    /**
     * @return Возвращает количество баллов, набранное игроком.
     */
    public int getScore() {
        return score;
    }

    /**
     * @return Возвращает количество тиков, оставшееся до любого следующего действия.
     * Если значение равно {@code 0}, игрок может совершить действие в данный тик.
     */
    public int getRemainingActionCooldownTicks() {
        return remainingActionCooldownTicks;
    }

    /**
     * @return Возвращает количество тиков, оставшееся до следующего тактического ядерного удара.
     * Если значение равно {@code 0}, игрок может запросить удар в данный тик.
     */
    public int getRemainingNuclearStrikeCooldownTicks() {
        return remainingNuclearStrikeCooldownTicks;
    }

    /**
     * @return Возвращает идентификатор техники, осуществляющей наведение ядерного удара на цель или {@code -1}.
     */
    public long getNextNuclearStrikeVehicleId() {
        return nextNuclearStrikeVehicleId;
    }

    /**
     * @return Возвращает тик нанесения следующего ядерного удара или {@code -1}.
     */
    public int getNextNuclearStrikeTickIndex() {
        return nextNuclearStrikeTickIndex;
    }

    /**
     * @return Возвращает абсциссу цели следующего ядерного удара или {@code -1.0}.
     */
    public double getNextNuclearStrikeX() {
        return nextNuclearStrikeX;
    }

    /**
     * @return Возвращает ординату цели следующего ядерного удара или {@code -1.0}.
     */
    public double getNextNuclearStrikeY() {
        return nextNuclearStrikeY;
    }
}
