package model;

public final class PlayerContext {
    private final Player player;
    private final World world;

    public PlayerContext(Player player, World world) {
        this.player = player;
        this.world = world;
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }
}
