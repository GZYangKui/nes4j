package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.control.NesGameItem;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;

@Slf4j
public class NES4j extends Application {
    private ListView<NesGameItem> listView;

    @Override
    public void start(Stage stage) {
        this.listView = new ListView<>();

        var menuBar = new MenuBar();
        var menu = new Menu("File");

        menuBar.getMenus().add(menu);

        var root = new BorderPane();

        root.setTop(menuBar);
        root.setCenter(listView);

        var scene = new Scene(root);

        scene.getStylesheets().add(FXResource.loadStyleSheet("Nes4j.css"));

        stage.setWidth(400);
        stage.setHeight(900);
        stage.setScene(scene);
        stage.setTitle("nes4j");
        stage.show();

        this.loadLocalGame();
    }

    private void loadLocalGame() {
        this.listView.getItems().clear();

        var file = new File("nes");
        var subFiles = file.listFiles();
        if (subFiles == null) {
            return;
        }
        var list = Arrays
                .stream(subFiles)
                .filter(it -> !it.isDirectory() && it.getName().endsWith(".nes"))
                .map(NesGameItem::new)
                .toList();

        this.listView.getItems().addAll(list);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
