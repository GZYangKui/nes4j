package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.INes;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class SearchTextField extends HBox {
    private final Label prefix;
    private final TextField textField;

    public SearchTextField() {
        this.prefix = new Label();
        this.textField = new TextField();
        this.prefix.getStyleClass().add("prefix");
        this.textField.setFocusTraversable(false);
        this.textField.setPromptText(INes.localeValue("nes4j.search", true));

        HBox.setHgrow(this.textField, Priority.ALWAYS);

        this.getStyleClass().add("search-text-field");

        this.getChildren().addAll(this.prefix, this.textField);
    }
}
