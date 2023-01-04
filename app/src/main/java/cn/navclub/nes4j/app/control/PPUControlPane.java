package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.util.BinUtil;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class PPUControlPane extends Tab {
    private final TextField x = new TextField();
    private final TextField y = new TextField();
    private final TextField ctrl = new TextField();
    private final TextField mask = new TextField();
    private final TextField status = new TextField();
    private final TextField oaddr = new TextField();
    private final TextField paddr = new TextField();
    private final TextField scanline = new TextField();


    public PPUControlPane() {
        var l0 = new Label("PPUCTRL");
        var l1 = new Label("PPUMASK");
        var l2 = new Label("PPUSTAT");
        var l3 = new Label("OAMADDR");
        var l4 = new Label("PPUADDR");
        var l5 = new Label("Scanline");
        var l6 = new Label("X Scroll");
        var l7 = new Label("Y Scroll");

        var gridPane = new GridPane();


        gridPane.add(l0, 0, 0);
        gridPane.add(ctrl, 1, 0);
        gridPane.add(l1, 2, 0);
        gridPane.add(mask, 3, 0);

        gridPane.add(l2, 0, 1);
        gridPane.add(status, 1, 1);
        gridPane.add(l3, 2, 1);
        gridPane.add(oaddr, 3, 1);

        gridPane.add(l4, 0, 2);
        gridPane.add(paddr, 1, 2);
        gridPane.add(l5, 2, 2);
        gridPane.add(scanline, 3, 2);

        gridPane.add(l6, 0, 3);
        gridPane.add(x, 1, 3);
        gridPane.add(l7, 2, 3);
        gridPane.add(y, 3, 3);


        GridPane.setHgrow(ctrl, Priority.ALWAYS);
        GridPane.setHgrow(mask, Priority.ALWAYS);
        GridPane.setHgrow(status, Priority.ALWAYS);
        GridPane.setHgrow(oaddr, Priority.ALWAYS);

        this.setText("PPU");
        this.setClosable(false);
        this.setContent(gridPane);
    }

    public void update(NES context) {
        var ppu = context.getPpu();

        this.x.setText(Integer.toString(ppu.x()));
        this.y.setText(Integer.toString(ppu.y()));
        this.scanline.setText(Long.toString(ppu.getScanline()));
        this.paddr.setText(String.format("$%s", Integer.toHexString(ppu.getV())));
        this.oaddr.setText(String.format("$%s", BinUtil.toHexStr((byte) ppu.getOamAddr())));
        this.mask.setText(String.format("$%s", BinUtil.toHexStr(ppu.getMask().getBits())));
        this.ctrl.setText(String.format("$%s", BinUtil.toHexStr(ppu.getCtr().getBits())));
        this.status.setText(String.format("$%s", BinUtil.toHexStr(ppu.getStatus())));

    }
}
