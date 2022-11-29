package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.CPU;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CPUControlPane extends Tab {
    private final GridPane gridPane;
    private final GridPane gridPane0;
    private final TextField a;
    private final TextField x;
    private final TextField y;
    private final TextField cycles;
    private final TextField instructions;
    private final TextField pc;

    private final VBox leftBox;
    private final VBox rightBox;

    private final HBox content;

    public CPUControlPane() {
        this.content = new HBox();
        this.leftBox = new VBox();
        this.rightBox = new VBox();

        this.gridPane = new GridPane();
        this.gridPane0 = new GridPane();

        this.a = new TextField();
        this.x = new TextField();
        this.y = new TextField();
        this.cycles = new TextField();
        this.instructions = new TextField();
        this.pc = new TextField();

        var l0 = new Label("PC:");
        var l1 = new Label("A:");
        var l2 = new Label("X:");
        var l3 = new Label("Y:");

        var l4 = new Label("CPU Cycles:");
        var l5 = new Label("Instructions:");

        this.gridPane.add(l0, 0, 0);
        this.gridPane.add(pc, 1, 0);
        this.gridPane.add(l1, 2, 0);
        this.gridPane.add(a, 3, 0);
        this.gridPane.add(l2, 4, 0);
        this.gridPane.add(x, 5, 0);
        this.gridPane.add(l3, 6, 0);
        this.gridPane.add(y, 7, 0);
        this.gridPane.add(l4, 0, 1);
        this.gridPane.add(cycles, 1, 1, 7, 1);
        this.gridPane.add(l5, 0, 2);
        this.gridPane.add(instructions, 1, 2, 7, 1);

        this.leftBox.getChildren().add(this.gridPane);
        this.leftBox.getChildren().add(this.gridPane0);

        this.content.getChildren().addAll(this.leftBox, this.rightBox);

        this.setText("CPU");
        this.setClosable(false);
        this.setContent(this.content);
    }


    public void update(NES context) {
        var cpu = context.getCpu();
        this.pc.setText(Long.toHexString(cpu.getPc()));
        this.x.setText(Integer.toHexString(cpu.getRx()));
        this.y.setText(Integer.toHexString(cpu.getRy()));
        this.a.setText(Integer.toHexString(cpu.getRa()));
        this.cycles.setText(Long.toString(context.getCycles()));
        this.instructions.setText(Long.toString(context.getInstructions()));

    }

    public void reset() {
        this.x.setText(null);
        this.y.setText(null);
        this.a.setText(null);
        this.pc.setText(null);
        this.cycles.setText(null);
    }
}
