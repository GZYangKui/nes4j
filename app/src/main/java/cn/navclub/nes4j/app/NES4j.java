package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.config.NESConfig;
import cn.navclub.nes4j.app.control.NesGameItem;
import cn.navclub.nes4j.app.dialog.DHandle;
import cn.navclub.nes4j.app.util.JsonUtil;
import cn.navclub.nes4j.app.util.StrUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
public class NES4j extends Application {
    private static final String DEFAULT_CONFIG_PATH = "config/config.json";

    private ListView<NesGameItem> listView;

    public static NESConfig config;

    @Override
    public void start(Stage stage) {
        this.listView = new ListView<>();

        var menuBar = new MenuBar();
        var menu = new Menu("File");
        var game = new Menu("Game");

        var input = new MenuItem("Handle");
        input.setOnAction(event -> {
            var handle = new DHandle(config.getMapper());
            var optional = handle.showAndWait();
            if (optional.isEmpty()) {
                return;
            }
            config.setMapper(optional.get());
            config.save();
        });
        game.getItems().add(input);
        menuBar.getMenus().addAll(menu, game);

        var root = new BorderPane();

        root.setTop(menuBar);
        root.setCenter(listView);

        var scene = new Scene(root);

        scene.getStylesheets().add(FXResource.loadStyleSheet("Nes4j.css"));

        stage.setWidth(400);
        stage.setHeight(900);
        stage.setScene(scene);
        stage.setTitle("nes4j");
        stage.setResizable(false);
        stage.getIcons().add(FXResource.loadImage("icon.png"));
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

    public static void main(String[] args) throws Exception {
        loadLocalConfig(args);

        launch(args);
    }

    /**
     * 加载本地配置文件
     */
    private static void loadLocalConfig(String[] args) throws Exception {
        var map = StrUtil.args2Map(args);
        var pathStr = map.get("--config");
        if (!StrUtil.isBlank(pathStr)) {
            var exist = Files.exists(Path.of(pathStr));
            if (!exist) {
                throw new RuntimeException("Target config file " + pathStr + " not found.");
            }
        } else {
            pathStr = DEFAULT_CONFIG_PATH;
        }
        var path = Path.of(pathStr);
        if (Files.exists(path)) {
            var jsonStr = Files.readString(path);
            config = JsonUtil.parse(jsonStr, NESConfig.class);
        } else {
            config = new NESConfig();
        }
        config.setPath(path);
        config.save();
    }
}
