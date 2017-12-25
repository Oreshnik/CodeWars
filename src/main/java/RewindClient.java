import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;


/**
 * Java client for Rewind viewer. Put this file to the same default package where Strategy/MyStrategy/Runner and other
 * files are extracted.
 * Sample usage: look at main method
 */
public class RewindClient {

    private final Socket socket;
    private final OutputStream outputStream;

    public enum Side {
        OUR(-1),
        NEUTRAL(0),
        ENEMY(1);
        final int side;

        Side(int side) {
            this.side = side;
        }
    }

    public enum AreaType {
        UNKNOWN(0),
        FOREST(1),
        SWAMP(2),
        RAIN(3),
        CLOUD(4),;
        final int areaType;

        AreaType(int areaType) {
            this.areaType = areaType;
        }
    }

    public enum FacilityType {
        CONTROL_CENTER(0),
        VEHICLE_FACTORY(1);
        final int facilityType;

        FacilityType(int facilityType) {
            this.facilityType = facilityType;
        }
    }

    public enum UnitType {
        UNKNOWN(0),
        TANK(1),
        IFV(2),
        ARRV(3),
        HELICOPTER(4),
        FIGHTER(5);
        final int unitType;

        UnitType(int unitType) {
            this.unitType = unitType;
        }
    }

    /**
     * Should be send on end of move function all turn primitives can be rendered after that point
     */
    void endFrame() {
        send("{\"type\":\"end\"}");
    }

    void circle(double x, double y, double r, Color color, int layer) {
        send(String.format("{\"type\": \"circle\", \"x\": %f, \"y\": %f, \"r\": %f, \"color\": %d, \"layer\": %d}", x, y, r, color.getRGB(), layer));
    }

    void rect(double x1, double y1, double x2, double y2, Color color, int layer) {
        send(String.format("{\"type\": \"rectangle\", \"x1\": %f, \"y1\": %f, \"x2\": %f, \"y2\": %f, \"color\": %d, \"layer\": %d}", x1, y1, x2, y2, color.getRGB(), layer));
    }

    void line(double x1, double y1, double x2, double y2, Color color, int layer) {
        send(String.format("{\"type\": \"line\", \"x1\": %f, \"y1\": %f, \"x2\": %f, \"y2\": %f, \"color\": %d, \"layer\": %d}", x1, y1, x2, y2, color.getRGB(), layer));
    }

    void popup(double x, double y, double r, String text) {
        send(String.format("{\"type\": \"popup\", \"x\": %f, \"y\": %f, \"r\": %f, \"text\": \"%s \"}", x, y, r, text));
    }

    void livingUnit(double x, double y, double r, int hp, int maxHp,
                    Side side) {
        livingUnit(x, y, r, hp, maxHp, side, 0, UnitType.UNKNOWN, 0, 0, false);
    }

    void areaDescription(int cellX, int cellY, AreaType areaType) {
        send(String.format("{\"type\": \"area\", \"x\": %d, \"y\": %d, \"area_type\": %d}", cellX, cellY, areaType.areaType));
    }

    /**
     * Facility - rectangle with texture and progress bars
     * @param cell_x - x cell of top left facility part
     * @param cell_y - y cell of top left facility part
     * @param type - type of facility
     * @param side - enemy, ally or neutral
     * @param production - current production progress, set to 0 if no production
     * @param max_production - maximum production progress, used together with `production`
     * @param capture - current capture progress, should be in range [-max_capture, max_capture],
     * where negative values mean that facility is capturing by enemy
     * @param max_capture - maximum capture progress, used together with `capture`
     */

    void facility(int cell_x, int cell_y, FacilityType type, Side side, int production, int max_production, int capture, int max_capture) {
        send(String.format("{\"type\": \"facility\", \"x\": %d, \"y\": %d, \"facility_type\": %d, \"enemy\": %d, \"production\": %d, \"max_production\": %d, \"capture\": %d, \"max_capture\": %d}",
                cell_x, cell_y, type.facilityType, side.side, production, max_production, capture, max_capture));
    }

    void close() {
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pass arbitrary user message to be stored in frame
     * Message content displayed in separate window inside viewer
     * Can be used several times per frame
     *
     * @param msg .
     */
    public void message(String msg) {
        String s = "{\"type\": \"message\", \"message\" : \"" + msg + " \"}";
        send(s);
    }

    /**
     * @param x           - x pos of the unit
     * @param y           - y pos of the unit
     * @param r           - radius of the unit
     * @param hp          - current health
     * @param maxHp       - max possible health
     * @param side        - owner of the unit
     * @param course      - rotation of the unit - angle in radians [0, 2 * pi) counter clockwise
     * @param unitType    - id of unit type (see UnitType enum: https://github.com/kswaldemar/rewind-viewer/blob/master/src/viewer/Frame.h)
     * @param remCooldown -
     * @param maxCooldown -
     */
    void livingUnit(double x, double y, double r, int hp, int maxHp,
                    Side side, double course, UnitType unitType,
                    int remCooldown, int maxCooldown, boolean selected) {
        send(String.format(
                "{\"type\": \"unit\", \"x\": %f, \"y\": %f, \"r\": %f, \"hp\": %d, \"max_hp\": %d, \"enemy\": %d, \"unit_type\":%d, \"course\": %.3f," +
                        "\"rem_cooldown\":%d, \"cooldown\":%d, \"selected\":%d }",
                x, y, r, hp, maxHp, side.side, unitType.unitType, course, remCooldown, maxCooldown, selected ? 1 : 0));
    }

    public RewindClient(String host, int port) {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RewindClient() {
        this("127.0.0.1", 9111);
    }

    private void send(String buf) {
        try {
            outputStream.write(buf.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Example of use
     *
     * @param args .
     */
    public static void main(String[] args) {
        // Double is formatted with a comma "," in some locales.
        // For json to work properly, should use dot instead.
        Locale.setDefault(Locale.US);

        RewindClient rc = new RewindClient();
        int worldSize = 1024;
        for (int x = 0; x < worldSize / 32; x++) {
            for (int y = 0; y < worldSize / 32; y++) {
                rc.areaDescription(x, y, AreaType.values()[(int) (Math.random() * AreaType.values().length)]);
            }
        }
        rc.message("Hello World");
        rc.endFrame();


        for (int i = 0; i < 2000; i++) {
            rc.message("Step " + i);
            for (int j = 0; j < 100; j++) {
                Color rndColor = new Color((int) (255 * 255 * 255 * Math.random()));


                double unixX = Math.random() * worldSize;
                double unitY = Math.random() * 32 + i;

                rc.livingUnit(
                        unixX,
                        unitY,
                        2 + Math.random() * 10,
                        10,
                        100,
                        Side.values()[(int) (Side.values().length * Math.random())],
                        Math.PI * Math.random(),
                        UnitType.values()[(int) (UnitType.values().length * Math.random())],
                        (int) (60 * Math.random()),
                        60,
                        Math.random() > 0.8);

                rc.line(unixX, unitY, Math.random() * worldSize, Math.random() * worldSize, rndColor, 3);

                double circleX = unixX + Math.random() * 40 - 20;
                double circleY = unitY + Math.random() * 40 - 20;

                rc.circle(circleX, circleY, 10 + Math.random() * 10, rndColor, 2);

                double rectX1 = circleX + Math.random() * 100;
                double rectY1 = circleY + Math.random() * 100;
                rc.rect(rectX1, rectY1, rectX1 + Math.random() * 40, rectY1 + Math.random() * 40, rndColor, 1);
            }
            rc.popup(128, 128, 28, "Hello!\\nThis is popup message!\\nabcdefg");
            rc.endFrame();
        }

        rc.message("Bye!");
        rc.endFrame();

        rc.close();
    }
}