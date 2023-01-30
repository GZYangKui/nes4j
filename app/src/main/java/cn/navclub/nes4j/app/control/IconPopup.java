package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.control.skin.IconPopupSkin;
import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.util.Duration;

/**
 * A icon popup implement.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class IconPopup extends PopupControl {
    private final FadeTransition transition;
    private final ObjectProperty<Image> image;

    public IconPopup() {
        this.setAutoFix(true);
        this.transition = new FadeTransition();
        this.transition.setToValue(0);
        this.transition.setFromValue(1.0);
        this.transition.setOnFinished(event -> this.hide());
        this.transition.setDuration(Duration.millis(1000));
        this.image = new SimpleObjectProperty<>(this, "image", null);

        this.setOnShown(event -> {
            this.transition.stop();
            this.transition.setNode(this.getSkin().getNode());
            this.transition.setDelay(Duration.millis(500));
            this.transition.play();
            this.calculateXY();
        });
    }

    public IconPopup(Image image) {
        this();
        this.setImage(image);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new IconPopupSkin(this);
    }

    public Image getImage() {
        return image.get();
    }

    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    public void setImage(Image image) {
        this.image.set(image);
    }

    private void calculateXY() {
        var screen = Screen.getPrimary();
        var rect = screen.getVisualBounds();

        var x = (rect.getWidth() - this.getWidth()) / 2;
        var y = (rect.getHeight() - this.getHeight()) - 10;

        this.setX(x);
        this.setY(y);
    }
}
