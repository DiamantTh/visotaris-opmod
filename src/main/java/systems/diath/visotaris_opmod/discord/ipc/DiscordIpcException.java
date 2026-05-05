package systems.diath.visotaris_opmod.discord.ipc;

public final class DiscordIpcException extends Exception {
    public DiscordIpcException(String message) {
        super(message);
    }

    public DiscordIpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
