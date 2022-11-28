package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.view.DebuggerView;
import cn.navclub.nes4j.bin.debug.OpenCode;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.Getter;

public class BreakLine extends HBox {
    public static final String DEFAULT_STYLE_CLASS = "break-line";
    @Getter
    private boolean drag;
    private final Label label;
    private final Label address;
    private final Label instruct;
    private final Label operator;
    @Getter
    private final int index;

    public BreakLine(DebuggerView view, int index) {
        this.index = index;
        this.label = new Label();
        this.address = new Label();
        this.instruct = new Label();
        this.operator = new Label();

        label.getStyleClass().add("break-label");

        this.getChildren().addAll(label, address, instruct, operator);
        this.getStyleClass().add(DEFAULT_STYLE_CLASS);

        label.setOnMouseClicked(event -> {
            view.point(this);
            if (!this.drag)
                this.label.getStyleClass().add("break-label-drag");
            else
                this.label.getStyleClass().remove("break-label-drag");

            this.drag = !drag;
        });
    }

    public BreakLine(DebuggerView view, OpenCode openCode) {
        this(view, openCode.index());
        this.operator.setText(openCode.operator());
        this.instruct.setText(openCode.instruction().name());
        this.address.setText(String.format(":%s:", Integer.toHexString(openCode.index())));
    }
}
