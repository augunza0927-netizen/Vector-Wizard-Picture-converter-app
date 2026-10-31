package com.vectorconvert;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class VectorConvertApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vectorconvert/view/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1080, 720);
        // Base and default light theme stylesheets will be attached here
        scene.getStylesheets().add(getClass().getResource("/com/vectorconvert/css/base.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/com/vectorconvert/css/theme-light.css").toExternalForm());

        try (var iconStream = getClass().getResourceAsStream("/com/vectorconvert/Image/Wizard Logo.jpg")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());
        }

        primaryStage.setTitle("VectorWizard");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
