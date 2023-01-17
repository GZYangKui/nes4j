package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.concurrent.TaskService;
import cn.navclub.nes4j.app.config.EventBusAddress;
import cn.navclub.nes4j.app.control.GameTray;
import cn.navclub.nes4j.app.dialog.DNesHeader;
import cn.navclub.nes4j.bin.eventbus.Message;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * Visible Game wall
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class GameHall {
    public static final String INES_OPEN_GAME = "ines-open-game";

    @FXML
    private FlowPane flowPane;
    @FXML
    private ListView<String> listView;

    private final Stage stage;

    private TaskService<List<File>> taskService;

    public GameHall(Stage stage) {
        Scene scene = new Scene(FXResource.loadFXML(this));

        this.stage = stage;
        this.stage.setWidth(1200);
        this.stage.setHeight(900);
        this.stage.setTitle("nes4j");
        this.stage.setScene(scene);
        this.stage.initStyle(StageStyle.UNDECORATED);
        this.stage.getIcons().add(FXResource.loadImage("nes4j.png"));
        this.stage.show();

        this.loadAssort();

        this.listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (taskService != null)
                taskService.cancel();

            this.flowPane.getChildren().clear();

            taskService = TaskService.execute(this.loadGameList(newValue));
            taskService.setOnSucceeded(event -> {
                @SuppressWarnings("all")
                var files = (List<File>) event.getSource().getValue();
                var list = files.stream().map(GameTray::new).toList();
                this.flowPane.getChildren().addAll(list);
            });
        });

        if (!this.listView.getItems().isEmpty())
            this.listView.getSelectionModel().select(0);

        INes.eventBus.listener(INES_OPEN_GAME, this::requestRun);
    }

    private boolean requestRun(Message<File> message) {
        var file = message.body();
        var header = new DNesHeader(message.body(), this.stage);
        var execute = header.showAndWait().orElse(false);
        if (execute) {
            GameWorld.run(file);
        }
        return execute;
    }

    private void loadAssort() {
        var file = new File("nes");
        var list = file.listFiles();
        if (list == null) {
            return;
        }
        for (File item : list) {
            if (item.isFile())
                continue;
            this.listView.getItems().add(item.getName());
        }
    }


    @SuppressWarnings("all")
    private Task<List<File>> loadGameList(String assort) {
        return new Task<>() {
            @Override
            protected List<File> call() {
                var path = Path.of("nes", assort);
                var file = path.toFile();
                if (!file.exists() || file.listFiles() == null) {
                    return List.of();
                }
                return Arrays
                        .stream(file.listFiles()).filter(File::isFile)
                        .filter(it -> it.getName().endsWith(".nes"))
                        .toList();
            }
        };
    }

    /**
     * <p>
     * Open current project open source address.If user location in China will visit <a href="gitee.com">gitee</a>,
     * otherwise visit <a href="https://github.com">github</a>.</p>
     */
    @FXML
    public void github() {
        final String uri;
        var tid = TimeZone.getDefault().getID();
        if (tid.toLowerCase().contains("shanghai")) {
            uri = "https://gitee.com/navigatorcode/nes4j";
        } else {
            uri = "https://github.com/GZYangKui/nes4j";
        }
        INes.eventBus.publish(EventBusAddress.OPEN_URI, uri);
    }

    @FXML
    public void exit() {
        Platform.exit();
    }

    @FXML
    public void iconified() {
        this.stage.setIconified(true);
    }

    @FXML
    public void createResource() {
    }
}
