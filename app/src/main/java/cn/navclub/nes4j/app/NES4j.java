package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.bin.core.JoyPad;
import cn.navclub.nes4j.bin.core.NES;
import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.screen.Frame;
import cn.navclub.nes4j.bin.screen.Render;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class NES4j extends Application {
    private Frame frame;
    private Canvas canvas;

    @Override
    public void start(Stage stage) {
        this.frame = new Frame();

        this.canvas = new Canvas();
        this.canvas.setWidth(500);
        this.canvas.setHeight(600);

        var root = new BorderPane();
        var menuBar = new MenuBar();
        var menu = new Menu("File");
        var open = new MenuItem("Open");
        open.setOnAction(e -> {
            var optional = OSUtil.chooseFile(stage, "NES rom file", "*.nes", "*.NES");
            if (optional.isEmpty()) {
                return;
            }
            this.execute(optional.get());
        });
        menu.getItems().add(open);
        menuBar.getMenus().add(menu);
        root.setTop(menuBar);
        root.setCenter(this.canvas);
        stage.setScene(new Scene(root));
        stage.setWidth(900);
        stage.setHeight(600);
        stage.setTitle("NES4j");
        stage.show();
    }

    private void execute(File file) {
        CompletableFuture.runAsync(() -> {
            var nes = NES.NESBuilder
                    .newBuilder()
                    .file(file)
                    .gameLoopCallback(this::gameLoopCallback)
                    .errorHandler(t -> {
                        t.printStackTrace();
                        return true;
                    })
                    .build();
            nes.execute();
        });
    }

    private void gameLoopCallback(PPU ppu, JoyPad joyPad) {
        Render.render(ppu, this.frame);
        var w = frame.getWidth();
        var h = frame.getHeight();
        var image = new WritableImage(w, h);
        var arr = new byte[3];
        var writer = image.getPixelWriter();
        var format = PixelFormat.getByteRgbInstance();
        for (int i = 0; i < h; i++) {
            var start = i * w;
            for (int j = 0; j < w; j++) {
                arr[0] = frame.getPixels()[start];
                arr[1] = frame.getPixels()[start + 1];
                arr[2] = frame.getPixels()[start + 2];
                writer.setPixels(j, i, 1, 1, format, ByteBuffer.wrap(arr), 0);
                start += 3;
            }
        }
        Platform.runLater(() -> this.canvas.getGraphicsContext2D().drawImage(image, 0, 0));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
