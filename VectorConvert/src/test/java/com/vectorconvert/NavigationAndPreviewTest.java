package com.vectorconvert;

import com.vectorconvert.controller.PreviewController;
import com.vectorconvert.controller.SettingsController;
import com.vectorconvert.model.ImageItem;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;


public class NavigationAndPreviewTest {

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
        assertTrue(latch.await(15, TimeUnit.SECONDS), "JavaFX thread action timed out");
    }

    @Test
    public void testSettingsNavigationStates() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/SettingsView.fxml"));
                Parent root = loader.load();
                SettingsController controller = loader.getController();

                Button backBtn = (Button) root.lookup("#backButton");
                Button nextBtn = (Button) root.lookup("#nextButton");
                Label indexLabel = (Label) root.lookup("#imageIndexLabel");


                controller.updateNavigationStatus(0, 3);
                assertTrue(backBtn.isDisabled(), "Back button should be disabled on first image");
                assertFalse(nextBtn.isDisabled(), "Next button should be enabled on first image");
                assertEquals("1 / 3", indexLabel.getText());


                controller.updateNavigationStatus(1, 3);
                assertFalse(backBtn.isDisabled(), "Back button should be enabled on middle image");
                assertFalse(nextBtn.isDisabled(), "Next button should be enabled on middle image");
                assertEquals("2 / 3", indexLabel.getText());


                controller.updateNavigationStatus(2, 3);
                assertFalse(backBtn.isDisabled(), "Back button should be enabled on last image");
                assertTrue(nextBtn.isDisabled(), "Next button should be disabled on last image");
                assertEquals("3 / 3", indexLabel.getText());


                controller.updateNavigationStatus(1, 3);
                AtomicBoolean backTriggered = new AtomicBoolean(false);
                AtomicBoolean nextTriggered = new AtomicBoolean(false);
                controller.setOnBackAction(() -> backTriggered.set(true));
                controller.setOnNextAction(() -> nextTriggered.set(true));

                backBtn.fire();
                assertTrue(backTriggered.get(), "Back button click should invoke back action listener");

                nextBtn.fire();
                assertTrue(nextTriggered.get(), "Next button click should invoke next action listener");

            } catch (Exception e) {
                fail("Failed to test SettingsController: " + e.getMessage());
            }
        });
    }

    @Test
    public void testPreviewZoomAndFileSizeDisplay() throws Exception {
        runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/PreviewView.fxml"));
                Parent root = loader.load();
                PreviewController controller = loader.getController();

                Button zoomIn = (Button) root.lookup("#zoomInButton");
                Button zoomOut = (Button) root.lookup("#zoomOutButton");
                Button zoomFit = (Button) root.lookup("#zoomFitButton");

                //Zoom In
                double initialZoom = controller.getCurrentZoomFactor();
                zoomIn.fire();
                assertTrue(controller.getCurrentZoomFactor() > initialZoom, "Zoom In should increase zoom factor");

                //Zoom Out
                zoomOut.fire();
                assertEquals(initialZoom, controller.getCurrentZoomFactor(), 0.001, "Zoom Out should decrease zoom factor back");

                //Zoom Fit
                zoomIn.fire();
                zoomIn.fire();
                zoomFit.fire();
                assertEquals(1.0, controller.getCurrentZoomFactor(), 0.001, "Zoom Fit should reset factor to 1.0");


                WritableImage image = new WritableImage(50, 50);
                ImageItem item = new ImageItem(image, "photo.png");
                item.setSvgContent("<svg></svg>");

                controller.displayItem(item);

                Label originalSizeLabel = (Label) root.lookup("#originalSizeLabel");
                Label vectorSizeLabel = (Label) root.lookup("#vectorSizeLabel");
                assertNotNull(originalSizeLabel.getText());
                assertNotNull(vectorSizeLabel.getText());

            } catch (Exception e) {
                fail("Failed to test PreviewController: " + e.getMessage());
            }
        });
    }
}
