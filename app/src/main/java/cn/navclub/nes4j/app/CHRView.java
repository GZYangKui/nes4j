package cn.navclub.nes4j.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class CHRView extends Application {
    private Canvas canvas;

    @Override
    public void start(Stage stage) throws Exception {
        this.canvas = new Canvas();
        var root = new BorderPane();
        root.setCenter(this.canvas);

        var scene = new Scene(root);
        scene.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
        scene.setOnDragEntered(event -> {
            var board = event.getDragboard();
            var list = board
                    .getFiles()
                    .stream()
                    .filter(it -> it.getName().matches("(\\w)*.nes"))
                    .toList();
            if (list.isEmpty()) {
                return;
            }
        });
        stage.setWidth(400);
        stage.setHeight(600);
        stage.setScene(scene);
        stage.setTitle("ch-view");

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
