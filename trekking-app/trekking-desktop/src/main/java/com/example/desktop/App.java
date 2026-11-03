package com.example.desktop;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.util.NavigationShell;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ConexionDB.crearTabla();

        FXMLLoader loader = new FXMLLoader(
                App.class.getResource(
                        "/com/example/fxml/salidas.fxml"
                )
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        NavigationShell.install(scene, "trips");

        stage.setTitle("Trekking App");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
