package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.FXResource;
import cn.navclub.nes4j.app.NES4J;
import cn.navclub.nes4j.app.control.BreakLine;
import cn.navclub.nes4j.app.control.CPUControlPane;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.Bus;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.debug.OpenCode;
import cn.navclub.nes4j.bin.debug.OpenCodeFormat;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DebuggerView extends Stage implements Debugger {
    private final ListView<BreakLine> listView;
    private final Map<Integer, Integer> breaks;
    private final CPUControlPane controlPane;
    private NES instance;

    public DebuggerView(final Window owner) {
        var run = new Button();
        var topBox = new HBox();
        var tabPane = new TabPane();
        this.breaks = new HashMap<>();
        this.listView = new ListView<>();
        var borderPane = new BorderPane();
        this.controlPane = new CPUControlPane();

        run.setTooltip(new Tooltip(NES4J.localeValue("nes4j.run")));
        run.setGraphic(new ImageView(FXResource.loadImage("run.png")));
        topBox.getStyleClass().add("top-box");
        topBox.getChildren().add(run);

        tabPane.getTabs().add(this.controlPane);

        borderPane.setTop(topBox);
        borderPane.setCenter(tabPane);
        borderPane.setLeft(this.listView);

        run.setOnAction(event -> {
            if (this.instance == null) {
                return;
            }
            this.instance.release();
        });

        var scene = new Scene(borderPane);

        scene.getStylesheets().add(FXResource.loadStyleSheet("DebuggerView.css"));

        this.setHeight(900);
        this.setScene(scene);
        this.initOwner(owner);
        this.setResizable(false);
        this.setTitle("Debugger View");
        this.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue || this.instance == null) {
                return;
            }
            this.instance.release();
        });
    }

    @Override
    public boolean hack(Bus bus) {
        if (!this.isShowing()) {
            return false;
        }
        var cpu = bus.getCpu();
        var programCounter = cpu.getPc();
        var index = this.breaks.get(programCounter);
        if (index != null) {
            Platform.runLater(() -> {
                //Select debug line
                this.listView.getSelectionModel().select(index);
                this.controlPane.update(cpu, bus.getCycles());
            });
        }
        return index != null;
    }

    @Override
    public void buffer(byte[] buffer, int offset) {
        var openCodes = OpenCodeFormat.formatOpenCode(buffer);
        var list = new ArrayList<BreakLine>();
        for (OpenCode openCode : openCodes) {
            list.add(new BreakLine(this, openCode));
        }
        Platform.runLater(() -> this.listView.getItems().addAll(list));
    }

    public void point(BreakLine line) {
        var index = line.getIndex();
        if (line.isDrag()) {
            this.breaks.remove(index);
        } else {
            breaks.put(index, this.listView.getItems().indexOf(line));
        }
    }

    @Override
    public void inject(NES instance) {
        this.instance = instance;
    }
}
