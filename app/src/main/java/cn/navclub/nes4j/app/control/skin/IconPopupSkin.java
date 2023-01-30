package cn.navclub.nes4j.app.control.skin;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.control.IconPopup;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;


public class IconPopupSkin implements Skin<IconPopup> {
    private VBox node;
    @SuppressWarnings("all")
    private final ImageView icon;
    private final IconPopup popup;

    public IconPopupSkin(IconPopup popup) {
        this.popup = popup;
        this.node = new VBox();
        this.icon = new ImageView();

        this.icon.imageProperty().bind(popup.imageProperty());

        this.node.getChildren().add(this.icon);
        this.node.getStyleClass().add("text-popup");
        this.node.getStylesheets().add(FXResource.loadStyleSheet("TextPopup.css"));
    }

    @Override
    public IconPopup getSkinnable() {
        return this.popup;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public void dispose() {
        this.node = null;
        this.icon.imageProperty().unbind();
    }
}
