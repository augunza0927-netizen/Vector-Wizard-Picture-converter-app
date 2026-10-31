package com.vectorconvert.controller;

import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.ImageItem;
import com.vectorconvert.service.BatchProcessingManager;
import com.vectorconvert.service.ThemeManager;
import com.vectorconvert.service.VectorizationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox dropZone;

    @FXML
    private BorderPane editorContainer;

    @FXML
    private Button themeToggleButton;

    @FXML
    private ProgressBar batchProgressBar;

    @FXML
    private Label statusLabel;

    @FXML
    private DropZoneController dropZoneController;

    @FXML
    private PreviewController previewController;

    @FXML
    private SettingsController settingsController;

    private final ThemeManager themeManager = new ThemeManager();
    private final VectorizationService vectorizationService = new VectorizationService();
    private final BatchProcessingManager batchProcessingManager = new BatchProcessingManager(vectorizationService);

    private final ObservableList<ImageItem> imageItems = FXCollections.observableArrayList();
    private int currentItemIndex = 0;
    private Task<File> activePreviewTask;

    @FXML
    public void initialize() {
        // Wire progress
        batchProgressBar.progressProperty().bind(batchProcessingManager.progressProperty());
        batchProcessingManager.statusMessageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> statusLabel.setText(newVal));//buggggggggg
            }
        });


        if (dropZoneController != null) {
            dropZoneController.setOnFilesDropped(this::handleDroppedFiles);
        }


        if (settingsController != null) {
            settingsController.setOnBackAction(this::handlePreviousImage);
            settingsController.setOnNextAction(this::handleNextImage);
            settingsController.setOnConfigChanged(this::handleConfigChanged);
            settingsController.setOnExportAction(this::handleExportAll);
        }
    }

    //Export
    public void handleExportAll() {
        if (imageItems.isEmpty()) {
            statusLabel.setText("No images to export.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory for Vectorized SVGs");

        Window window = (rootPane != null && rootPane.getScene() != null)
                ? rootPane.getScene().getWindow()
                : null;

        File selectedDir = chooser.showDialog(window);
        if (selectedDir != null) {
            exportToDirectory(selectedDir, null);
        } else {
            statusLabel.setText("Export cancelled.");
        }
    }


    public void exportToDirectory(File outputDir, Runnable onFinished) {
        if (outputDir == null || imageItems.isEmpty()) return;

        ConversionConfig config = settingsController != null
                ? settingsController.getCurrentConfig()
                : new ConversionConfig();

        if (settingsController != null) {
            settingsController.setExportButtonDisabled(true);
        }

        final int count = imageItems.size();
        batchProcessingManager.startExport(
                new ArrayList<>(imageItems),
                config,
                outputDir,
                () -> {
                    if (settingsController != null) {
                        settingsController.setExportButtonDisabled(false);
                    }


                    if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                        try {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Export Complete");
                            alert.setHeaderText("Batch Export Successful");
                            alert.setContentText(String.format("Successfully exported %d SVG file(s) to:\n%s", count, outputDir.getAbsolutePath()));
                            alert.showAndWait();
                        } catch (Exception ignored) {

                        }
                    }

                    if (onFinished != null) {
                        onFinished.run();
                    }
                }
        );
    }

    @FXML
    private void handleToggleTheme() {
        // Feature 1
        if (rootPane != null && rootPane.getScene() != null) {
            themeManager.toggleTheme(rootPane.getScene());
            boolean isDark = themeManager.getCurrentTheme() == ThemeManager.Theme.DARK;
            themeToggleButton.setText(isDark ? " Light Mode" : " Dark Mode");
        }
    }


    public void handleDroppedFiles(List<File> files) {
        if (files == null || files.isEmpty()) return;

        imageItems.clear();
        for (File file : files) {
            imageItems.add(new ImageItem(file));
        }


        if (dropZone != null) {
            dropZone.setVisible(false);
            dropZone.setManaged(false);
        }
        if (editorContainer != null) {
            editorContainer.setVisible(true);
            editorContainer.setManaged(true);
        }

        currentItemIndex = 0;
        displayCurrentImage();


        if (imageItems.size() > 1) {
            ConversionConfig config = settingsController != null ? settingsController.getCurrentConfig() : new ConversionConfig();
            batchProcessingManager.startPreprocessing(
                    new ArrayList<>(imageItems),
                    config,
                    readyItem -> {
                        if (!imageItems.isEmpty() && imageItems.get(currentItemIndex) == readyItem) {
                            if (previewController != null) {
                                previewController.displayItem(readyItem);
                            }
                        }
                    },
                    () -> {
                        if (!imageItems.isEmpty() && currentItemIndex < imageItems.size()) {
                            ImageItem current = imageItems.get(currentItemIndex);
                            if (previewController != null) {
                                previewController.displayItem(current);
                            }
                        }
                    }
            );
        }
    }

    private void handlePreviousImage() {
        if (currentItemIndex > 0) {
            currentItemIndex--;
            displayCurrentImage();
        }
    }

    private void handleNextImage() {
        if (currentItemIndex < imageItems.size() - 1) {
            currentItemIndex++;
            displayCurrentImage();
        }
    }


    public void displayCurrentImage() {
        if (imageItems.isEmpty()) {
            if (previewController != null) previewController.clearDisplay();
            if (settingsController != null) settingsController.updateNavigationStatus(0, 0);
            return;
        }

        ImageItem current = imageItems.get(currentItemIndex);

        // Update navigation
        if (settingsController != null) {
            settingsController.updateNavigationStatus(currentItemIndex, imageItems.size());
            

            try {
                int detected = vectorizationService.detectColors(current);
                settingsController.setAutoDetectedColorCount(detected);
            } catch (Exception e) {
                settingsController.setAutoDetectedColorCount(2);
            }
        }

        // Update preview
        if (previewController != null) {
            previewController.displayItem(current);
        }

        //Preview SVG
        if (current.getSvgContent() == null && !batchProcessingManager.isProcessing()) {
            generatePreviewForCurrent(current);
        }
    }

    public void handleConfigChanged(ConversionConfig newConfig) {
        if (imageItems.isEmpty()) return;
        ImageItem current = imageItems.get(currentItemIndex);
        generatePreviewForCurrent(current);
    }


    public void generatePreviewForCurrent(ImageItem item) {
        if (item == null || item.getOriginalFile() == null) return;


        if (activePreviewTask != null && activePreviewTask.isRunning()) {
            activePreviewTask.cancel(true);
        }

        ConversionConfig config = settingsController != null ? settingsController.getCurrentConfig() : new ConversionConfig();

        Task<File> previewTask = new Task<>() {
            @Override
            protected File call() throws Exception {
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "vector_preview");
                tempDir.mkdirs();
                return vectorizationService.convertToSvg(item, config, tempDir);
            }
        };

        this.activePreviewTask = previewTask;

        previewTask.setOnSucceeded(e -> {
            Platform.runLater(() -> { //Conc
                statusLabel.setText("Preview updated");
                if (!imageItems.isEmpty() && imageItems.get(currentItemIndex) == item) {
                    if (previewController != null) {
                        previewController.displayItem(item);
                    }
                }
            });
        });

        previewTask.setOnFailed(e -> {
            Throwable ex = previewTask.getException();
            if (ex != null) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error generating preview: " + ex.getMessage());
                });
            }
        });

        Thread thread = new Thread(previewTask, "PreviewGeneratorThread");
        thread.setDaemon(true);
        thread.start();
    }

    public ObservableList<ImageItem> getImageItems() {
        return imageItems;
    }

    public int getCurrentItemIndex() {
        return currentItemIndex;
    }

    public VectorizationService getVectorizationService() {
        return vectorizationService;
    }

    public SettingsController getSettingsController() {
        return settingsController;
    }

    public PreviewController getPreviewController() {
        return previewController;
    }

    public BatchProcessingManager getBatchProcessingManager() {
        return batchProcessingManager;
    }

    public void shutdown() {
        if (activePreviewTask != null && activePreviewTask.isRunning()) {
            activePreviewTask.cancel(true);
        }
        batchProcessingManager.shutdown();
    }
}
