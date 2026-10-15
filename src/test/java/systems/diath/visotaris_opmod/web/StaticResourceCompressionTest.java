package systems.diath.visotaris_opmod.web;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticResourceCompressionTest {
    private static final String SCRIPT = "assets/webui/static/app/historyCharts.js";

    @Test void prefersBrotliWhenAccepted() {
        assertEncoding("br", Set.of(SCRIPT + ".br"), StaticResourceEncoding.BROTLI);
    }

    @Test void usesZstandardWhenBrotliIsNotAccepted() {
        assertEncoding("zstd", Set.of(SCRIPT + ".zst"), StaticResourceEncoding.ZSTANDARD);
    }

    @Test void projectPreferenceChoosesBrotliRegardlessOfHeaderOrder() {
        Set<String> variants = Set.of(SCRIPT + ".br", SCRIPT + ".zst");
        assertEncoding("br, zstd", variants, StaticResourceEncoding.BROTLI);
        assertEncoding("zstd, br", variants, StaticResourceEncoding.BROTLI);
    }

    @Test void respectsRejectedEncodingsAndFallsBackToRaw() {
        assertEncoding("br;q=0, zstd", Set.of(SCRIPT + ".br", SCRIPT + ".zst"), StaticResourceEncoding.ZSTANDARD);
        assertEncoding("br;q=0, zstd;q=0", Set.of(SCRIPT + ".br", SCRIPT + ".zst"), StaticResourceEncoding.RAW);
    }

    @Test void rawIsUsedForMissingOrUnsupportedAcceptEncoding() {
        assertEncoding(null, Set.of(SCRIPT + ".br", SCRIPT + ".zst"), StaticResourceEncoding.RAW);
        assertEncoding("gzip", Set.of(SCRIPT + ".br", SCRIPT + ".zst"), StaticResourceEncoding.RAW);
        assertEncoding("*", Set.of(SCRIPT + ".br", SCRIPT + ".zst"), StaticResourceEncoding.BROTLI);
        assertEncoding("*;q=0", Set.of(SCRIPT + ".br", SCRIPT + ".zst"), StaticResourceEncoding.RAW);
        assertEncoding("gzip, br", Set.of(SCRIPT + ".br"), StaticResourceEncoding.BROTLI);
        assertEncoding("gzip, zstd", Set.of(SCRIPT + ".zst"), StaticResourceEncoding.ZSTANDARD);
    }

    @Test void fallsThroughWhenPreferredVariantIsMissing() {
        assertEncoding("br, zstd", Set.of(SCRIPT + ".zst"), StaticResourceEncoding.ZSTANDARD);
        assertEncoding("br", Set.of(), StaticResourceEncoding.RAW);
    }

    @Test void preservesLogicalPathForContentTypeSelection() {
        StaticResourceVariant variant = select("br", Set.of(SCRIPT + ".br"));
        assertEquals(SCRIPT, variant.getRequestedPath());
        assertEquals(SCRIPT + ".br", variant.getResourcePath());
        assertEquals("br", variant.getEncoding().getToken());
        assertTrue(StaticResourceCompression.INSTANCE.isCompressible(variant.getRequestedPath()));
        assertTrue(StaticResourceCompression.INSTANCE.requiresVary(variant.getRequestedPath()));
        assertFalse(StaticResourceCompression.INSTANCE.isCompressible("assets/webui/static/fonts/fira.woff2"));
        assertFalse(StaticResourceCompression.INSTANCE.requiresVary("assets/webui/static/fonts/fira.woff2"));
    }

    @Test void rejectsTraversalSegmentsBeforeClasspathLookup() {
        assertTrue(StaticResourceCompression.INSTANCE.isSafeStaticPath("app/historyCharts.js"));
        assertFalse(StaticResourceCompression.INSTANCE.isSafeStaticPath("../WebServer.kt"));
        assertFalse(StaticResourceCompression.INSTANCE.isSafeStaticPath("app/../WebServer.kt"));
        assertFalse(StaticResourceCompression.INSTANCE.isSafeStaticPath("app\\WebServer.kt"));
    }

    private static void assertEncoding(String header, Set<String> available, StaticResourceEncoding expected) {
        assertEquals(expected, select(header, available).getEncoding());
    }

    private static StaticResourceVariant select(String header, Set<String> available) {
        return StaticResourceCompression.INSTANCE.select(SCRIPT, header, available::contains);
    }
}
