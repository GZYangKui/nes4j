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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class GameWorld extends Stage {
    private final Frame frame;
    private final Canvas canvas;
    private final TextArea textArea;
    private final GraphicsContext ctx;
    private final SplitPane splitPane;
    private final HBox gameViewpoint;

    private NES instance;


    public GameWorld(final File file) {
        this.frame = new Frame();
        this.canvas = new Canvas();
        this.textArea = new TextArea();
        this.gameViewpoint = new HBox();
//        this.textArea.setWrapText(true);


        this.canvas.setWidth(256);
        this.canvas.setHeight(240);
        this.ctx = this.canvas.getGraphicsContext2D();

        this.gameViewpoint.setAlignment(Pos.CENTER);
        this.gameViewpoint.getChildren().add(canvas);


        this.splitPane = new SplitPane();
        this.splitPane.getItems().add(this.gameViewpoint);
        this.splitPane.getItems().add(this.textArea);


        this.setWidth(800);
        this.setHeight(900);
        this.setTitle(file.getName());
        this.setScene(new Scene(this.splitPane));
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
            this.renderAssembly(this.instance.getFile());
            this.instance.execute();
        }).whenComplete((r, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
        });
    }

    private void renderAssembly(NESFile nesFile) {
        var rgb = nesFile.getRgb();
        var sb = new StringBuilder();
        var line = 0;
        for (int i = 0; i < nesFile.getRgbSize(); i++) {
            if (i % 16 == 0) {
                var str = Integer.toHexString(line * 16);
                var temp = new StringBuilder(str);
                var fw = 8 - str.length();
                for (int j = 0; j < fw; j++) {
                    temp.insert(0, "0");
                }
                if (i != 0) {
                    sb.append("\n");
                }
                sb.append(temp);
                sb.append(" ");
                line++;
            }
            var code = Integer.toHexString(rgb[i] & 0xff);
            code = String.format("0x%s%s", code.length() < 2 ? "0" : "", code);
            sb.append(code);
            sb.append(" ");
        }
        var size = this.textArea.getFont().getSize();
        Platform.runLater(() -> this.textArea.setText(sb.toString()));
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
        Platform.runLater(() -> this.ctx.drawImage(image, 0, 0));
    }
}
