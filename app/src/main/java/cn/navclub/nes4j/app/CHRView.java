package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.control.Tile;
import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.bin.NESFile;
import cn.navclub.nes4j.bin.util.PatternTableUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * ch-rom数据可视化查看程序
 */
public class CHRView extends Application {
    private static final Color TRANSPARENT = new Color(102 / 255.0, 102 / 255.0, 102 / 255.0, .5);

    private FlowPane flowPane;

    @Override
    public void start(Stage stage) {

        this.flowPane = new FlowPane();

        this.flowPane.setHgap(10);
        this.flowPane.setVgap(10);
        this.flowPane.setAlignment(Pos.CENTER);


        var scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(this.flowPane);
        scrollPane.setPadding(new Insets(10));

        this.flowPane.prefWidthProperty().bind(scrollPane.widthProperty());

        var menuBar = new MenuBar();

        var menu = new Menu("File");
        var open = new MenuItem("Open");
        var setting = new MenuItem("Setting");
        open.setOnAction(e -> {
            var optional = OSUtil.chooseFile(stage, "NES rom file", "*.nes", "*.NES");
            if (optional.isEmpty()) {
                return;
            }
            this.loadNESFile(optional.get());
        });
        menu.getItems().addAll(open, setting);
        menuBar.getMenus().add(menu);

        var root = new BorderPane();

        root.setTop(menuBar);
        root.setCenter(scrollPane);

        var scene = new Scene(root);
        scene.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            var board = event.getDragboard();
            var list = board
                    .getFiles()
                    .stream()
                    .filter(it -> it.getName().endsWith(".nes"))
                    .toList();
            if (list.isEmpty()) {
                return;
            }
            this.loadNESFile(list.get(0));
            event.consume();
        });
        stage.setWidth(600);
        stage.setHeight(800);
        stage.setScene(scene);
        stage.setTitle("CHView");
        stage.getIcons().add(FXResource.loadImage("bin.png"));
        scene.getStylesheets().add(FXResource.loadStyleSheet("common.css"));

        stage.show();
    }

    public void loadNESFile(File file) {
        //Clear already exist tiles
        this.flowPane.getChildren().clear();
        var future = CompletableFuture.supplyAsync(() -> new NESFile(file));
        future.whenComplete((nesFile, t) -> {
            if (t != null) {
                t.printStackTrace();
                return;
            }
            var ch = nesFile.getCh();
            var len = ch.length / 16;
            for (int i = 0; i < len; i++) {
                var k = i * 16;
                var arr = new byte[0x10];
                System.arraycopy(ch, k, arr, 0, 0x10);
                try {
                    this.renderTile(PatternTableUtil.tiles(arr), i);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    public void renderTile(byte[][] tile, int index) {
        var w = 15;
        var h = 15;
        var image = new WritableImage(w * 8, h * 8);
        var writer = image.getPixelWriter();
        var pixelFormat = PixelFormat.getByteBgraInstance();
        for (int i = 0; i < tile.length; i++) {
            for (int j = 0; j < tile[i].length; j++) {
                var value = tile[i][j];
                var color = switch (value) {
                    case 1 -> Color.RED;
                    case 2 -> Color.GREEN;
                    case 3 -> Color.BLUE;
                    default -> TRANSPARENT;
                };
                var arr = new byte[w * h];
                for (int k = 0; k < (w * h); k++) {
                    arr[k] = (byte) Math.round(color.getBlue() * 0xff);
                    arr[k + 1] = (byte) Math.round(color.getGreen() * 0xff);
                    arr[k + 2] = (byte) Math.round(color.getRed() * 0xff);
                    arr[k + 3] = (byte) Math.round(color.getOpacity() * 0xff);
                    k += 4;
                }
                writer.setPixels(j * w, i * h, w, h, pixelFormat, ByteBuffer.wrap(arr), 0);
            }
        }
        Platform.runLater(() -> this.flowPane.getChildren().add(new Tile(image, index)));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
