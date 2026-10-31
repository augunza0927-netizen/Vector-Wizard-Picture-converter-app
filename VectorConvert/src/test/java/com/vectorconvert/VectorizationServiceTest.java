package com.vectorconvert;

import com.vectorconvert.model.ColorMode;
import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.DetailLevel;
import com.vectorconvert.model.ImageItem;
import com.vectorconvert.service.VectorizationService;
import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;


public class VectorizationServiceTest {

    private VectorizationService service = new VectorizationService();
    private File testOutputDir = new File(System.getProperty("java.io.tmpdir"), "vector_test_out");

    @BeforeAll
    public static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {

        }
    }

    @BeforeEach
    public void setUp() {
        service = new VectorizationService();
        testOutputDir.mkdirs();
    }


    private WritableImage createSampleJavaFxImage(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, Color.WHITE);
            }
        }


        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 4;
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (Math.hypot(x - centerX, y - centerY) <= radius) {
                    writer.setColor(x, y, Color.DODGERBLUE);
                }
            }
        }


        for (int y = height / 3; y < 2 * height / 3; y++) {
            for (int x = width / 3; x < 2 * width / 3; x++) {
                writer.setColor(x, y, Color.CRIMSON);
            }
        }

        return image;
    }

    @Test
    public void testToolAvailability() {
        assertTrue(service.isToolAvailable(), "Potrace binary should be detected and executable");
    }

    @Test
    public void testColorDetection() throws Exception {
        WritableImage sample = createSampleJavaFxImage(100, 100);
        ImageItem item = new ImageItem(sample, "sample.png");

        int detected = service.detectColors(item);
        assertTrue(detected >= 2 && detected <= 5, "Detected colors must be capped between 2 and 5");
        assertEquals(detected, item.getDetectedColorCount());
    }

    @Test
    public void testVectorizationWithDefaultSettings() throws Exception {
        WritableImage sample = createSampleJavaFxImage(120, 120);
        ImageItem item = new ImageItem(sample, "test_render.png");

        ConversionConfig config = new ConversionConfig(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);
        File svgOutput = service.convertToSvg(item, config, testOutputDir);

        assertNotNull(svgOutput);
        assertTrue(svgOutput.exists());
        assertTrue(svgOutput.length() > 0);

        String svgContent = Files.readString(svgOutput.toPath());
        assertTrue(svgContent.contains("<svg"), "Output must contain <svg tag");
        assertTrue(svgContent.contains("</svg>"), "Output must close with </svg>");
        assertTrue(svgContent.contains("<path"), "Output must contain vector paths");
    }

    @Test
    public void testVectorizationWithDifferentDetailLevels() throws Exception {
        WritableImage sample = createSampleJavaFxImage(100, 100);
        ImageItem item = new ImageItem(sample, "test_detail.png");

        for (DetailLevel level : DetailLevel.values()) {
            ConversionConfig config = new ConversionConfig(level, ColorMode.CUSTOM, 3, false);
            File svgOutput = service.convertToSvg(item, config, testOutputDir);

            assertNotNull(svgOutput);
            assertTrue(svgOutput.exists());
            assertTrue(svgOutput.length() > 0);
        }
    }

    @Test
    public void testRemoveBackgroundTransparentSvg() throws Exception {
        WritableImage sample = createSampleJavaFxImage(100, 100);
        ImageItem item = new ImageItem(sample, "test_bg.png");

        File dirWithBg = new File(testOutputDir, "with_bg");
        File dirNoBg = new File(testOutputDir, "no_bg");
        dirWithBg.mkdirs();
        dirNoBg.mkdirs();


        ConversionConfig configWithBg = new ConversionConfig(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);
        File svgWithBg = service.convertToSvg(item, configWithBg, dirWithBg);

        //without background
        ConversionConfig configNoBg = new ConversionConfig(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, true);
        File svgNoBg = service.convertToSvg(item, configNoBg, dirNoBg);

        assertNotNull(svgWithBg);
        assertNotNull(svgNoBg);

        String contentWithBg = Files.readString(svgWithBg.toPath());
        String contentNoBg = Files.readString(svgNoBg.toPath());

        assertTrue(contentWithBg.contains("<rect"), "SVG with background must have background rect");
        assertFalse(contentNoBg.contains("<rect"), "SVG without background must not have background rect (transparent)");
        assertNotEquals(contentWithBg, contentNoBg);
    }
}
