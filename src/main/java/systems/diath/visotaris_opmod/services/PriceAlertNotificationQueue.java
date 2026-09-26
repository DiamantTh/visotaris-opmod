package systems.diath.visotaris_opmod.services;

import java.util.ArrayDeque;
import java.util.Deque;

/** Bounded, short-lived handoff from market synchronization to the client HUD. */
public final class PriceAlertNotificationQueue {
    public static final long DISPLAY_MS = 4_000L;
    private static final long MAX_AGE_MS = 30_000L;
    private static final int MAX_PENDING = 3;

    private final Deque<PriceAlertService.Event> pending = new ArrayDeque<>();
    private PriceAlertService.Event current;
    private long shownAtMs;

    public synchronized void enqueue(PriceAlertService.Event event) {
        if (event == null) return;
        if (pending.size() == MAX_PENDING) pending.removeFirst();
        pending.addLast(event);
    }

    public synchronized PriceAlertService.Event current(long nowMs) {
        if (current != null && nowMs - shownAtMs >= DISPLAY_MS) current = null;
        while (current == null && !pending.isEmpty()) {
            PriceAlertService.Event candidate = pending.removeFirst();
            if (nowMs - candidate.timestampMs() <= MAX_AGE_MS) {
                current = candidate;
                shownAtMs = nowMs;
            }
        }
        return current;
    }

    public synchronized int pendingCount() { return pending.size(); }

    public synchronized void clear() {
        pending.clear();
        current = null;
    }
}
