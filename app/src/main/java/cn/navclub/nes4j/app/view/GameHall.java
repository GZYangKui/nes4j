package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.control.LoadingPane;
import cn.navclub.nes4j.app.event.DragEventHandler;
import cn.navclub.nes4j.app.model.GTreeItem;
import cn.navclub.nes4j.app.service.LoadingService;
import cn.navclub.nes4j.app.service.TaskService;
import cn.navclub.nes4j.app.config.EventBusAddress;
import cn.navclub.nes4j.app.control.GameTray;
import cn.navclub.nes4j.app.dialog.DNesHeader;
import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.util.UIUtil;
import cn.navclub.nes4j.bin.eventbus.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
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

    private final Stage stage;

    @FXML
    private FlowPane flowPane;
    @FXML
    private TreeItem<String> rootItem;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private LoadingPane<List<File>> loadingPane;

    private TaskService<List<File>> taskService;

    public GameHall(Stage stage) {

        this.stage = stage;
        this.stage.setWidth(1200);
        this.stage.setHeight(900);
        this.stage.setTitle("nes4j");
        this.stage.setScene(new Scene(FXResource.loadFXML(this)));
        this.stage.initStyle(StageStyle.UNDECORATED);
        this.stage.getIcons().add(FXResource.loadImage("nes4j.png"));


        this.loadingPane.setService(new LoadingService<>() {
            @Override
            public void preExecute() {
                GameHall.this.flowPane.getChildren().clear();
            }

            @Override
            @SuppressWarnings("all")
            public List<File> execute(Object... params) {
                var path = Path.of(OSUtil.workstation().toString(), params[0].toString());
                var file = path.toFile();
                if (!file.exists() || file.listFiles() == null) {
                    return List.of();
                }
                return Arrays
                        .stream(file.listFiles()).filter(File::isFile)
                        .filter(it -> it.getName().endsWith(".nes"))
                        .toList();
            }

            @Override
            public void onSuccess(List<File> files) {
                var list = files.stream().map(GameTray::new).toList();
                GameHall.this.flowPane.getChildren().addAll(list);
            }
        });


        this.treeView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> this.loadingPane.load(newValue.getValue()));

        INes.eventBus.listener(INES_OPEN_GAME, this::requestRun);

        DragEventHandler.register(this.flowPane, new DragEventHandler.FileDragEventService(".nes", true) {
            @Override
            public void after(Dragboard board) {
                var item = GameHall.this.treeView.getSelectionModel().getSelectedItem();
                if (item == null) {
                    return;
                }
                var list = OSUtil.move(((GTreeItem) item).getFile(), board.getFiles());
                if (list.size() > 0) {
                    GameHall.this.loadingPane.load(item.getValue());
                }
            }
        }, TransferMode.COPY, TransferMode.MOVE);

        this.stage.show();

        this.loadAssort();
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
        var file = OSUtil.workstation().toFile();
        var list = file.listFiles();
        if (list == null) {
            return;
        }
        rootItem.getChildren().addAll(
                Arrays.stream(list).filter(File::isDirectory).map(GTreeItem::new).toList()
        );
        this.treeView.getSelectionModel().select(0);
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
    public void mkdirs() {
        final String str;
        {
            var optional = UIUtil.textFieldDialog("nes4j.assort.name");
            if (optional.isEmpty() || StrUtil.isBlank(optional.get())) {
                return;
            }
            str = optional.get();
        }
        var optional = OSUtil.mkdirAssort(str);
        if (optional.isEmpty()) {
            return;
        }
        this.rootItem.getChildren().add(new GTreeItem(optional.get()));
    }
}
