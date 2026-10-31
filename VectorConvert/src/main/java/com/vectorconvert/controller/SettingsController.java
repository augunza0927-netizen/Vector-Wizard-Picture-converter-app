package com.vectorconvert.controller;

import com.vectorconvert.model.ColorMode;
import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.DetailLevel;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.function.Consumer;


public class SettingsController {

    @FXML
    private Button exportAllButton;

    @FXML
    private ComboBox<DetailLevel> detailLevelComboBox;

    @FXML
    private ToggleGroup colorToggleGroup;

    @FXML
    private RadioButton unlimitedColorRadio;

    @FXML
    private RadioButton customColorRadio;

    @FXML
    private Spinner<Integer> customColorSpinner;

    @FXML
    private CheckBox removeBackgroundCheckBox;

    @FXML
    private Button backButton;

    @FXML
    private Button nextButton;

    @FXML
    private Label imageIndexLabel;

    private final ConversionConfig currentConfig = new ConversionConfig();
    private Consumer<ConversionConfig> configChangedListener;
    private Runnable exportActionListener;
    private Runnable backActionListener;
    private Runnable nextActionListener;

    @FXML
    public void initialize() {

        detailLevelComboBox.getItems().setAll(DetailLevel.LOW, DetailLevel.MEDIUM, DetailLevel.HIGH);
        detailLevelComboBox.setValue(DetailLevel.MEDIUM);
        detailLevelComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentConfig.setDetailLevel(newVal);
                notifyConfigChanged();
            }
        });

        //2-5
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2);
        customColorSpinner.setValueFactory(valueFactory);
        customColorSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentConfig.setCustomColorCount(newVal);
                notifyConfigChanged();
            }
        });


        if (customColorRadio != null) {
            customColorRadio.setSelected(true);
        }
        if (colorToggleGroup != null) {
            colorToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == unlimitedColorRadio) {
                    currentConfig.setColorMode(ColorMode.UNLIMITED);
                    customColorSpinner.setDisable(true);
                } else {
                    currentConfig.setColorMode(ColorMode.CUSTOM);
                    customColorSpinner.setDisable(false);
                }
                notifyConfigChanged();
            });
        }


        removeBackgroundCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            currentConfig.setRemoveBackground(Boolean.TRUE.equals(newVal));
            notifyConfigChanged();
        });


        backButton.setDisable(true);
        nextButton.setDisable(true);
    }

    private void notifyConfigChanged() {
        if (configChangedListener != null) {
            configChangedListener.accept(currentConfig);
        }
    }

    public ConversionConfig getCurrentConfig() {
        return currentConfig;
    }

    public void setOnConfigChanged(Consumer<ConversionConfig> listener) {
        this.configChangedListener = listener;
    }

    public void setOnExportAction(Runnable listener) {
        this.exportActionListener = listener;
    }

    public void setExportButtonDisabled(boolean disabled) {
        if (exportAllButton != null) {
            exportAllButton.setDisable(disabled);
        }
    }

    public void setOnBackAction(Runnable listener) {
        this.backActionListener = listener;
    }

    public void setOnNextAction(Runnable listener) {
        this.nextActionListener = listener;
    }

    @FXML
    private void handleExportAll() {
        if (exportActionListener != null) exportActionListener.run();
    }

    @FXML
    private void handleBack() {
        if (backActionListener != null) backActionListener.run();
    }

    @FXML
    private void handleNext() {
        if (nextActionListener != null) nextActionListener.run();
    }

    public void updateNavigationStatus(int currentIndex, int totalCount) {
        imageIndexLabel.setText((totalCount > 0 ? (currentIndex + 1) : 0) + " / " + totalCount);
        backButton.setDisable(currentIndex <= 0);
        nextButton.setDisable(currentIndex >= totalCount - 1);
    }


    public void setAutoDetectedColorCount(int count) {
        int maxAllowed = Math.max(2, Math.min(5, count));
        if (customColorSpinner != null && customColorSpinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory factory) {
            factory.setMax(maxAllowed);
            factory.setValue(2);
            currentConfig.setCustomColorCount(2);
        }
    }


    public void applyConfig(ConversionConfig config) {
        if (config == null) return;
        currentConfig.setDetailLevel(config.getDetailLevel());
        currentConfig.setColorMode(config.getColorMode());
        currentConfig.setCustomColorCount(config.getCustomColorCount());
        currentConfig.setRemoveBackground(config.isRemoveBackground());

        if (detailLevelComboBox != null) {
            detailLevelComboBox.setValue(config.getDetailLevel());
        }
        if (config.getColorMode() == ColorMode.UNLIMITED) {
            if (unlimitedColorRadio != null) unlimitedColorRadio.setSelected(true);
        } else {
            if (customColorRadio != null) customColorRadio.setSelected(true);
        }
        if (customColorSpinner != null && customColorSpinner.getValueFactory() != null) {
            customColorSpinner.getValueFactory().setValue(config.getCustomColorCount());
        }
        if (removeBackgroundCheckBox != null) {
            removeBackgroundCheckBox.setSelected(config.isRemoveBackground());
        }
    }
}
