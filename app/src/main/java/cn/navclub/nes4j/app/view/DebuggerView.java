package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.FXResource;
import cn.navclub.nes4j.app.control.BreakLine;
import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.debug.OpenCode;
import cn.navclub.nes4j.bin.debug.OpenCodeFormat;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ArrayList;

public class DebuggerView extends Stage implements Debugger {
    private final TabPane tabPane;
    private final ListView<BreakLine> listView;

    public DebuggerView() {
        this.tabPane = new TabPane();
        this.listView = new ListView<>();

        var borderPane = new BorderPane();
        var scene = new Scene(borderPane);

        borderPane.setLeft(this.listView);
        borderPane.setCenter(this.tabPane);

        scene.getStylesheets().add(FXResource.loadStyleSheet("DebuggerView.css"));

        this.setScene(scene);

        this.setWidth(600);
        this.setHeight(900);
        this.setTitle("Debugger View");
    }

    @Override
    public boolean debug(CPU cpu) {
        return false;
    }

    @Override
    public void buffer(byte[] buffer, int offset) {
        var openCodes = OpenCodeFormat.formatOpenCode(buffer);
        var list = new ArrayList<BreakLine>();
        for (OpenCode openCode : openCodes) {
            list.add(new BreakLine(openCode));
        }
        Platform.runLater(() -> this.listView.getItems().addAll(list));
    }
}
