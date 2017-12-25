import model.*;

import java.io.IOException;

public final class Runner {
    private final RemoteProcessClient remoteProcessClient;
    private final String token;

    public static void main(String[] args) throws IOException {
        new Runner(args.length == 3 ? args : new String[] {"127.0.0.1", "31001", "0000000000000000"}).run();
    }

    private Runner(String[] args) throws IOException {
        remoteProcessClient = new RemoteProcessClient(args[0], Integer.parseInt(args[1]));
        token = args[2];
    }

    @SuppressWarnings("WeakerAccess")
    public void run() throws IOException {
        try {
            remoteProcessClient.writeTokenMessage(token);
            remoteProcessClient.writeProtocolVersionMessage();
            remoteProcessClient.readTeamSizeMessage();
            Game game = remoteProcessClient.readGameContextMessage();

            Strategy strategy = new MyStrategy();

            PlayerContext playerContext;

            while ((playerContext = remoteProcessClient.readPlayerContextMessage()) != null) {
                Player player = playerContext.getPlayer();
                if (player == null) {
                    break;
                }

                Move move = new Move();
                strategy.move(player, playerContext.getWorld(), game, move);

                remoteProcessClient.writeMoveMessage(move);
            }
        } finally {
            remoteProcessClient.close();
        }
    }
}
