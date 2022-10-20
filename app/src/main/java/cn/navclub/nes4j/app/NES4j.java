package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.app.view.GameWorld;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class NES4j extends Application {

    @Override
    public void start(Stage stage) {

        var root = new BorderPane();
        var menuBar = new MenuBar();
        var menu = new Menu("File");
        var open = new MenuItem("Open");
        var listView = new ListView<String>();
        open.setOnAction(e -> {
            var optional = OSUtil.chooseFile(stage, "NES rom file", "*.nes", "*.NES");
            if (optional.isEmpty()) {
                return;
            }
            new GameWorld(optional.get());
        });
        menu.getItems().add(open);
        menuBar.getMenus().add(menu);
        root.setTop(menuBar);
        root.setCenter(listView);
        stage.setScene(new Scene(root));
        stage.setWidth(400);
        stage.setHeight(900);
        stage.setTitle("nes4j");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
