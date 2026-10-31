package com.vectorconvert.controller;

import com.vectorconvert.model.ImageItem;
import com.vectorconvert.util.FileUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;


public class PreviewController {

    @FXML
    private Button zoomInButton;

    @FXML
    private Button zoomOutButton;

    @FXML
    private Button zoomFitButton;

    @FXML
    private ScrollPane originalScrollPane;

    @FXML
    private ScrollPane vectorScrollPane;

    @FXML
    private Group originalGroup;

    @FXML
    private ImageView originalImageView;

    @FXML
    private WebView vectorWebView;

    @FXML
    private Label originalSizeLabel;

    @FXML
    private Label vectorSizeLabel;

    @FXML
    private Label ratioLabel;

    private double currentZoomFactor = 1.0;
    private ImageItem currentItem;
    private boolean isSyncingScroll = false;

    @FXML
    public void initialize() {

        setupSynchronizedScrolling();
    }

    private void setupSynchronizedScrolling() {
        if (originalScrollPane == null || vectorScrollPane == null) return;


        originalScrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSyncingScroll) {
                isSyncingScroll = true;
                vectorScrollPane.setHvalue(newVal.doubleValue());
                isSyncingScroll = false;
            }
        });
        vectorScrollPane.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSyncingScroll) {
                isSyncingScroll = true;
                originalScrollPane.setHvalue(newVal.doubleValue());
                isSyncingScroll = false;
            }
        });


        originalScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSyncingScroll) {
                isSyncingScroll = true;
                vectorScrollPane.setVvalue(newVal.doubleValue());
                isSyncingScroll = false;
            }
        });
        vectorScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSyncingScroll) {
                isSyncingScroll = true;
                originalScrollPane.setVvalue(newVal.doubleValue());
                isSyncingScroll = false;
            }
        });
    }

    @FXML
    private void handleZoomIn() {
        currentZoomFactor = Math.min(currentZoomFactor + 0.25, 4.0);
        applyZoom();
    }

    @FXML
    private void handleZoomOut() {
        currentZoomFactor = Math.max(currentZoomFactor - 0.25, 0.25);
        applyZoom();
    }

    @FXML
    private void handleZoomFit() {
        currentZoomFactor = 1.0;
        applyZoom();
    }

    private void applyZoom() {
        if (originalImageView != null) {
            originalImageView.setScaleX(currentZoomFactor);
            originalImageView.setScaleY(currentZoomFactor);
        }

        if (vectorWebView != null) {
            vectorWebView.setZoom(currentZoomFactor);
        }
    }


    public void displayItem(ImageItem item) {
        this.currentItem = item;
        if (item == null) {
            clearDisplay();
            return;
        }//Update Preview


        Image fxImage = item.getFxImage();
        if (fxImage != null) {
            originalImageView.setImage(fxImage);
            double imgWidth = fxImage.getWidth();
            double imgHeight = fxImage.getHeight();
            originalImageView.setFitWidth(imgWidth);
            originalImageView.setFitHeight(imgHeight);

            if (vectorWebView != null && imgWidth > 0 && imgHeight > 0) {
                vectorWebView.setPrefSize(imgWidth, imgHeight);
            }
        } else {
            originalImageView.setImage(null);
        }


        renderSvg(item.getSvgContent());

        //Feature 2
        long originalBytes = item.getOriginalSizeBytes();
        long vectorBytes = item.getVectorizedSizeBytes();

        originalSizeLabel.setText(FileUtils.formatFileSize(originalBytes));
        vectorSizeLabel.setText(vectorBytes > 0 ? FileUtils.formatFileSize(vectorBytes) : "--");
        ratioLabel.setText(FileUtils.calculateRatio(originalBytes, vectorBytes));

        applyZoom();
    }

    public void renderSvg(String svgContent) {
        if (vectorWebView == null) return;

        if (svgContent != null && !svgContent.isBlank()) {
            String htmlContent = buildHtmlSvgWrapper(svgContent);
            Platform.runLater(() -> vectorWebView.getEngine().loadContent(htmlContent, "text/html"));
        } else {
            String emptyPlaceholder = "<html><body style='margin:0;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;color:#a0aec0;'><div>Processing vector preview...</div></body></html>";
            Platform.runLater(() -> vectorWebView.getEngine().loadContent(emptyPlaceholder, "text/html"));
        }
    }

    private String buildHtmlSvgWrapper(String svgContent) {
        String cleanSvg = svgContent
                .replaceFirst("<\\?xml[^>]*\\?>", "")
                .replaceFirst("<!DOCTYPE[^>]*>", "")
                .trim();

        return "<!DOCTYPE html><html>" +
                "<head><style>" +
                "html, body { margin:0; padding:0; width:100%; height:100%; background:transparent; display:flex; justify-content:center; align-items:center; box-sizing:border-box; overflow:hidden; }" +
                "svg { width:95%; height:95%; display:block; }" +
                "</style></head>" +
                "<body>" + cleanSvg + "</body></html>";
    }

    public void clearDisplay() {
        if (originalImageView != null) originalImageView.setImage(null);
        if (vectorWebView != null) vectorWebView.getEngine().loadContent("", "text/html");
        originalSizeLabel.setText("--");
        vectorSizeLabel.setText("--");
        ratioLabel.setText("--");
    }

    public double getCurrentZoomFactor() {
        return currentZoomFactor;
    }
}
