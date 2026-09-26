package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceAlertNotificationQueueTest {
    private static PriceAlertService.Event event(String id, long at) {
        return new PriceAlertService.Event(id, "diamond", "BUY_ABOVE", 100, 120, at, "HUD");
    }

    @Test void showsOneAtATimeAndBoundsPendingNotifications() {
        PriceAlertNotificationQueue queue = new PriceAlertNotificationQueue();
        queue.enqueue(event("first", 1_000));
        assertEquals("first", queue.current(1_000).id());
        queue.enqueue(event("second", 1_000));
        queue.enqueue(event("third", 1_000));
        queue.enqueue(event("fourth", 1_000));
        queue.enqueue(event("fifth", 1_000));
        assertEquals(3, queue.pendingCount());
        assertEquals("third", queue.current(5_000).id());
        assertEquals("fourth", queue.current(9_000).id());
        queue.clear();
        assertNull(queue.current(9_001));
    }

    @Test void dropsExpiredNotifications() {
        PriceAlertNotificationQueue queue = new PriceAlertNotificationQueue();
        queue.enqueue(event("old", 1_000));
        assertNull(queue.current(32_000));
    }
}
