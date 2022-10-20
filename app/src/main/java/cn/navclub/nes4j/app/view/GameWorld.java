package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.FXResource;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.JoyPad;
import cn.navclub.nes4j.bin.core.NESFile;
import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.screen.Frame;
import cn.navclub.nes4j.bin.screen.Render;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class GameWorld extends Stage {
    private final Frame frame;
    private final Canvas canvas;
    private final GraphicsContext ctx;
    private final BorderPane viewpoint;
    private NES instance;


    public GameWorld(final File file) {
        this.frame = new Frame();
        this.canvas = new Canvas();
        this.viewpoint = new BorderPane();
        this.ctx = canvas.getGraphicsContext2D();

        var menuBar = new MenuBar();
        var emulator = new Menu("Emulator");

        var pausePlay = new MenuItem("Pause/Play");
        var softRest = new MenuItem("Rest(Soft)");
        var hardwareRest = new MenuItem("Reset(Hardware)");

        menuBar.getMenus().add(emulator);
        emulator.getItems().addAll(pausePlay, softRest, hardwareRest);

        this.viewpoint.setTop(menuBar);
        this.viewpoint.setCenter(canvas);
        this.viewpoint.heightProperty().addListener(
                (observable, oldValue, newValue) -> canvas.setHeight(newValue.doubleValue() - menuBar.getHeight()));
        canvas.widthProperty().bind(viewpoint.widthProperty());


        this.setWidth(900);
        this.setHeight(600);
        this.setScene(new Scene(viewpoint));
        this.setTitle(file.getName().substring(0, file.getName().indexOf(".")));
        this.getScene().getStylesheets().add(FXResource.loadStyleSheet("common.css"));
        this.show();

        this.setOnCloseRequest(event -> {
            if (this.instance == null) {
                return;
            }
            this.instance.setStop(true);
        });

        this.execute(file);
    }


    private void execute(File file) {
        CompletableFuture.runAsync(() -> {
            this.instance = NES.NESBuilder
                    .newBuilder()
                    .file(file)
                    .gameLoopCallback(this::gameLoopCallback)
                    .errorHandler(t -> {
                        t.printStackTrace();
                        return true;
                    })
                    .build();
            this.instance.execute();
        }).whenComplete((r, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
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
            var offset = i * w * 3;
            for (int j = 0; j < w; j++) {
                arr[0] = frame.getPixels()[offset];
                arr[1] = frame.getPixels()[offset + 1];
                arr[2] = frame.getPixels()[offset + 2];
                writer.setPixels(j, i, 1, 1, format, ByteBuffer.wrap(arr), 0);
                offset += 3;
            }
        }
        frame.clear();
        Platform.runLater(() -> {
            this.ctx.setFill(Color.BLACK);
            this.ctx.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            this.ctx.drawImage(image, 0, 0);
        });
    }
}
