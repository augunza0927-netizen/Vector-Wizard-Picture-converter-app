package com.vectorconvert.service;

import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.ImageItem;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class BatchProcessingManager {

    private final VectorizationService vectorizationService;
    private final ExecutorService threadPool;
    private final File tempWorkingDir;

    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final BooleanProperty isProcessing = new SimpleBooleanProperty(false);

    //CPU
    public BatchProcessingManager(VectorizationService vectorizationService) {
        this.vectorizationService = vectorizationService;

        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.threadPool = Executors.newFixedThreadPool(cores);

        this.tempWorkingDir = new File(System.getProperty("java.io.tmpdir"), "vectorwizard_batch");
        if (!tempWorkingDir.exists()) {
            tempWorkingDir.mkdirs();
        }
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public BooleanProperty isProcessingProperty() {
        return isProcessing;
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public File getTempWorkingDir() {
        return tempWorkingDir;
    }


    public void startPreprocessing(List<ImageItem> items, ConversionConfig config, Consumer<ImageItem> onItemReady, Runnable onAllComplete) {
        if (items == null || items.isEmpty()) {
            if (onAllComplete != null) Platform.runLater(onAllComplete);
            return;
        }

        final int total = items.size();
        final boolean runInParallel = config.qualifiesForParallelProcessing() && total > 1;


        Platform.runLater(() -> {
            isProcessing.set(true);
            progress.set(0.0);
            statusMessage.set(runInParallel
                    ? String.format("Preprocessing in parallel (%d images)...", total)
                    : String.format("Preprocessing sequentially (%d images)...", total));
        });

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        if (runInParallel) {

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (ImageItem item : items) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    processSingleItem(item, config, onItemReady, errorCount);
                    updateBatchProgress(completedCount.incrementAndGet(), total, errorCount.get(), true, "Preprocessing");
                }, threadPool);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                finishBatchOperation("Preprocessing", total, errorCount.get(), true, onAllComplete);
            });

        } else {
            threadPool.submit(() -> {
                for (ImageItem item : items) {
                    processSingleItem(item, config, onItemReady, errorCount);
                    updateBatchProgress(completedCount.incrementAndGet(), total, errorCount.get(), false, "Preprocessing");
                }
                finishBatchOperation("Preprocessing", total, errorCount.get(), false, onAllComplete);
            });
        }
    }


    public void startExport(List<ImageItem> items, ConversionConfig config, File outputDirectory, Runnable onComplete) {
        if (items == null || items.isEmpty() || outputDirectory == null) {
            if (onComplete != null) Platform.runLater(onComplete);
            return;
        }

        final int total = items.size();
        final boolean runInParallel = config.qualifiesForParallelProcessing() && total > 1;

        Platform.runLater(() -> {
            isProcessing.set(true);
            progress.set(0.0);
            statusMessage.set(runInParallel
                    ? String.format("Exporting in parallel to %s (%d files)...", outputDirectory.getName(), total)
                    : String.format("Exporting sequentially to %s (%d files)...", outputDirectory.getName(), total));
        });

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        if (runInParallel) {
            // Concurrent export
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (ImageItem item : items) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    exportSingleItem(item, config, outputDirectory, errorCount);
                    updateBatchProgress(completedCount.incrementAndGet(), total, errorCount.get(), true, "Exporting");
                }, threadPool);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                finishBatchOperation("Export", total, errorCount.get(), true, onComplete);
            });

        } else {
            //Sequential export!!!
            threadPool.submit(() -> {
                for (ImageItem item : items) {
                    exportSingleItem(item, config, outputDirectory, errorCount);
                    updateBatchProgress(completedCount.incrementAndGet(), total, errorCount.get(), false, "Exporting");
                }
                finishBatchOperation("Export", total, errorCount.get(), false, onComplete);
            });
        }
    }

    private void processSingleItem(ImageItem item, ConversionConfig config, Consumer<ImageItem> onItemReady, AtomicInteger errorCount) {
        try {
            item.setStatus(ImageItem.Status.PREPROCESSING);
            //Detect colors
            int colors = vectorizationService.detectColors(item);
            item.setDetectedColorCount(colors);

            File svgFile = vectorizationService.convertToSvg(item, config, tempWorkingDir);
            item.setVectorizedSvgFile(svgFile);

            if (svgFile != null && svgFile.exists()) {
                String svgText = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);
                item.setSvgContent(svgText);
                item.setStatus(ImageItem.Status.READY);
            }

            if (onItemReady != null) {
                Platform.runLater(() -> onItemReady.accept(item));
            }
        } catch (Exception ex) {
            errorCount.incrementAndGet();
            item.setStatus(ImageItem.Status.FAILED);
            item.setErrorMessage(ex.getMessage());
        }
    }

    private void exportSingleItem(ImageItem item, ConversionConfig config, File outputDirectory, AtomicInteger errorCount) {
        try {
            item.setStatus(ImageItem.Status.EXPORTING);
            File svgFile = vectorizationService.convertToSvg(item, config, outputDirectory);
            item.setVectorizedSvgFile(svgFile);
            item.setStatus(ImageItem.Status.COMPLETED);
        } catch (Exception ex) {
            errorCount.incrementAndGet();
            item.setStatus(ImageItem.Status.FAILED);
            item.setErrorMessage(ex.getMessage());
        }
    }

    private void updateBatchProgress(int done, int total, int errors, boolean inParallel, String action) {
        double currentProgress = (double) done / total;
        String mode = inParallel ? "parallel" : "sequential";
        String message = String.format("%s in %s: %d / %d (%.0f%%)%s",
                action, mode, done, total, currentProgress * 100,
                errors > 0 ? " [" + errors + " error(s)]" : "");

        Platform.runLater(() -> {
            progress.set(currentProgress);
            statusMessage.set(message);
        });
    }

    private void finishBatchOperation(String action, int total, int errors, boolean inParallel, Runnable onComplete) {
        String mode = inParallel ? "Parallel" : "Sequential";
        String finalMessage = errors == 0
                ? String.format("%s %s completed successfully (%d items).", mode, action, total)
                : String.format("%s %s completed with %d error(s) (%d total).", mode, action, errors, total);

        Platform.runLater(() -> {
            progress.set(1.0);
            statusMessage.set(finalMessage);
            isProcessing.set(false);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }


    public void shutdown() {
        threadPool.shutdownNow();
    }
}
