package cn.navclub.nes4j.app.model;

import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import lombok.Getter;

import java.io.File;

public class GTreeItem extends TreeItem<String> {
    @Getter
    private final File file;
    @SuppressWarnings("all")
    private final Label label;

    public GTreeItem(File file) {
        this.file = file;
        this.label = new Label();
        this.setGraphic(this.label);
        this.setValue(file.getName());
        this.label.getStyleClass().add("assort-folder");
    }
}
