package com.vectorconvert;

import com.vectorconvert.model.ImageItem;
import com.vectorconvert.service.ThemeManager;
import com.vectorconvert.util.FileUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class AdditionalFeaturesTest {

    @BeforeAll
    public static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {

            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
    }

    @Test
    @DisplayName("Theme CSS stylesheets must exist on classpath")
    public void testThemeCssResourcesExist() {
        URL baseCss = getClass().getResource("/com/vectorconvert/css/base.css");
        URL lightCss = getClass().getResource("/com/vectorconvert/css/theme-light.css");
        URL darkCss = getClass().getResource("/com/vectorconvert/css/theme-dark.css");

        assertNotNull(baseCss, "base.css must exist");
        assertNotNull(lightCss, "theme-light.css must exist");
        assertNotNull(darkCss, "theme-dark.css must exist");
    }

    @Test
    @DisplayName("ThemeManager should toggle between Light and Dark mode dynamically")
    public void testThemeManagerToggle() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ThemeManager themeManager = new ThemeManager();
                assertEquals(ThemeManager.Theme.LIGHT, themeManager.getCurrentTheme(), "Initial theme must be LIGHT");

                Scene scene = new Scene(new StackPane(), 400, 300);
                scene.getStylesheets().add(getClass().getResource("/com/vectorconvert/css/theme-light.css").toExternalForm());

                //DARK
                themeManager.toggleTheme(scene);
                assertEquals(ThemeManager.Theme.DARK, themeManager.getCurrentTheme(), "Theme must switch to DARK");
                assertTrue(scene.getStylesheets().stream().anyMatch(s -> s.contains("theme-dark.css")), "Scene must contain theme-dark.css");
                assertFalse(scene.getStylesheets().stream().anyMatch(s -> s.contains("theme-light.css")), "Scene must not contain theme-light.css");

                //LIGHT
                themeManager.toggleTheme(scene);
                assertEquals(ThemeManager.Theme.LIGHT, themeManager.getCurrentTheme(), "Theme must switch back to LIGHT");
                assertTrue(scene.getStylesheets().stream().anyMatch(s -> s.contains("theme-light.css")), "Scene must contain theme-light.css");
                assertFalse(scene.getStylesheets().stream().anyMatch(s -> s.contains("theme-dark.css")), "Scene must not contain theme-dark.css");


                themeManager.applyTheme(scene, ThemeManager.Theme.DARK);
                assertEquals(ThemeManager.Theme.DARK, themeManager.getCurrentTheme());
                assertTrue(scene.getStylesheets().stream().anyMatch(s -> s.contains("theme-dark.css")));
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "ThemeManager test timed out");
    }

    @Test
    @DisplayName("FileUtils should correctly format various byte sizes")
    public void testFileUtilsFormatFileSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0));
        assertEquals("0 B", FileUtils.formatFileSize(-50));
        assertEquals("500.0 B", FileUtils.formatFileSize(500));
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024));
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536));
        assertEquals("170.9 KB", FileUtils.formatFileSize(175001));
        assertEquals("1.0 MB", FileUtils.formatFileSize(1024 * 1024));
        assertEquals("2.5 MB", FileUtils.formatFileSize((long) (2.5 * 1024 * 1024)));
    }

    @Test
    @DisplayName("FileUtils should accurately calculate storage footprint ratio and handle edge cases")
    public void testFileUtilsCalculateRatio() {
        // Standard comparisons
        assertEquals("50.0%", FileUtils.calculateRatio(1000, 500));
        assertEquals("100.0%", FileUtils.calculateRatio(1000, 1000));
        assertEquals("166.2%", FileUtils.calculateRatio(175000, 290850));
        assertEquals("200.0%", FileUtils.calculateRatio(500, 1000));

        // Edge cases
        assertEquals("--", FileUtils.calculateRatio(0, 500));
        assertEquals("--", FileUtils.calculateRatio(1000, 0));
        assertEquals("--", FileUtils.calculateRatio(-1, -1));
        assertEquals("--", FileUtils.calculateRatio(0, 0));
    }

    @Test
    @DisplayName("ImageItem should update and retain original and vector file size metadata")
    public void testImageItemFileSizeTracking() throws IOException {
        File tempOriginal = File.createTempFile("test_orig_", ".png");
        File tempSvg = File.createTempFile("test_vec_", ".svg");
        tempOriginal.deleteOnExit();
        tempSvg.deleteOnExit();

        Files.write(tempOriginal.toPath(), new byte[2048]); // 2 KB
        Files.write(tempSvg.toPath(), new byte[1024]);     // 1 KB

        ImageItem item = new ImageItem(tempOriginal);
        assertEquals(2048, item.getOriginalSizeBytes(), "Original file size must match");
        assertEquals("2.0 KB", FileUtils.formatFileSize(item.getOriginalSizeBytes()));

        // Initially no SVG
        assertEquals(0, item.getVectorizedSizeBytes());
        assertEquals("--", FileUtils.calculateRatio(item.getOriginalSizeBytes(), item.getVectorizedSizeBytes()));

        // Set vectorized SVG
        item.setVectorizedSvgFile(tempSvg);
        assertEquals(1024, item.getVectorizedSizeBytes());
        assertEquals("1.0 KB", FileUtils.formatFileSize(item.getVectorizedSizeBytes()));
        assertEquals("50.0%", FileUtils.calculateRatio(item.getOriginalSizeBytes(), item.getVectorizedSizeBytes()));
    }
}
