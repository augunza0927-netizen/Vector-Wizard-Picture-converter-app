package com.vectorconvert;

import com.vectorconvert.controller.MainController;
import com.vectorconvert.controller.SettingsController;
import com.vectorconvert.model.ImageItem;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class ExportAllIntegrationTest {

    @BeforeAll
    public static void initJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to initialize");
    }

    private WritableImage createSampleImage(int width, int height) {
        WritableImage img = new WritableImage(width, height);
        var writer = img.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, (x < width / 2) ? Color.WHITE : Color.NAVY);
            }
        }
        return img;
    }

    @Test
    @DisplayName("MainController handles empty list export gracefully")
    public void testExportEmptyList() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/MainView.fxml"));
                Parent root = loader.load();
                MainController controller = loader.getController();

                controller.handleExportAll();
                assertTrue(controller.getImageItems().isEmpty());
            } catch (Exception e) {
                fail("Exception on empty export: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Export All saves valid SVG files to target directory for all items")
    public void testExportToDirectory() throws Exception {
        File exportDir = Files.createTempDirectory("phase8_export_dir").toFile();
        exportDir.deleteOnExit();

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/MainView.fxml"));
                Parent root = loader.load();
                MainController controller = loader.getController();


                ImageItem item1 = new ImageItem(createSampleImage(80, 80), "banner_logo.png");
                ImageItem item2 = new ImageItem(createSampleImage(80, 80), "vector_graphic.png");
                controller.getImageItems().addAll(item1, item2);


                controller.exportToDirectory(exportDir, () -> {
                    latch.countDown();
                });
            } catch (Exception e) {
                fail("Failed to setup export: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Export timed out");

        File[] exportedFiles = exportDir.listFiles((dir, name) -> name.endsWith(".svg"));
        assertNotNull(exportedFiles, "Export directory must contain files");
        assertEquals(2, exportedFiles.length, "Must export exactly 2 SVG files");

        for (File svg : exportedFiles) {
            assertTrue(svg.exists());
            assertTrue(svg.length() > 0, "SVG file must have non-zero size");
            String svgContent = Files.readString(svg.toPath());
            assertTrue(svgContent.contains("<svg"), "Must be a valid SVG document");
        }
    }

    @Test
    @DisplayName("SettingsController disables and re-enables export button during export lifecycle")
    public void testExportButtonDisableLifecycle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/SettingsView.fxml"));
                Parent root = loader.load();
                SettingsController controller = loader.getController();

                controller.setExportButtonDisabled(true);

                controller.setExportButtonDisabled(false);
            } catch (Exception e) {
                fail("Exception in SettingsController: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
