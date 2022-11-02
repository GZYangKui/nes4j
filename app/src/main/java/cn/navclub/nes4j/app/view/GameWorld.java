package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.FXResource;
import cn.navclub.nes4j.app.dialog.DPalette;
import cn.navclub.nes4j.app.event.GameEventWrap;
import cn.navclub.nes4j.app.util.UIUtil;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.JoyPad;
import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.screen.Frame;
import cn.navclub.nes4j.bin.screen.Render;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class GameWorld extends Stage {
    private final Frame frame;
    private final Canvas canvas;
    private final GraphicsContext ctx;
    private NES instance;
    //记录最后一帧时间戳
    private long lastFrameTime;
    private final Render render;
    //记录1s内帧数
    private int frameCounter;
    private final Label frameLabel;
    private final MenuBar menuBar;
    //使用队列模式在GameLoop和UILoop之间共享事件
    private final BlockingQueue<GameEventWrap> eventQueue;


    public GameWorld(final File file) {
        this.frame = new Frame();
        this.render = new Render();
        this.canvas = new Canvas();
        this.menuBar = new MenuBar();
        this.frameLabel = new Label("fps:0");
        this.ctx = canvas.getGraphicsContext2D();
        this.eventQueue = new LinkedBlockingDeque<>();


        var view = new Menu("View");
        var emulator = new Menu("Emulator");

        var softRest = new MenuItem("Rest(Soft)");
        var pausePlay = new MenuItem("Pause/Play");
        var palette = new MenuItem("System Palette");
        var hardwareRest = new MenuItem("Reset(Hardware)");


        palette.setOnAction(this::systemPalette);

        view.getItems().addAll(palette);
        emulator.getItems().addAll(pausePlay, softRest, hardwareRest);

        menuBar.getMenus().addAll(emulator, view);

        var stackPane = new StackPane();

        this.canvas.widthProperty().bind(stackPane.widthProperty());
        this.canvas.heightProperty().bind(stackPane.heightProperty());

        StackPane.setAlignment(this.frameLabel, Pos.TOP_LEFT);

        this.frameLabel.setTextFill(Color.RED);
        this.frameLabel.setFont(Font.font(20));
        this.frameLabel.setPadding(new Insets(10, 10, 0, 0));

        stackPane.getChildren().addAll(this.canvas, this.frameLabel);

        var viewpoint = new BorderPane();

        viewpoint.setTop(menuBar);
        viewpoint.setCenter(stackPane);

        this.setWidth(900);
        this.setHeight(600);
        this.setResizable(false);
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

        this.getScene().addEventHandler(KeyEvent.ANY, event -> {
            var code = event.getCode();
            var eventType = event.getEventType();
            if (!(eventType == KeyEvent.KEY_PRESSED || eventType == KeyEvent.KEY_RELEASED)) {
                return;
            }
            try {
                if (code == KeyCode.A) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_A));
                }
                if (code == KeyCode.S) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_B));
                }
                if (code == KeyCode.DOWN) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_DN));
                }
                if (code == KeyCode.UP) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_UP));
                }
                if (code == KeyCode.SPACE) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_SE));
                }
                if (code == KeyCode.ENTER) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_ST));
                }
                if (code == KeyCode.LEFT) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_LF));
                }
                if (code == KeyCode.RIGHT) {
                    this.eventQueue.put(new GameEventWrap(eventType, JoyPad.JoypadButton.BTN_RT));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void systemPalette(ActionEvent event) {
        var dialog = new DPalette(this.render.getSysPalette());
        var buttonType = dialog.showAndWait().orElse(null);
        //Restore system palette
        if (buttonType == null || buttonType == ButtonType.CANCEL) {
            dialog.restore(this.render.getSysPalette());
        }
    }


    private void execute(File file) {
        CompletableFuture.runAsync(() -> {
            this.instance = NES.NESBuilder
                    .newBuilder()
                    .file(file)
                    .gameLoopCallback(this::gameLoopCallback)
                    .errorHandler(t -> {
                        t.printStackTrace();
                        UIUtil.showError(t, null, it -> this.close());
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
        this.render.render(ppu, this.frame);

        var wPixel = 3;
        var hPixel = 3;
        var w = frame.getWidth();
        var h = frame.getHeight();
        var image = new WritableImage(w * wPixel, h * hPixel);
        var arr = new byte[3 * wPixel * hPixel];
        var writer = image.getPixelWriter();
        var format = PixelFormat.getByteRgbInstance();
        for (int i = 0; i < h; i++) {
            var offset = i * w * 3;
            for (int j = 0; j < w; j++) {
                for (int k = 0; k < wPixel * hPixel; k = k + 3) {
                    arr[k] = frame.getPixels()[offset];
                    arr[k + 1] = frame.getPixels()[offset + 1];
                    arr[k + 2] = frame.getPixels()[offset + 2];
                }
                writer.setPixels(j * wPixel, i * hPixel, wPixel, hPixel, format, ByteBuffer.wrap(arr), 0);
                offset += 3;
            }
        }
        var event = eventQueue.poll();
        if (event != null) {
            joyPad.updateBtnStatus(event.btn(), event.event() == KeyEvent.KEY_PRESSED);
        }
        frame.clear();
        Platform.runLater(() -> {

            //动态调整窗口大小
            this.setWidth(image.getWidth() / 2);
            this.setHeight(image.getHeight() + this.menuBar.getHeight());


            var width = this.getWidth();
            var height = this.canvas.getHeight();

            this.ctx.clearRect(0, 0, width, height);
            this.ctx.fillRect(0, 0, width, height);
            this.ctx.drawImage(image, 0, 0);

            var nanoTime = System.nanoTime();
            if (this.lastFrameTime == 0) {
                lastFrameTime = nanoTime;
            }
            if (nanoTime - this.lastFrameTime >= 1e9) {
                this.lastFrameTime = 0;
                this.frameLabel.setText("fps:" + frameCounter);
                this.frameCounter = 0;
            } else {
                this.frameCounter++;
            }
        });
    }
}
