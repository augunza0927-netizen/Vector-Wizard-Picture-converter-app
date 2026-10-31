package com.vectorconvert;

import com.vectorconvert.model.ColorMode;
import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.DetailLevel;
import com.vectorconvert.model.ImageItem;
import com.vectorconvert.service.BatchProcessingManager;
import com.vectorconvert.service.VectorizationService;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

//parallel
public class BatchProcessingManagerTest {

    private static VectorizationService vectorizationService;

    @BeforeAll
    public static void setUp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
        vectorizationService = new VectorizationService();
    }

    private WritableImage createSampleJavaFxImage(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        javafx.scene.image.PixelWriter writer = image.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, Color.WHITE);
            }
        }

        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (Math.hypot(x - centerX, y - centerY) <= radius) {
                    writer.setColor(x, y, Color.DODGERBLUE);
                }
            }
        }

        return image;
    }

    private ImageItem createTestItem(String name, int width, int height) {
        WritableImage sample = createSampleJavaFxImage(width, height);
        return new ImageItem(sample, name);
    }

    @Test
    @DisplayName("Verify constraint-triggered parallel processing rules")
    public void testParallelProcessingConstraints() {
        ConversionConfig config = new ConversionConfig();


        config.setDetailLevel(DetailLevel.MEDIUM);
        config.setColorMode(ColorMode.CUSTOM);
        config.setCustomColorCount(2);
        assertTrue(config.qualifiesForParallelProcessing(), "Medium + Custom (2 colors) must qualify for parallel processing");

        config.setCustomColorCount(5);
        assertTrue(config.qualifiesForParallelProcessing(), "Medium + Custom (5 colors) must qualify for parallel processing");


        config.setDetailLevel(DetailLevel.LOW);
        assertFalse(config.qualifiesForParallelProcessing(), "Low detail must not qualify for parallel processing");


        config.setDetailLevel(DetailLevel.HIGH);
        assertFalse(config.qualifiesForParallelProcessing(), "High detail must not qualify for parallel processing");


        config.setDetailLevel(DetailLevel.MEDIUM);
        config.setColorMode(ColorMode.UNLIMITED);
        assertFalse(config.qualifiesForParallelProcessing(), "Unlimited colors must not qualify for parallel processing");
    }

    @Test
    @DisplayName("Batch preprocessing in parallel mode executes all items and updates progress")
    public void testParallelBatchPreprocessing() throws Exception {
        BatchProcessingManager manager = new BatchProcessingManager(vectorizationService);

        List<ImageItem> items = new ArrayList<>();
        items.add(createTestItem("parallel_1.png", 60, 60));
        items.add(createTestItem("parallel_2.png", 60, 60));

        ConversionConfig config = new ConversionConfig(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);
        assertTrue(config.qualifiesForParallelProcessing());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger readyCount = new AtomicInteger(0);

        manager.startPreprocessing(
                items,
                config,
                item -> readyCount.incrementAndGet(),
                latch::countDown
        );

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Parallel batch preprocessing timed out");
        assertEquals(2, readyCount.get(), "Both items should have triggered ready callback");
        assertEquals(1.0, manager.getProgress(), 0.001, "Progress should reach 1.0");
        assertTrue(manager.getStatusMessage().contains("completed"), "Status message should indicate completion");

        for (ImageItem item : items) {
            assertEquals(ImageItem.Status.READY, item.getStatus());
            assertNotNull(item.getVectorizedSvgFile());
            assertTrue(item.getVectorizedSvgFile().exists());
            assertNotNull(item.getSvgContent());
        }

        manager.shutdown();
    }

    @Test
    @DisplayName("Batch preprocessing in sequential mode executes when settings do not qualify")
    public void testSequentialBatchPreprocessing() throws Exception {
        BatchProcessingManager manager = new BatchProcessingManager(vectorizationService);

        List<ImageItem> items = new ArrayList<>();
        items.add(createTestItem("seq_1.png", 60, 60));
        items.add(createTestItem("seq_2.png", 60, 60));


        ConversionConfig config = new ConversionConfig(DetailLevel.HIGH, ColorMode.CUSTOM, 2, false);
        assertFalse(config.qualifiesForParallelProcessing());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger readyCount = new AtomicInteger(0);

        manager.startPreprocessing(
                items,
                config,
                item -> readyCount.incrementAndGet(),
                latch::countDown
        );

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Sequential batch preprocessing timed out");
        assertEquals(2, readyCount.get());
        assertEquals(1.0, manager.getProgress(), 0.001);
        assertTrue(manager.getStatusMessage().toLowerCase().contains("completed"));

        for (ImageItem item : items) {
            assertEquals(ImageItem.Status.READY, item.getStatus());
            assertNotNull(item.getVectorizedSvgFile());
            assertTrue(item.getVectorizedSvgFile().exists());
        }

        manager.shutdown();
    }

    @Test
    @DisplayName("Batch export saves valid SVGs to output folder in parallel")
    public void testBatchExport() throws Exception {
        BatchProcessingManager manager = new BatchProcessingManager(vectorizationService);
        File exportDir = Files.createTempDirectory("vector_export_test").toFile();
        exportDir.deleteOnExit();

        List<ImageItem> items = new ArrayList<>();
        items.add(createTestItem("exp_1.png", 60, 60));
        items.add(createTestItem("exp_2.png", 60, 60));

        ConversionConfig config = new ConversionConfig(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);

        CountDownLatch latch = new CountDownLatch(1);
        manager.startExport(items, config, exportDir, latch::countDown);

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Batch export timed out");
        assertEquals(1.0, manager.getProgress(), 0.001);

        File[] exportedFiles = exportDir.listFiles((dir, name) -> name.endsWith(".svg"));
        assertNotNull(exportedFiles);
        assertEquals(2, exportedFiles.length, "Must export 2 SVG files");

        for (File svg : exportedFiles) {
            assertTrue(svg.length() > 0, "Exported SVG file must not be empty");
            String content = Files.readString(svg.toPath());
            assertTrue(content.contains("<svg"), "Exported file must contain valid SVG tag");
        }

        manager.shutdown();
    }
}
