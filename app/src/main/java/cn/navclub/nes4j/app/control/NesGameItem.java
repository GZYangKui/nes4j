package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.view.GameWorld;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.io.File;

public class NesGameItem extends HBox {
    private final File file;
    private final Label label;
    private final HBox leftBox;
    private final HBox rightBox;
    private final ImageView imageView;

    public NesGameItem(final File file) {
        this.file = file;
        this.leftBox = new HBox();
        this.rightBox = new HBox();
        this.label = new Label(StrUtil.getFileName(file));
        this.imageView = new ImageView(FXResource.loadImage("game.png"));

        HBox.setHgrow(this.leftBox, Priority.ALWAYS);
        this.leftBox.getChildren().addAll(this.imageView, this.label);

        this.getChildren().addAll(this.leftBox, this.rightBox);

        this.leftBox.getStyleClass().add("left-box");
        this.rightBox.getStyleClass().add("right-box");

        var delete = new Button();
        var launcher = new Button();

        delete.setTooltip(new Tooltip("删除"));
        launcher.setTooltip(new Tooltip("启动"));

        delete.setGraphic(new ImageView(FXResource.loadImage("delete.png")));
        launcher.setGraphic(new ImageView(FXResource.loadImage("launcher.png")));

        launcher.setOnAction(event -> new GameWorld(this.file));

        this.rightBox.getChildren().addAll(delete, launcher);
    }


    public File getFile() {
        return file;
    }
}
