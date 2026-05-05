package systems.diath.visotaris_opmod.discord.ipc;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.util.Locale;
import java.util.UUID;

public final class DiscordIpcClient implements Closeable {

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;

    private final String applicationId;
    private ByteChannel channel;

    public DiscordIpcClient(String applicationId) {
        this.applicationId = applicationId;
    }

    public synchronized void connect() throws DiscordIpcException {
        if (isOpen()) return;
        channel = openChannel();
        send(OP_HANDSHAKE, "{\"v\":1,\"client_id\":" + quote(applicationId) + "}");
        Frame frame = readFrame();
        if (frame.opcode == OP_CLOSE) {
            closeQuietly();
            throw new DiscordIpcException("Discord hat den Handshake geschlossen: " + frame.payload);
        }
    }

    public synchronized void setActivity(String activityJson) throws DiscordIpcException {
        ensureOpen();
        String payload = "{"
            + "\"cmd\":\"SET_ACTIVITY\","
            + "\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":" + activityJson + "},"
            + "\"nonce\":" + quote(UUID.randomUUID().toString())
            + "}";
        send(OP_FRAME, payload);
        drainResponse();
    }

    public synchronized void clearActivity() throws DiscordIpcException {
        ensureOpen();
        String payload = "{"
            + "\"cmd\":\"SET_ACTIVITY\","
            + "\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":null},"
            + "\"nonce\":" + quote(UUID.randomUUID().toString())
            + "}";
        send(OP_FRAME, payload);
        drainResponse();
    }

    public synchronized boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public synchronized void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    private ByteChannel openChannel() throws DiscordIpcException {
        if (isWindows()) {
            return openWindowsPipe();
        }
        return openUnixPipe();
    }

    private static ByteChannel openUnixPipe() throws DiscordIpcException {
        Path pipe = DiscordPipeLocator.findUnixPipe();
        try {
            SocketChannel socket = SocketChannel.open(StandardProtocolFamily.UNIX);
            socket.connect(UnixDomainSocketAddress.of(pipe));
            return socket;
        } catch (IOException e) {
            throw new DiscordIpcException("Discord IPC Pipe konnte nicht geöffnet werden: " + pipe, e);
        }
    }

    private static ByteChannel openWindowsPipe() throws DiscordIpcException {
        IOException last = null;
        for (int i = 0; i < DiscordPipeLocator.maxPipeCount(); i++) {
            Path path = Path.of(DiscordPipeLocator.windowsPipeName(i));
            try {
                return FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            } catch (IOException e) {
                last = e;
            }
        }
        throw new DiscordIpcException("keine Discord IPC Named Pipe gefunden", last);
    }

    private void send(int opcode, String payload) throws DiscordIpcException {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(body.length);
        header.flip();
        writeFully(header);
        writeFully(ByteBuffer.wrap(body));
    }

    private Frame readFrame() throws DiscordIpcException {
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        readFully(header);
        header.flip();
        int opcode = header.getInt();
        int length = header.getInt();
        if (length < 0 || length > 1024 * 1024) {
            throw new DiscordIpcException("ungültige Discord IPC Paketlänge: " + length);
        }
        ByteBuffer body = ByteBuffer.allocate(length);
        readFully(body);
        body.flip();
        return new Frame(opcode, StandardCharsets.UTF_8.decode(body).toString());
    }

    private void drainResponse() throws DiscordIpcException {
        Frame frame = readFrame();
        if (frame.opcode == OP_CLOSE) {
            throw new DiscordIpcException("Discord hat die Verbindung geschlossen: " + frame.payload);
        }
        if (frame.opcode == OP_PING) {
            send(OP_PONG, frame.payload);
        }
    }

    private void writeFully(ByteBuffer buffer) throws DiscordIpcException {
        try {
            while (buffer.hasRemaining()) {
                if (channel.write(buffer) < 0) {
                    throw new DiscordIpcException("Discord IPC Verbindung wurde geschlossen");
                }
            }
        } catch (IOException e) {
            throw new DiscordIpcException("Discord IPC Schreibfehler", e);
        }
    }

    private void readFully(ByteBuffer buffer) throws DiscordIpcException {
        try {
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    throw new DiscordIpcException("Discord IPC Verbindung wurde geschlossen");
                }
            }
        } catch (IOException e) {
            throw new DiscordIpcException("Discord IPC Lesefehler", e);
        }
    }

    private void ensureOpen() throws DiscordIpcException {
        if (!isOpen()) {
            throw new DiscordIpcException("Discord IPC ist nicht verbunden");
        }
    }

    private void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
            // Best effort.
        }
    }

    public static String quote(String value) {
        if (value == null) return "\"\"";
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private record Frame(int opcode, String payload) {}
}
