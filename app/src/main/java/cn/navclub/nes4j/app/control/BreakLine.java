package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.bin.debug.OpenCode;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class BreakLine extends HBox {
    public static final String DEFAULT_STYLE_CLASS = "break-line";

    private final CheckBox box;
    private final Label address;
    private final Label instruct;
    private final Label operator;

    public BreakLine() {
        this.box = new CheckBox();
        this.address = new Label();
        this.instruct = new Label();
        this.operator = new Label();

        this.getChildren().addAll(box, address, instruct, operator);
        this.getStyleClass().add(DEFAULT_STYLE_CLASS);

        box.setOnAction(event -> {

        });
    }

    public BreakLine(OpenCode openCode) {
        this();
        this.operator.setText(openCode.operator());
        this.instruct.setText(openCode.instruction().name());
        this.address.setText(String.format(":%s:",Integer.toHexString(openCode.index())));
    }
}
