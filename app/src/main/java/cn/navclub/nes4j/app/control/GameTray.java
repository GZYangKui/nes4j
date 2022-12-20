package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.view.GameHall;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;

public class GameTray extends VBox {
    private static final Image DEFAULT_IMAGE = FXResource.loadImage("game.png");


    private final File file;
    private final Label label;
    private final ImageView icon;


    public GameTray(File file) {
        this.file = file;
        this.icon = new ImageView(DEFAULT_IMAGE);
        this.label = new Label(StrUtil.getFileName(file));
        this.getChildren().addAll(this.icon, this.label);
        this.getStyleClass().add("game-tray");

        this.setOnMouseClicked(event -> INes.eventBus.publish(GameHall.INES_OPEN_GAME, this.file));
    }
}
