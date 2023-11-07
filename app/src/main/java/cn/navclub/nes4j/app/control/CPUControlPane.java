package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.util.BinUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.ByteArrayOutputStream;

public class CPUControlPane extends Tab {
    private static final LoggerDelegate log = LoggerFactory.logger(CPUControlPane.class);
    private final TextField a;
    private final TextField x;
    private final TextField y;
    private final TextField cycles;
    private final TextField instructions;
    private final TextField pc;

    private final CheckBox N;
    private final CheckBox V;
    private final CheckBox U;
    private final CheckBox B;
    private final CheckBox D;
    private final CheckBox I;
    private final CheckBox Z;
    private final CheckBox C;

    private final Label stackFlag;

    private final TextArea stackPane;


    public CPUControlPane() {
        var content = new VBox();

        var gridPane = new GridPane();
        var gridPane0 = new GridPane();

        this.a = new TextField();
        this.x = new TextField();
        this.y = new TextField();
        this.N = new CheckBox("N");
        this.V = new CheckBox("V");
        this.U = new CheckBox("U");
        this.B = new CheckBox("B");
        this.D = new CheckBox("D");
        this.I = new CheckBox("I");
        this.Z = new CheckBox("Z");
        this.C = new CheckBox("C");

        this.pc = new TextField();
        this.cycles = new TextField();
        this.instructions = new TextField();

        var l0 = new Label("PC:");
        var l1 = new Label("A:");
        var l2 = new Label("X:");
        var l3 = new Label("Y:");

        var l4 = new Label("CPU Cycles:");
        var l5 = new Label("Instructions:");
        var l6 = new Label("Status flags:");

        gridPane.add(l0, 0, 0);
        gridPane.add(pc, 1, 0);
        gridPane.add(l1, 2, 0);
        gridPane.add(a, 3, 0);
        gridPane.add(l2, 4, 0);
        gridPane.add(x, 5, 0);
        gridPane.add(l3, 6, 0);
        gridPane.add(y, 7, 0);
        gridPane.add(l4, 0, 1);
        gridPane.add(cycles, 1, 1, 7, 1);
        gridPane.add(l5, 0, 2);
        gridPane.add(instructions, 1, 2, 7, 1);


        gridPane0.add(N, 1, 0);
        gridPane0.add(V, 2, 0);
        gridPane0.add(U, 3, 0);
        gridPane0.add(B, 4, 0);

        gridPane0.add(l6, 0, 1);

        gridPane0.add(D, 1, 2);
        gridPane0.add(I, 2, 2);
        gridPane0.add(Z, 3, 2);
        gridPane0.add(C, 4, 2);


        gridPane0.getStyleClass().add("status-grid");


        var vbox = new VBox();

        this.stackPane = new TextArea();
        this.stackPane.setWrapText(true);
        this.stackPane.setEditable(false);
        this.stackFlag = new Label("Stack:$fd");


        vbox.setSpacing(5);
        vbox.setPadding(new Insets(5));
        VBox.setVgrow(this.stackPane, Priority.ALWAYS);
        vbox.getChildren().addAll(this.stackFlag, this.stackPane);

        VBox.setVgrow(vbox, Priority.ALWAYS);

        content.getChildren().addAll(gridPane, gridPane0, vbox);

        this.setText("CPU");
        this.setClosable(false);
        this.setContent(content);
    }


    public void update(NesConsole console) {
        var cpu = console.getCpu();
        this.pc.setText(Long.toHexString(cpu.getPc()));
        this.x.setText(Integer.toHexString(cpu.getRx()));
        this.y.setText(Integer.toHexString(cpu.getRy()));
        this.a.setText(Integer.toHexString(cpu.getRa()));
        this.instructions.setText(Long.toString(console.getIns()));
        this.stackFlag.setText("Stack:$%s".formatted(Integer.toHexString(cpu.getSp())));
        this.cycles.setText(String.format("%d(+%d)", console.getCycles(), console.getDcycle()));

        var length = 0xff - cpu.getSp();
        if (length > 0) {
            try (var buffer = new ByteArrayOutputStream()) {
                var offset = CPU.STACK + cpu.getSp() + 1;
                BinUtil.snapshot(buffer, 16, console.getBus().getRam(), offset, length);
                this.stackPane.setText(buffer.toString());
            } catch (Exception e) {
                log.fatal("Snapshot cpu stack memory view fail.", e);
            }
        }

        // Init cpu status flag value
        var arr = new CheckBox[]{
                this.C,
                this.Z,
                this.I,
                this.D,
                this.B,
                this.U,
                this.V,
                this.N
        };
        var value = cpu.getStatus();
        for (int i = 0; i < 8; i++) {
            arr[i].setSelected(((value >>> i) & 0x01) == 1);
        }
    }

    public void reset() {
        this.x.setText(null);
        this.y.setText(null);
        this.a.setText(null);
        this.pc.setText(null);
        this.cycles.setText(null);
    }
}
