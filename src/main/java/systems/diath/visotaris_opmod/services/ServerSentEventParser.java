package systems.diath.visotaris_opmod.services;

import java.util.Optional;

/** Minimal UTF-8 SSE framing parser. Comments (including keepalive pings) are ignored. */
public final class ServerSentEventParser {
    public record Message(String id, String event, String data) { }

    // SSE keeps the last event ID across dispatches until another `id:` field arrives.
    private String lastEventId;
    private String event;
    private final StringBuilder data = new StringBuilder();
    private boolean hasDataField;

    public Optional<Message> accept(String line) {
        if (line == null) return Optional.empty();
        if (line.isEmpty()) {
            if (!hasDataField) {
                event = null;
                data.setLength(0);
                return Optional.empty();
            }
            Message message = new Message(lastEventId, event, data.toString());
            event = null;
            data.setLength(0);
            hasDataField = false;
            return Optional.of(message);
        }
        if (line.charAt(0) == ':') return Optional.empty();

        int separator = line.indexOf(':');
        String field = separator < 0 ? line : line.substring(0, separator);
        String value = separator < 0 ? "" : line.substring(separator + 1);
        if (value.startsWith(" ")) value = value.substring(1);
        switch (field) {
            case "id" -> lastEventId = value;
            case "event" -> event = value;
            case "data" -> {
                if (hasDataField) data.append('\n');
                data.append(value);
                hasDataField = true;
            }
            default -> { /* retry and extension fields are not needed by Visotaris */ }
        }
        return Optional.empty();
    }
}
