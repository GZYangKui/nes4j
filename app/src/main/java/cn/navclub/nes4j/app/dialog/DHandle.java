package cn.navclub.nes4j.app.dialog;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Arrays;

/**
 * Visible handle config pane.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class DHandle extends Dialog<KeyMapper[]> {
    private static LoggerDelegate logger = LoggerFactory.logger(DHandle.class);

    private final VBox content;

    private final VBox center;
    private final GridPane left;
    private final GridPane right;

    private final Button topBtn;
    private final Button leftBtn;
    private final Button rightBtn;
    private final Button start;
    private final Button select;
    private final Button bottomBtn;
    private final Button x;
    private final Button y;
    private final Button a;
    private final Button b;
    private final KeyMapper[] mappers;
    private final TableView<KeyMapper> tableView;


    public DHandle(final KeyMapper[] mappers) {
        this.center = new VBox();
        this.content = new VBox();
        this.left = new GridPane();
        this.right = new GridPane();
        this.tableView = new TableView<>();

        this.mappers = new KeyMapper[mappers.length];
        for (int i = 0; i < mappers.length; i++) {
            this.mappers[i] = mappers[i].copy();
        }


        this.topBtn = new Button();
        this.leftBtn = new Button();
        this.rightBtn = new Button();
        this.bottomBtn = new Button();

        this.topBtn.getStyleClass().add("top-btn");
        this.leftBtn.getStyleClass().add("left-btn");
        this.rightBtn.getStyleClass().add("right-btn");
        this.bottomBtn.getStyleClass().add("bottom-btn");


        this.left.add(topBtn, 1, 0);
        this.left.add(leftBtn, 0, 1);
        this.left.add(bottomBtn, 1, 2);
        this.left.add(rightBtn, 2, 1);

        this.start = new Button("Start");
        this.select = new Button("Select");

        var box = new HBox();
        box.getChildren().addAll(this.start, this.select);
        this.center.getChildren().add(box);
        this.center.setAlignment(Pos.BOTTOM_CENTER);

        this.x = new Button("X");
        this.y = new Button("Y");
        this.a = new Button("A");
        this.b = new Button("B");

        var e0 = new Button("0");
        var e1 = new Button("1");

        e0.setVisible(false);
        e1.setVisible(false);

        this.right.add(this.x, 1, 0);
        this.right.add(e0, 1, 1);
        this.right.add(this.y, 0, 2);
        this.right.add(this.a, 2, 0);
        this.right.add(e1, 2, 1);
        this.right.add(this.b, 1, 2);


        this.left.getStyleClass().add("left-box");
        this.right.getStyleClass().add("right-box");
        this.center.getStyleClass().add("center-box");
        var hBox = new HBox();

        hBox.getStyleClass().add("box");
        hBox.getChildren().addAll(this.left, this.center, this.right);

        this.content.getStyleClass().add("content");
        this.content.getChildren().addAll(hBox, this.tableView);

        this.getDialogPane().setContent(this.content);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        this.getDialogPane().getStylesheets().add(FXResource.loadStyleSheet("DHandle.css"));

        this.setResultConverter(btn -> {
            if (btn == null || btn == ButtonType.CANCEL) {
                return null;
            }
            return this.mappers;
        });

        this.initTableView();

        this.setTitle(INes.localeValue("nes4j.handle", true));
    }

    private void initTableView() {
        TableColumn<KeyMapper, String> key = new TableColumn<>("Button");
        TableColumn<KeyMapper, String> value = new TableColumn<>("KeyCode");
        key.setCellValueFactory(new PropertyValueFactory<>("button"));
        value.setCellValueFactory(new PropertyValueFactory<>("keyCode"));

        this.tableView.setOnKeyReleased(event -> {
            var keyMapper = this.tableView.getSelectionModel().getSelectedItem();
            if (keyMapper == null) {
                return;
            }
            var optional = Arrays.stream(this.mappers)
                    .filter(it -> it != keyMapper && it.getKeyCode() == event.getCode()).findAny();
            if (optional.isPresent()) {
                logger.warning("Hot key conflict");
                return;
            }
            keyMapper.setKeyCode(event.getCode());
            this.tableView.refresh();
        });

        this.tableView.setSortPolicy(it -> null);
        this.tableView.getItems().addAll(this.mappers);
        this.tableView.getColumns().add(key);
        this.tableView.getColumns().add(value);
        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}
