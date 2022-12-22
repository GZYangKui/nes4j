package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.view.Debugger;
import cn.navclub.nes4j.bin.debug.OpenCode;
import cn.navclub.nes4j.bin.config.AddressMode;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.BinUtil.toHexStr;

public class BreakLine extends HBox {
    private static final String DEBUG_LINE = "debug-line";
    public static final String DEFAULT_STYLE_CLASS = "break-line";

    private final static Map<Integer, String> ALIAS = new HashMap<>();

    static {
        ALIAS.put(0x2000, "PPU_CTR");
        ALIAS.put(0x2001, "PPU_MASK");
        ALIAS.put(0x2002, "PPU_STATUS");
        ALIAS.put(0x2003, "PPU_OAM_ADDR");
        ALIAS.put(0x2005, "PPU_SCROLL");
        ALIAS.put(0x4014, "OAM_DMA");
        ALIAS.put(0x4017, "JOY2_FRAME");
        ALIAS.put(0x4010, "DMC_RREQ");
        ALIAS.put(0x2006, "PPU_ADDRESS");
        ALIAS.put(0x2007, "PPU_DATA");
    }

    @Getter
    private boolean drag;
    private final Label label;
    private final Label address;
    private final Label instruct;
    private final Label operator;
    @Getter
    private final int index;

    public BreakLine(Debugger view, int index) {
        this.index = index;
        this.label = new Label();
        this.address = new Label();
        this.instruct = new Label();
        this.operator = new Label();

        this.label.setAlignment(Pos.CENTER);
        this.label.getStyleClass().add("break-label");
        this.getChildren().addAll(label, address, instruct, operator);
        this.getStyleClass().add(DEFAULT_STYLE_CLASS);

        this.label.setOnMouseClicked(event -> {
            event.consume();
            view.point(this);
            this.label.setGraphic(null);
            if (!this.drag) {
                var w = this.label.getWidth();
                var h = this.label.getHeight();
                var min = Math.min(w, h);
                var redis = min / 2.0 / 1.5;
                var circle = new Circle(redis, Color.RED);
                this.label.setGraphic(circle);
            }
            this.drag = !drag;
        });
    }

    public BreakLine(Debugger view, OpenCode openCode) {
        this(view, openCode.index());
        var operand = openCode.operand();
        var mode = operand.mode();
        var lsb = operand.lsb();
        var msb = operand.msb();
        var value = (lsb & 0xff | (msb & 0xff) << 8);

        String text = ALIAS.get(value);

        if (StrUtil.isBlank(text)) {
            var hexStr = "$%s%s".formatted(toHexStr(msb), toHexStr(lsb));
            text = switch (mode) {
                case Accumulator -> "A";
                case Absolute, ZeroPage -> hexStr;
                case Immediate -> "$%s".formatted(toHexStr(lsb));
                case Indirect -> "(%s)".formatted(hexStr);
                case ZeroPage_Y -> "$%s,Y".formatted(toHexStr(lsb));
                case Indirect_Y -> "($%s),y".formatted(toHexStr(lsb));
                case Relative -> {
                    var address = openCode.index() + lsb;
                    var a = toHexStr(int8(address));
                    var b = toHexStr(int8(address >> 8));
                    yield "$%s%s".formatted(b, a);
                }
                case ZeroPage_X, Indirect_X -> "($%s,x)".formatted(toHexStr(lsb));
                case Absolute_X, Absolute_Y -> "%s,%s".formatted(hexStr, mode == AddressMode.Absolute_X ? "x" : "y");
                default -> "";
            };
        }
        this.operator.setText(text);
        this.instruct.setText(openCode.instruction().name());
        this.address.setText(String.format(":%s:", Integer.toHexString(openCode.index())));
    }

    public void debug(boolean b) {
        var c = this.getStyleClass().contains(DEBUG_LINE);
        if (b && !c) {
            this.getStyleClass().add(DEBUG_LINE);
        }
        if (!b) {
            this.getStyleClass().remove(DEBUG_LINE);
        }
    }
}
