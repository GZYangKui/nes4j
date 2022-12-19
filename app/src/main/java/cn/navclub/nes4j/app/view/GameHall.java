package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.event.ControlDragEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;

/**
 * 游戏大厅
 */
public class GameHall {
    @FXML
    private HBox navbar;
    @FXML
    private FlowPane flowPane;
    @FXML
    private ListView<String> listView;

    private final Stage stage;
    private final Scene scene;

    public GameHall(Stage stage) {
        this.scene = new Scene(FXResource.loadFXML(this));

        //注册拖拽事件
        ControlDragEvent.bind(navbar);

        this.stage = stage;
        this.stage.setWidth(1200);
        this.stage.setHeight(900);
        this.stage.setTitle("ines");
        this.stage.setScene(this.scene);
        this.stage.initStyle(StageStyle.UNDECORATED);
        this.stage.show();

        this.loadAssort();
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

    @FXML
    public void exit() {
        Platform.exit();
    }

    @FXML
    public void iconified() {
        this.stage.setIconified(true);
    }
}
