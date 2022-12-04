package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.Nes4j;
import cn.navclub.nes4j.app.audio.NativePlayer;
import cn.navclub.nes4j.app.dialog.DPalette;
import cn.navclub.nes4j.app.event.GameEventWrap;
import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.PPU;
import cn.navclub.nes4j.bin.ppu.Frame;
import cn.navclub.nes4j.bin.ppu.Render;
import javafx.animation.AnimationTimer;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class GameWorld extends Stage {
    private final Frame frame;
    private final Canvas canvas;
    private final GraphicsContext ctx;
    private final Render render;
    private final Label frameLabel;
    private final MenuBar menuBar;
    private final AnimationTimer fpsTimer;
    private final DebuggerView debuggerView;
    //使用队列模式在GameLoop和UILoop之间共享事件
    private final BlockingQueue<GameEventWrap> eventQueue;

    private NES instance;
    //记录最后一帧时间戳
    private long lastFrameTime;
    //记录1s内帧数
    private int frameCounter;

    public GameWorld(final File file) {
        this.frame = new Frame();
        this.render = new Render();
        this.canvas = new Canvas();
        this.menuBar = new MenuBar();
        this.frameLabel = new Label();
        this.fpsTimer = this.createFPSTimer();
        this.ctx = canvas.getGraphicsContext2D();
        this.eventQueue = new LinkedBlockingDeque<>();
        this.debuggerView = new DebuggerView(this);

        var view = new Menu(Nes4j.localeValue("nes4j.view"));
        var tool = new Menu(Nes4j.localeValue("nes4j.tool"));
        var emulator = new Menu(Nes4j.localeValue("nes4j.emulator"));

        var debug = new MenuItem(Nes4j.localeValue("nes4j.debug"));
        var softRest = new MenuItem(Nes4j.localeValue("nes4j.reset"));
        var pausePlay = new MenuItem(Nes4j.localeValue("nes4j.pplay"));
        var palette = new MenuItem(Nes4j.localeValue("nes4j.palette"));

        palette.setOnAction(this::systemPalette);

        tool.getItems().add(debug);
        view.getItems().addAll(palette);
        emulator.getItems().addAll(pausePlay, softRest);

        menuBar.getMenus().addAll(emulator, view, tool);

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

        this.setOnCloseRequest(event -> this.dispose(null));

        this.fpsTimer.start();

        var thread = new Thread(this.execute(file));
        thread.setName("Nes4j-game-thread");
        thread.start();

        debug.setOnAction((event) -> this.debuggerView.show());

        this.getScene().addEventHandler(KeyEvent.ANY, event -> {
            var code = event.getCode();
            var eventType = event.getEventType();
            if (!(eventType == KeyEvent.KEY_PRESSED || eventType == KeyEvent.KEY_RELEASED)) {
                return;
            }
            for (KeyMapper keyMapper : Nes4j.config.getMapper()) {
                if (keyMapper.getKeyCode() == code) {
                    try {
                        this.eventQueue.put(new GameEventWrap(eventType, keyMapper.getButton()));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private AnimationTimer createFPSTimer() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                var that = GameWorld.this;
                var nanoTime = System.nanoTime();
                if (that.lastFrameTime == 0) {
                    lastFrameTime = nanoTime;
                }
                if ((nanoTime - that.lastFrameTime) >= 1e9) {
                    that.lastFrameTime = 0;
                    that.frameLabel.setText("fps:" + frameCounter);
                    that.frameCounter = 0;
                }
            }
        };
    }

    private void systemPalette(ActionEvent event) {
        var dialog = new DPalette(this.render.getSysPalette());
        var buttonType = dialog.showAndWait().orElse(null);
        //Restore system palette
        if (buttonType == null || buttonType == ButtonType.CANCEL) {
            dialog.restore(this.render.getSysPalette());
        }
    }


    private Runnable execute(File file) {
        return () -> {
            try {
                this.instance = NES.NESBuilder
                        .newBuilder()
                        .file(file)
                        .player(NativePlayer.class)
                        .debugger(this.debuggerView)
                        .gameLoopCallback(this::gameLoopCallback)
                        .build();
                this.instance.execute();
            } catch (Exception e) {
                Platform.runLater(() -> this.dispose(e));
            }
        };
    }

    private void dispose(Throwable t) {
        if (this.instance != null) {
            this.instance.stop();
        }
        if (t != null) {
            var dialog = new ExceptionDialog(t);
            dialog.setHeaderText(Nes4j.localeValue("nes4j.game.error"));
            dialog.showAndWait();
//            this.close();
        }
        this.fpsTimer.stop();
    }

    private void gameLoopCallback(PPU ppu, JoyPad joyPad, JoyPad joyPad1) {
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

            this.setWidth(image.getWidth() / 2);
            this.setHeight(image.getHeight() + this.menuBar.getHeight());

            var width = this.getWidth();
            var height = this.canvas.getHeight();

            this.ctx.clearRect(0, 0, width, height);
            this.ctx.fillRect(0, 0, width, height);
            this.ctx.drawImage(image, 0, 0);

            this.frameCounter++;
        });
    }
}
