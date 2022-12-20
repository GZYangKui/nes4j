package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.assets.FXResource;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;


public class Empty extends VBox {
    private static final Image DEFAULT_IMAGE = FXResource.loadImage("empty.png");

    private final Label label;
    private final ImageView icon;

    private StringProperty text;
    private ObjectProperty<Parent> attach;
    private ListChangeListener<Node> listChangeListener;

    public Empty() {
        this.label = new Label();
        this.icon = new ImageView(DEFAULT_IMAGE);

        this.getStyleClass().add("empty");
        this.getChildren().addAll(this.icon, this.label);
    }

    private ListChangeListener<Node> listChangeListener() {
        return c -> {
            var children = this.getAttach().getChildrenUnmodifiable();
            this.setVisible(children.isEmpty());
        };
    }

    public Parent getAttach() {
        return this.attachProperty().get();
    }

    public ObjectProperty<Parent> attachProperty() {
        if (this.attach == null) {
            this.attach = new SimpleObjectProperty<>(this, "attach", null);
        }
        return attach;
    }

    public void setAttach(Parent attach) {
        var oldValue = this.getAttach();
        if (oldValue == attach) {
            return;
        }
        if (oldValue != null) {
            oldValue.getChildrenUnmodifiable().removeListener(this.listChangeListener);
        }
        this.listChangeListener = this.listChangeListener();
        attach.getChildrenUnmodifiable().addListener(this.listChangeListener);
        this.attachProperty().set(attach);
    }

    public String getText() {
        return this.textProperty().get();
    }

    public StringProperty textProperty() {
        if (this.text == null) {
            this.text = new SimpleStringProperty(this, "text", null);
        }
        return this.text;
    }

    public void setText(String text) {
        this.label.setText(text);
        this.textProperty().set(text);
    }
}
