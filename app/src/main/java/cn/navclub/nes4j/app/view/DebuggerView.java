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
    private final Map<Integer, Integer> map;
    private final Map<Integer, Void> debuggers;
    private final CPUControlPane controlPane;
    private NES instance;
    private boolean stepInto;

    public DebuggerView(final Window owner) {

        var topBox = new HBox();
        var tabPane = new TabPane();
        this.map = new HashMap<>();
        this.debuggers = new HashMap<>();
        this.listView = new ListView<>();
        var borderPane = new BorderPane();
        this.controlPane = new CPUControlPane();

        var run = new Button();
        var rrun = new Button();
        var stepOut = new Button();
        var stepInto = new Button();

        rrun.setTooltip(new Tooltip("re-run"));
        stepOut.setTooltip(new Tooltip("step out"));
        stepInto.setTooltip(new Tooltip("step into"));
        run.setTooltip(new Tooltip(NES4J.localeValue("nes4j.run")));

        run.setGraphic(new ImageView(FXResource.loadImage("run.png")));
        rrun.setGraphic(new ImageView(FXResource.loadImage("rrun.png")));
        stepOut.setGraphic(new ImageView(FXResource.loadImage("stepout.png")));
        stepInto.setGraphic(new ImageView(FXResource.loadImage("stepinto.png")));

        topBox.getStyleClass().add("top-box");
        topBox.getChildren().addAll(run, stepInto, stepOut, rrun);

        tabPane.getTabs().add(this.controlPane);

        borderPane.setTop(topBox);
        borderPane.setCenter(tabPane);
        borderPane.setLeft(this.listView);


        stepOut.setOnAction(event -> {
            if (this.instance == null) {
                return;
            }
            this.instance.release();
        });

        run.setOnAction(event -> {
            if (this.instance == null) {
                return;
            }
            this.stepInto = false;
            this.instance.release();
        });

        stepInto.setOnAction((event) -> this.stepInto = true);

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
    public boolean hack(NES context) {
        if (!this.isShowing()) {
            return false;
        }
        var cpu = context.getCpu();
        var programCounter = cpu.getPc();
        var debug = this.debuggers.containsKey(programCounter);
        debug = debug || this.stepInto;
        if (debug) {
            var index = this.map.get(programCounter);
            if (index != null) {
                Platform.runLater(() -> {
                    //scroll debug line
                    this.listView.scrollTo(index);
                    //Select debug line
                    this.listView.getSelectionModel().select(index);
                    this.controlPane.update(context);
                });
            }
        }
        return debug;
    }

    @Override
    public void buffer(byte[] buffer) {
        if (this.map.size() != 0)
            this.map.clear();

        var index = 0;
        var openCodes = OpenCodeFormat.formatOpenCode(buffer);
        var list = new ArrayList<BreakLine>();
        for (OpenCode openCode : openCodes) {
            this.map.put(openCode.index(), index);
            list.add(new BreakLine(this, openCode));
            index++;
        }
        Platform.runLater(() -> this.listView.getItems().addAll(list));
    }

    public void point(BreakLine line) {
        var index = line.getIndex();
        if (line.isDrag()) {
            this.debuggers.remove(index);
        } else {
            debuggers.put(index, null);
        }
    }

    @Override
    public void inject(NES instance) {
        this.instance = instance;
    }
}
