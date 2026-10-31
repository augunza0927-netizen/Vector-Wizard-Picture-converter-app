package com.vectorconvert;

import com.vectorconvert.controller.SettingsController;
import com.vectorconvert.model.ColorMode;
import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.DetailLevel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


public class ConfigurationAndInstantPreviewTest {

    @BeforeAll
    public static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {

        }
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX thread execution timed out");
    }

    @Test
    public void testDetailLevelChangesTriggerListener() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/SettingsView.fxml"));
                Parent root = loader.load();
                SettingsController controller = loader.getController();

                AtomicReference<ConversionConfig> receivedConfig = new AtomicReference<>();
                controller.setOnConfigChanged(receivedConfig::set);

                @SuppressWarnings("unchecked")
                ComboBox<DetailLevel> detailBox = (ComboBox<DetailLevel>) root.lookup(".combo-box");
                assertNotNull(detailBox);


                detailBox.setValue(DetailLevel.HIGH);
                assertNotNull(receivedConfig.get());
                assertEquals(DetailLevel.HIGH, receivedConfig.get().getDetailLevel());


                detailBox.setValue(DetailLevel.LOW);
                assertEquals(DetailLevel.LOW, receivedConfig.get().getDetailLevel());

            } catch (Exception e) {
                fail("DetailLevel test failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testColorModeAndSpinnerAutoDetectionCapping() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/SettingsView.fxml"));
                Parent root = loader.load();
                SettingsController controller = loader.getController();

                RadioButton unlimitedRadio = (RadioButton) root.lookup("#unlimitedColorRadio");
                RadioButton customRadio = (RadioButton) root.lookup("#customColorRadio");
                @SuppressWarnings("unchecked")
                Spinner<Integer> spinner = (Spinner<Integer>) root.lookup("#customColorSpinner");

                //limit
                controller.setAutoDetectedColorCount(8);
                assertEquals(2, spinner.getValue(), "Should default to 2 colors upon load per rubric");
                assertEquals(2, controller.getCurrentConfig().getCustomColorCount());

                //Unlimited
                unlimitedRadio.setSelected(true);
                assertEquals(ColorMode.UNLIMITED, controller.getCurrentConfig().getColorMode());
                assertTrue(spinner.isDisabled(), "Spinner should be disabled in Unlimited color mode");


                customRadio.setSelected(true);
                assertEquals(ColorMode.CUSTOM, controller.getCurrentConfig().getColorMode());
                assertFalse(spinner.isDisabled(), "Spinner should be enabled in Custom color mode");

            } catch (Exception e) {
                fail("Color mode test failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testRemoveBackgroundCheckboxTrigger() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/SettingsView.fxml"));
                Parent root = loader.load();
                SettingsController controller = loader.getController();

                AtomicReference<ConversionConfig> receivedConfig = new AtomicReference<>();
                controller.setOnConfigChanged(receivedConfig::set);

                CheckBox bgCheckBox = (CheckBox) root.lookup("#removeBackgroundCheckBox");
                assertNotNull(bgCheckBox);


                bgCheckBox.setSelected(true);
                assertNotNull(receivedConfig.get());
                assertTrue(receivedConfig.get().isRemoveBackground());


                bgCheckBox.setSelected(false);
                assertFalse(receivedConfig.get().isRemoveBackground());

            } catch (Exception e) {
                fail("RemoveBackground test failed: " + e.getMessage());
            }
        });
    }
}
