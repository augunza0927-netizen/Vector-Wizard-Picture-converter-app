package com.vectorconvert.model;


public class ConversionConfig {

    private DetailLevel detailLevel = DetailLevel.MEDIUM;
    private ColorMode colorMode = ColorMode.CUSTOM;
    private int customColorCount = 2; // Default specification
    private boolean removeBackground = false;

    public ConversionConfig() {
    }

    public ConversionConfig(DetailLevel detailLevel, ColorMode colorMode, int customColorCount, boolean removeBackground) {
        this.detailLevel = detailLevel;
        this.colorMode = colorMode;
        this.customColorCount = customColorCount;
        this.removeBackground = removeBackground;
    }

    public DetailLevel getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(DetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    public ColorMode getColorMode() {
        return colorMode;
    }

    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
    }

    public int getCustomColorCount() {
        return customColorCount;
    }

    public void setCustomColorCount(int customColorCount) {
        this.customColorCount = Math.max(2, Math.min(5, customColorCount));
    }

    public boolean isRemoveBackground() {
        return removeBackground;
    }

    public void setRemoveBackground(boolean removeBackground) {
        this.removeBackground = removeBackground;
    }

    public boolean qualifiesForParallelProcessing() {
        return detailLevel == DetailLevel.MEDIUM 
                && colorMode == ColorMode.CUSTOM 
                && customColorCount >= 2 
                && customColorCount <= 5;
    }
}
