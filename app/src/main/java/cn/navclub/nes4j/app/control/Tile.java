package cn.navclub.nes4j.app.control;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Tile extends VBox {
    public Tile(final Image image, int index) {
        var titleBox = new HBox();
        this.getChildren().add(titleBox);
        this.getChildren().add(new ImageView(image));
        titleBox.getChildren().add(new Label(String.valueOf(index + 1)));

        titleBox.setStyle("-fx-background-color: #fefefe");
    }
}
