package org.example.triharf;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.triharf.utils.SoundManager;

import java.io.IOException;

public class

HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Initialise le SoundManager
        // new SoundManager();

        // Charge le menu principal
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/main_menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("TriHarf - Baccalauréat+");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}