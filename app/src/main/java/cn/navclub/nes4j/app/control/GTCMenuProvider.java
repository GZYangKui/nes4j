package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.INes;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class GTCMenuProvider {
    private GameTray tray;
    private final ContextMenu context;

    public GTCMenuProvider() {
        this.context = new ContextMenu();

        var runner = new MenuItem(INes.localeValue("nes4j.run"));
        var delete = new MenuItem(INes.localeValue("nes4j.delete"));

        runner.setOnAction(event -> this.tray.run());
        delete.setOnAction(event -> this.tray.delete());

        this.context.getItems().addAll(runner, delete);
    }

    public void showOnTray(GameTray tray, int dx, int dy) {
        assert tray != null;
        this.tray = tray;
        this.context.show(tray, Side.BOTTOM, dx, dy);
    }
}
