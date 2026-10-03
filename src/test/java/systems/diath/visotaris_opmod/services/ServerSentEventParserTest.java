package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerSentEventParserTest {
    @Test void parsesIdEventAndMultilineDataWhileIgnoringPingComments() {
        ServerSentEventParser parser = new ServerSentEventParser();
        assertTrue(parser.accept(":ping").isEmpty());
        assertTrue(parser.accept("id: 42").isEmpty());
        assertTrue(parser.accept("event: auction.updated").isEmpty());
        assertTrue(parser.accept("data: {\"uid\":").isEmpty());
        assertTrue(parser.accept(": another keepalive").isEmpty());
        assertTrue(parser.accept("data: \"auction-1\"}").isEmpty());
        ServerSentEventParser.Message message = parser.accept("").orElseThrow();
        assertEquals("42", message.id());
        assertEquals("auction.updated", message.event());
        assertEquals("{\"uid\":\n\"auction-1\"}", message.data());
        assertTrue(parser.accept("").isEmpty());

        parser.accept("event: auction.bid_placed");
        parser.accept("data: {\"uid\":\"auction-1\"}");
        ServerSentEventParser.Message resumed = parser.accept("").orElseThrow();
        assertEquals("42", resumed.id(), "Last-Event-ID remains in effect when a later event omits id:");
        assertEquals("auction.bid_placed", resumed.event());
    }
}
