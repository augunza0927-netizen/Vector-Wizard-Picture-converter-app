package com.vectorconvert.controller;

import com.vectorconvert.service.ZipExtractionService;
import com.vectorconvert.util.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class DropZoneController {

    @FXML
    private VBox dropTarget;

    private Consumer<List<File>> filesDroppedHandler;
    private final ZipExtractionService zipExtractionService = new ZipExtractionService();

    public void setOnFilesDropped(Consumer<List<File>> handler) {
        this.filesDroppedHandler = handler;
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void handleDragEntered(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            dropTarget.getStyleClass().add("drop-zone-active");
        }
        event.consume();
    }

    @FXML
    private void handleDragExited(DragEvent event) {
        dropTarget.getStyleClass().remove("drop-zone-active");
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        dropTarget.getStyleClass().remove("drop-zone-active");
        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            List<File> rawFiles = event.getDragboard().getFiles();
            processInputFiles(rawFiles);
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image(s) or ZIP Archive");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Files (*.png, *.jpg, *.jpeg, *.zip)", "*.png", "*.jpg", "*.jpeg", "*.zip"),
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("ZIP Archives (*.zip)", "*.zip")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(dropTarget.getScene().getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            processInputFiles(selectedFiles);
        }
    }

    //extracting ZIP

    public void processInputFiles(List<File> rawFiles) {
        if (rawFiles == null || rawFiles.isEmpty()) return;

        List<File> resolvedImages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (File file : rawFiles) {
            if (FileUtils.isZipFile(file)) {
                try {
                    List<File> unzipped = zipExtractionService.extractImages(file);
                    resolvedImages.addAll(unzipped);
                } catch (Exception e) {
                    errorMessages.add("ZIP Error (" + file.getName() + "): " + e.getMessage());
                }
            } else if (FileUtils.isImageFile(file)) {
                resolvedImages.add(file);
            } else {
                errorMessages.add("Skipped unsupported file: " + file.getName());
            }
        }

        if (resolvedImages.isEmpty()) {
            showErrorDialog("No Valid Images Found",
                    "None of the provided files could be processed.",
                    String.join("\n", errorMessages.isEmpty() ? List.of("Please provide .jpg, .png, or .zip files containing images.") : errorMessages));
            return;
        }

        if (filesDroppedHandler != null) {
            filesDroppedHandler.accept(resolvedImages);
        }
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
