package com.vectorconvert.model;

import java.io.File;


public class ImageItem {

    public enum Status {
        PENDING,
        PREPROCESSING,
        READY,
        EXPORTING,
        COMPLETED,
        FAILED
    }

    private final File originalFile;
    private final String fileName;
    //store value
    private final long originalSizeBytes;
    private javafx.scene.image.Image fxImage;
    
    private int detectedColorCount = 2;
    private File vectorizedSvgFile;
    private long vectorizedSizeBytes = 0;
    private String svgContent;
    
    private Status status = Status.PENDING;
    private String errorMessage;

    //pull file
    public ImageItem(File originalFile) {
        this.originalFile = originalFile;
        this.fileName = originalFile != null ? originalFile.getName() : "";
        this.originalSizeBytes = originalFile != null ? originalFile.length() : 0;
    }

    public ImageItem(javafx.scene.image.Image fxImage, String fileName) {
        this.originalFile = null;
        this.fileName = fileName != null ? fileName : "image.png";
        this.originalSizeBytes = 0;
        this.fxImage = fxImage;
    }

    public javafx.scene.image.Image getFxImage() {
        if (fxImage == null && originalFile != null && originalFile.exists()) {
            try (java.io.InputStream is = new java.io.BufferedInputStream(new java.io.FileInputStream(originalFile))) {
                this.fxImage = new javafx.scene.image.Image(is);
            } catch (Exception e) {
                this.fxImage = new javafx.scene.image.Image(originalFile.toURI().toString());
            }
        }
        return fxImage;
    }

    public void setFxImage(javafx.scene.image.Image fxImage) {
        this.fxImage = fxImage;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public String getFileName() {
        return fileName;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public int getDetectedColorCount() {
        return detectedColorCount;
    }

    public void setDetectedColorCount(int detectedColorCount) {
        this.detectedColorCount = detectedColorCount;
    }

    public File getVectorizedSvgFile() {
        return vectorizedSvgFile;
    }

    //Update SVG
    public void setVectorizedSvgFile(File vectorizedSvgFile) {
        this.vectorizedSvgFile = vectorizedSvgFile;
        if (vectorizedSvgFile != null && vectorizedSvgFile.exists()) {
            this.vectorizedSizeBytes = vectorizedSvgFile.length();
        }
    }

    public long getVectorizedSizeBytes() {
        return vectorizedSizeBytes;
    }

    public void setVectorizedSizeBytes(long vectorizedSizeBytes) {
        this.vectorizedSizeBytes = vectorizedSizeBytes;
    }

    public String getSvgContent() {
        return svgContent;
    }

    public void setSvgContent(String svgContent) {
        this.svgContent = svgContent;
        if (svgContent != null) {
            this.vectorizedSizeBytes = svgContent.getBytes().length;
        }
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
