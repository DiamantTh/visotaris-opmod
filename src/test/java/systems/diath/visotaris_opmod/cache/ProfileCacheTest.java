package systems.diath.visotaris_opmod.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCacheTest {
    private static final String UUID = "de27aa9bfd33440dfac2c6eee9bc371d";
    @TempDir Path directory;

    @Test void concurrentUuidLookupsShareOneAsyncResolutionAndPersistTheName() throws Exception {
        Path file = directory.resolve("players.json");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger minecraftLookups = new AtomicInteger();
        AtomicInteger fallbackLookups = new AtomicInteger();
        ProfileCache cache = new ProfileCache(file, uuid -> {
            minecraftLookups.incrementAndGet(); started.countDown(); release.await(2, TimeUnit.SECONDS); return "DiamondTh";
        }, uuid -> { fallbackLookups.incrementAndGet(); return "FallbackName"; });
        try {
            var first = cache.resolve(UUID);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            var second = cache.resolve("de27aa9b-fd33-440d-fac2-c6eee9bc371d");
            assertSame(first, second);
            release.countDown();
            assertEquals("DiamondTh", first.get(2, TimeUnit.SECONDS));
            assertEquals(1, minecraftLookups.get());
            assertEquals(0, fallbackLookups.get());
        } finally { release.countDown(); cache.close(); }

        try (ProfileCache reopened = new ProfileCache(file, uuid -> fail("persisted cache should be used"),
            uuid -> fail("persisted cache should avoid fallback"))) {
            assertEquals("DiamondTh", reopened.getDisplayName(UUID));
        }
    }

    @Test void unknownUuidIsShortenedAndNegativeResultsAreTemporarilyCached() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        try (ProfileCache cache = new ProfileCache(directory.resolve("negative.json"), uuid -> { lookups.incrementAndGet(); return null; },
            uuid -> { lookups.incrementAndGet(); return null; })) {
            assertEquals("de27aa9b…371d", ProfileCache.shortUuid(UUID));
            assertEquals("de27aa9b…371d", cache.resolve(UUID).get(2, TimeUnit.SECONDS));
            assertEquals("de27aa9b…371d", cache.resolve(UUID).get(2, TimeUnit.SECONDS));
            assertEquals(2, lookups.get(), "Minecraft and fallback lookup run once; negative cache blocks retries");
        }
    }
}
