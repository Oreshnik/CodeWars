import model.*;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class RemoteProcessClient implements Closeable {
    private static final int BUFFER_SIZE_BYTES = 1 << 20;
    private static final ByteOrder PROTOCOL_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int INTEGER_SIZE_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int LONG_SIZE_BYTES = Long.SIZE / Byte.SIZE;

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ByteArrayOutputStream outputStreamBuffer;

    private Player[] previousPlayers;
    private Facility[] previousFacilities;
    private TerrainType[][] terrainByCellXY;
    private WeatherType[][] weatherByCellXY;

    private final Map<Long, Player> previousPlayerById = new HashMap<>();
    private final Map<Long, Facility> previousFacilityById = new HashMap<>();

    public RemoteProcessClient(String host, int port) throws IOException {
        socket = new Socket();
        socket.setSendBufferSize(BUFFER_SIZE_BYTES);
        socket.setReceiveBufferSize(BUFFER_SIZE_BYTES);
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port));

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        outputStreamBuffer = new ByteArrayOutputStream(BUFFER_SIZE_BYTES);
    }

    public void writeTokenMessage(String token) throws IOException {
        writeEnum(MessageType.AUTHENTICATION_TOKEN);
        writeString(token);
        flush();
    }

    public void writeProtocolVersionMessage() throws IOException {
        writeEnum(MessageType.PROTOCOL_VERSION);
        writeInt(3);
        flush();
    }

    public void readTeamSizeMessage() throws IOException {
        ensureMessageType(readEnum(MessageType.class), MessageType.TEAM_SIZE);
        readInt();
    }

    public Game readGameContextMessage() throws IOException {
        ensureMessageType(readEnum(MessageType.class), MessageType.GAME_CONTEXT);
        if (!readBoolean()) {
            return null;
        }

        return new Game(
                readLong(), readInt(), readDouble(), readDouble(), readBoolean(), readInt(), readInt(), readInt(),
                readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readInt(), readDouble(), readDouble(), readDouble(),
                readDouble(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readDouble(),
                readDouble(), readDouble(), readDouble(), readInt(), readInt(), readInt(), readInt(), readInt(),
                readInt(), readInt(), readDouble(), readDouble(), readInt(), readInt(), readInt(), readDouble(),
                readDouble(), readInt(), readDouble(), readDouble(), readDouble(), readDouble(), readInt(), readInt(),
                readInt(), readInt(), readInt(), readInt(), readInt(), readDouble(), readDouble(), readDouble(),
                readDouble(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readDouble(),
                readDouble(), readDouble(), readDouble(), readInt(), readInt(), readDouble(), readDouble(), readInt()
        );
    }

    public PlayerContext readPlayerContextMessage() throws IOException {
        MessageType messageType = readEnum(MessageType.class);
        if (messageType == MessageType.GAME_OVER) {
            return null;
        }

        ensureMessageType(messageType, MessageType.PLAYER_CONTEXT);
        return readBoolean() ? new PlayerContext(readPlayer(), readWorld()) : null;
    }

    public void writeMoveMessage(Move move) throws IOException {
        writeEnum(MessageType.MOVE);

        if (move == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);

            writeEnum(move.getAction());
            writeInt(move.getGroup());
            writeDouble(move.getLeft());
            writeDouble(move.getTop());
            writeDouble(move.getRight());
            writeDouble(move.getBottom());
            writeDouble(move.getX());
            writeDouble(move.getY());
            writeDouble(move.getAngle());
            writeDouble(move.getFactor());
            writeDouble(move.getMaxSpeed());
            writeDouble(move.getMaxAngularSpeed());
            writeEnum(move.getVehicleType());
            writeLong(move.getFacilityId());
            writeLong(move.getVehicleId());
        }

        flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private World readWorld() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new World(
                readInt(), readInt(), readDouble(), readDouble(), readPlayers(), readVehicles(), readVehicleUpdates(),
                terrainByCellXY == null ? (terrainByCellXY = readEnumArray2D(TerrainType.class)) : terrainByCellXY,
                weatherByCellXY == null ? (weatherByCellXY = readEnumArray2D(WeatherType.class)) : weatherByCellXY,
                readFacilities()
        );
    }

    private Player[] readPlayers() throws IOException {
        Player[] players = readArray(Player.class, this::readPlayer);
        return players == null ? previousPlayers : (previousPlayers = players);
    }

    private Player readPlayer() throws IOException {
        byte flag = readByte();

        if (flag == 0) {
            return null;
        }

        if (flag == 127) {
            return previousPlayerById.get(readLong());
        }

        Player player = new Player(
                readLong(), readBoolean(), readBoolean(), readInt(), readInt(), readInt(), readLong(), readInt(),
                readDouble(), readDouble()
        );
        previousPlayerById.put(player.getId(), player);
        return player;
    }

    private Vehicle[] readVehicles() throws IOException {
        return readArray(Vehicle.class, this::readVehicle);
    }

    private Vehicle readVehicle() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Vehicle(
                readLong(), readDouble(), readDouble(), readDouble(), readLong(), readInt(), readInt(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readInt(),
                readInt(), readInt(), readInt(), readInt(), readInt(), readEnum(VehicleType.class), readBoolean(),
                readBoolean(), readIntArray()
        );
    }

    private Facility[] readFacilities() throws IOException {
        Facility[] facilities = readArray(Facility.class, this::readFacility);
        return facilities == null ? previousFacilities : (previousFacilities = facilities);
    }

    private Facility readFacility() throws IOException {
        byte flag = readByte();

        if (flag == 0) {
            return null;
        }

        if (flag == 127) {
            return previousFacilityById.get(readLong());
        }

        Facility facility = new Facility(
                readLong(), readEnum(FacilityType.class), readLong(), readDouble(), readDouble(), readDouble(),
                readEnum(VehicleType.class), readInt()
        );
        previousFacilityById.put(facility.getId(), facility);
        return facility;
    }

    private VehicleUpdate[] readVehicleUpdates() throws IOException {
        return readArray(VehicleUpdate.class, this::readVehicleUpdate);
    }

    private VehicleUpdate readVehicleUpdate() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new VehicleUpdate(
                readLong(), readDouble(), readDouble(), readInt(), readInt(), readBoolean(), readIntArray()
        );
    }

    private static void ensureMessageType(MessageType actualType, MessageType expectedType) {
        if (actualType != expectedType) {
            throw new IllegalArgumentException(String.format(
                    "Received wrong message [actual=%s, expected=%s].", actualType, expectedType
            ));
        }
    }

    private <E> E[] readArray(Class<E> elementClass, ElementReader<E> elementReader) throws IOException {
        int length = readInt();
        if (length < 0) {
            return null;
        }

        @SuppressWarnings("unchecked") E[] array = (E[]) Array.newInstance(elementClass, length);

        for (int i = 0; i < length; ++i) {
            array[i] = elementReader.read();
        }

        return array;
    }

    private <E> void writeArray(E[] array, ElementWriter<E> elementWriter) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            int length = array.length;
            writeInt(length);

            for (int i = 0; i < length; ++i) {
                elementWriter.write(array[i]);
            }
        }
    }

    private byte[] readByteArray(boolean nullable) throws IOException {
        int count = readInt();

        if (count <= 0) {
            return nullable && count < 0 ? null : EMPTY_BYTE_ARRAY;
        }

        return readBytes(count);
    }

    private void writeByteArray(byte[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            writeBytes(array);
        }
    }

    private <E extends Enum> E readEnum(Class<E> enumClass) throws IOException {
        byte ordinal = readByte();

        E[] values = enumClass.getEnumConstants();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    @SuppressWarnings("SubtractionInCompareTo")
    private <E extends Enum> E[] readEnumArray(Class<E> enumClass, int count) throws IOException {
        byte[] bytes = readBytes(count);
        @SuppressWarnings("unchecked") E[] array = (E[]) Array.newInstance(enumClass, count);

        E[] values = enumClass.getEnumConstants();
        int valueCount = values.length;

        for (int i = 0; i < count; ++i) {
            byte ordinal = bytes[i];

            if (ordinal >= 0 && ordinal < valueCount) {
                array[i] = values[ordinal];
            }
        }

        return array;
    }

    private <E extends Enum> E[] readEnumArray(Class<E> enumClass) throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        return readEnumArray(enumClass, count);
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum> E[][] readEnumArray2D(Class<E> enumClass) throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        E[][] array;
        try {
            array = (E[][]) Array.newInstance(Class.forName("[L" + enumClass.getName() + ';'), count);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Can't load array class for " + enumClass + '.', e);
        }

        for (int i = 0; i < count; ++i) {
            array[i] = readEnumArray(enumClass);
        }

        return array;
    }

    private <E extends Enum> void writeEnum(E value) throws IOException {
        writeByte(value == null ? -1 : value.ordinal());
    }

    private String readString() throws IOException {
        int length = readInt();
        if (length == -1) {
            return null;
        }

        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    private void writeString(String value) throws IOException {
        if (value == null) {
            writeInt(-1);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        writeInt(bytes.length);
        writeBytes(bytes);
    }

    private boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    private boolean[] readBooleanArray(int count) throws IOException {
        byte[] bytes = readBytes(count);
        boolean[] array = new boolean[count];

        for (int i = 0; i < count; ++i) {
            array[i] = bytes[i] != 0;
        }

        return array;
    }

    private boolean[] readBooleanArray() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        return readBooleanArray(count);
    }

    private boolean[][] readBooleanArray2D() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        boolean[][] array = new boolean[count][];

        for (int i = 0; i < count; ++i) {
            array[i] = readBooleanArray();
        }

        return array;
    }

    private void writeBoolean(boolean value) throws IOException {
        writeByte(value ? 1 : 0);
    }

    private int readInt() throws IOException {
        return ByteBuffer.wrap(readBytes(INTEGER_SIZE_BYTES)).order(PROTOCOL_BYTE_ORDER).getInt();
    }

    private int[] readIntArray(int count) throws IOException {
        byte[] bytes = readBytes(count * INTEGER_SIZE_BYTES);
        int[] array = new int[count];

        for (int i = 0; i < count; ++i) {
            array[i] = ByteBuffer.wrap(
                    bytes, i * INTEGER_SIZE_BYTES, INTEGER_SIZE_BYTES
            ).order(PROTOCOL_BYTE_ORDER).getInt();
        }

        return array;
    }

    private int[] readIntArray() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        return readIntArray(count);
    }

    private int[][] readIntArray2D() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        int[][] array = new int[count][];

        for (int i = 0; i < count; ++i) {
            array[i] = readIntArray();
        }

        return array;
    }

    private void writeInt(int value) throws IOException {
        writeBytes(ByteBuffer.allocate(INTEGER_SIZE_BYTES).order(PROTOCOL_BYTE_ORDER).putInt(value).array());
    }

    private long readLong() throws IOException {
        return ByteBuffer.wrap(readBytes(LONG_SIZE_BYTES)).order(PROTOCOL_BYTE_ORDER).getLong();
    }

    private void writeLong(long value) throws IOException {
        writeBytes(ByteBuffer.allocate(LONG_SIZE_BYTES).order(PROTOCOL_BYTE_ORDER).putLong(value).array());
    }

    private double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    private void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToLongBits(value));
    }

    private byte[] readBytes(int byteCount) throws IOException {
        byte[] bytes = new byte[byteCount];
        int offset = 0;
        int readByteCount;

        while (offset < byteCount && (readByteCount = inputStream.read(bytes, offset, byteCount - offset)) != -1) {
            offset += readByteCount;
        }

        if (offset != byteCount) {
            throw new IOException(String.format("Can't read %d bytes from input stream.", byteCount));
        }

        return bytes;
    }

    private void writeBytes(byte[] bytes) throws IOException {
        outputStreamBuffer.write(bytes);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private byte readByte() throws IOException {
        int value = inputStream.read();

        if (value == -1) {
            throw new IOException("Can't read a byte from input stream.");
        }

        return (byte) value;
    }

    private void writeByte(int value) throws IOException {
        try {
            outputStreamBuffer.write(value);
        } catch (RuntimeException e) {
            throw new IOException("Can't write a byte into output stream.", e);
        }
    }

    private void flush() throws IOException {
        outputStream.write(outputStreamBuffer.toByteArray());
        outputStreamBuffer.reset();
        outputStream.flush();
    }

    @SuppressWarnings("InterfaceNeverImplemented")
    private interface ElementReader<E> {
        E read() throws IOException;
    }

    @SuppressWarnings("InterfaceNeverImplemented")
    private interface ElementWriter<E> {
        void write(E element) throws IOException;
    }

    private enum MessageType {
        @SuppressWarnings("unused")
        UNKNOWN,
        GAME_OVER,
        AUTHENTICATION_TOKEN,
        TEAM_SIZE,
        PROTOCOL_VERSION,
        GAME_CONTEXT,
        PLAYER_CONTEXT,
        MOVE
    }
}
