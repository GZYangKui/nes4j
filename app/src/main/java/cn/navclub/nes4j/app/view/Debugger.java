package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.config.EventBusAddress;
import cn.navclub.nes4j.app.control.BreakLine;
import cn.navclub.nes4j.app.control.CPUControlPane;
import cn.navclub.nes4j.app.control.PPUControlPane;
import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.debug.OpenCode;
import cn.navclub.nes4j.bin.debug.OpenCodeFormat;
import cn.navclub.nes4j.bin.util.BinUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>6502 assembly code debugger tool.</p>
 * <b>note:</b> This may cause memory explosion. Non developers should not use this function.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class Debugger extends Stage implements cn.navclub.nes4j.bin.debug.Debugger {

    private final Map<Integer, Integer> map;
    private final Map<Integer, Void> debuggers;

    @FXML
    private CPUControlPane controlPane;
    @FXML
    private ListView<BreakLine> listView;
    @FXML
    private PPUControlPane ppuControlPane;

    private NesConsole console;
    private boolean stepInto;
    private volatile BreakLine currentLine;

    public Debugger(final GameWorld owner) {
        this.map = new HashMap<>();
        this.debuggers = new HashMap<>();

        var scene = new Scene(FXResource.loadFXML(this));

        this.setHeight(900);
        this.setScene(scene);
        this.initOwner(owner);
        this.setResizable(false);
        this.setTitle(INes.localeValue("nes4j.assembler.debugger"));
        this.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue || this.console == null) {
                return;
            }
            this.console.release();
        });

        this.setOnCloseRequest(event -> owner.debugDispose());
    }

    @SuppressWarnings("all")
    @FXML
    public void stepInto() {
        this.stepInto = true;
    }

    @SuppressWarnings("all")
    @FXML
    public void stepOut() {
        if (this.console == null) {
            return;
        }
        this.console.release();
    }

    @SuppressWarnings("all")
    @FXML
    public void execute() {
        if (this.console == null) {
            return;
        }
        this.stepInto = false;
        this.console.release();
    }

    @Override
    public boolean hack(NesConsole console) {
        if (!this.isShowing()) {
            return false;
        }
        var cpu = console.getCpu();
        var programCounter = cpu.getPc();
        var debug = this.debuggers.containsKey(programCounter);
        debug = debug || this.stepInto;
        if (debug) {
            var index = this.map.get(programCounter);
            if (index != null) {
                Platform.runLater(() -> {
                    var item = this.listView.getItems().get(index);
                    if (item != this.currentLine) {
                        if (this.currentLine != null) {
                            this.currentLine.debug(false);
                        }
                        this.currentLine = item;
                        this.currentLine.debug(true);
                    }
                    this.listView.scrollTo(item);
                    this.controlPane.update(console);
                    this.ppuControlPane.update(console);
                    this.listView.getSelectionModel().select(item);
                });
            }
        } else {
            if (this.currentLine != null) {
                Platform.runLater(() -> {
                    if (this.currentLine == null) {
                        return;
                    }
                    this.currentLine.debug(false);
                    this.currentLine = null;
                });
            }
        }
        return debug;
    }

    @Override
    public void buffer(byte[] buffer) {
        if (!this.map.isEmpty())
            this.map.clear();

        var index = 0;
        var openCodes = OpenCodeFormat.formatOpenCode(buffer);
        var list = new ArrayList<BreakLine>();
        for (OpenCode openCode : openCodes) {
            this.map.put(openCode.index(), index);
            list.add(new BreakLine(this, openCode));
            index++;
        }
        Platform.runLater(() -> {
            this.listView.getItems().clear();
            this.listView.getItems().addAll(list);
        });
    }

    public void point(BreakLine line) {
        var index = line.getIndex();
        if (line.isDrag()) {
            this.debuggers.remove(index);
        } else {
            this.debuggers.put(index, null);
        }
    }

    @FXML
    private void handleVSnapshot() {
        var file = new File("vram.txt");
        BinUtil.snapshot(file, 16, this.console.getPpu().getVram(), 0);
        INes.eventBus.publish(EventBusAddress.OPEN_URI, file.toURI().toString());
    }

    @FXML
    private void handleRSnapshot() {
        var file = new File("ram.txt");
        //Snapshot cpu ram to 'ram.txt'
        BinUtil.snapshot(file, 16, this.console.getBus().getRam(), 0);
    }

    @Override
    public void inject(NesConsole console) {
        this.console = console;
    }
}
