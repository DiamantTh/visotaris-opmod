package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionIconCacheTest {
    @TempDir Path directory;

    @Test void identicalUrlsShareAsyncDownloadAndPersistNormalizedIcon() throws Exception {
        byte[] png = validPng();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger downloads = new AtomicInteger();
        AuctionIconCache cache = new AuctionIconCache(directory, url -> {
            downloads.incrementAndGet(); started.countDown(); release.await(2, TimeUnit.SECONDS); return png;
        });
        try {
            var first = cache.request("https://cdn.opsucht.net/item.png");
            assertTrue(started.await(2, TimeUnit.SECONDS));
            var second = cache.request("https://cdn.opsucht.net/item.png");
            assertSame(first, second);
            release.countDown();
            assertNotNull(first.get(2, TimeUnit.SECONDS));
            assertEquals(1, downloads.get());
            assertTrue(cache.getCached("https://cdn.opsucht.net/item.png").length > 0);
        } finally { release.countDown(); cache.close(); }

        try (AuctionIconCache reopened = new AuctionIconCache(directory, url -> { fail("disk cache should avoid download"); return null; })) {
            assertNotNull(reopened.getCached("https://cdn.opsucht.net/item.png"));
        }
    }

    @Test void invalidRemoteImagesFailSafelyAndUrlsMustUseHttps() throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        try (AuctionIconCache cache = new AuctionIconCache(directory, url -> { downloads.incrementAndGet(); return "not an image".getBytes(); })) {
            assertNull(cache.request("https://cdn.opsucht.net/bad.png").get(2, TimeUnit.SECONDS));
            assertNull(cache.getCached("https://cdn.opsucht.net/bad.png"));
            assertEquals(1, downloads.get());
            assertNull(AuctionIconCache.normalizeUrl("http://cdn.opsucht.net/item.png"));
            assertNull(AuctionIconCache.normalizeUrl("https://localhost/item.png"));
            assertNull(AuctionIconCache.normalizeUrl("https://evil.mc-api.io/item.png"));
            assertNotNull(AuctionIconCache.normalizeUrl("https://img.mc-api.io/item.png"));
            assertNotNull(AuctionIconCache.normalizeUrl("/icons/item.png"));
        }
    }

    private static byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 1, 0xFF64B5F6);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}
