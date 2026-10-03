package systems.diath.visotaris_opmod.api;

import okhttp3.Request;
import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.VisotarisConst;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpAcceptHeaderTest {
    @Test void explicitEventStreamAcceptSurvivesCommonHttpDefaults() {
        Request sse = new Request.Builder().url("https://api.opsucht.net/auctions/stream")
            .header("Accept", "text/event-stream").build();
        Request prepared = VisotarisConst.applyDefaultHeaders(sse, "Visotaris-Test");
        assertEquals("text/event-stream", prepared.header("Accept"));
        assertEquals("Visotaris-Test", prepared.header("User-Agent"));
    }

    @Test void jsonRequestReceivesDefaultAcceptHeader() {
        Request json = new Request.Builder().url("https://api.opsucht.net/auctions/active").build();
        assertEquals("application/json", VisotarisConst.applyDefaultHeaders(json, "Visotaris-Test").header("Accept"));
    }
}
