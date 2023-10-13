package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.model.GTreeItem;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.util.UIUtil;
import cn.navclub.nes4j.app.view.GameHall;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GameTray extends VBox {
    private static final Image DEFAULT_IMAGE;
    private final static GTCMenuProvider GTC_MENU_PROVIDER;

    static {
        GTC_MENU_PROVIDER = new GTCMenuProvider();
        DEFAULT_IMAGE = FXResource.loadImage("game.png");
    }

    private final File file;
    private final Label label;


    public GameTray(File file) {
        this.file = file;
        var icon = new ImageView(DEFAULT_IMAGE);
        this.label = new Label(StrUtil.getFileName(file));
        
        this.getStyleClass().add("game-tray");
        this.getChildren().addAll(icon, this.label);

        this.setOnMouseClicked(event -> {
            var btn = event.getButton();
            if (btn == MouseButton.PRIMARY) {
                this.run();
            } else {
                GTC_MENU_PROVIDER.showOnTray(this, 0, 0);
            }
        });
    }

    public void run() {
        INes.eventBus.publish(GameHall.INES_OPEN_GAME, this.file);
    }

    public void delete() {
        var title = String.format(INes.localeValue("nes4j.game.delete"), this.label.getText());
        if (UIUtil.confirm(title)) {
            try {
                Files.delete(this.file.toPath());
                var pane = (Pane) (this.getParent());
                pane.getChildren().remove(this);
            } catch (IOException e) {
                UIUtil.showError(e, "", null);
            }
        }
    }
}
