package com.vectorconvert.service;

import javafx.scene.Scene;


public class ThemeManager {

    public enum Theme {
        LIGHT("/com/vectorconvert/css/theme-light.css"),
        DARK("/com/vectorconvert/css/theme-dark.css");

        private final String cssPath;

        Theme(String cssPath) {
            this.cssPath = cssPath;
        }

        public String getCssPath() {
            return cssPath;
        }
    }

    private Theme currentTheme = Theme.LIGHT;

    public Theme getCurrentTheme() {
        return currentTheme;
    }


    public void toggleTheme(Scene scene) {
        if (scene == null) return;
        Theme nextTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyTheme(scene, nextTheme);
    }


    public void applyTheme(Scene scene, Theme theme) {
        if (scene == null || theme == null) return;

        scene.getStylesheets().removeIf(url -> 
            url.contains("theme-light.css") || url.contains("theme-dark.css")
        );

        
        String themeUrl = getClass().getResource(theme.getCssPath()).toExternalForm();
        scene.getStylesheets().add(themeUrl);
        this.currentTheme = theme;
    }
}
