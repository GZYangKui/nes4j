package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.audio.NativePlayer;
import cn.navclub.nes4j.app.concurrent.TaskService;
import cn.navclub.nes4j.app.event.GameEventWrap;
import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import java.nio.IntBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class GameWorld extends Stage {
    @FXML
    private Canvas canvas;
    @FXML
    private MenuBar menuBar;

    private final GraphicsContext ctx;
    private final DebuggerView debuggerView;
    //使用队列模式在GameLoop和UILoop之间共享事件
    private final BlockingQueue<GameEventWrap> eventQueue;

    private NES instance;
    private TaskService<Void> service;

    public GameWorld() {
        var borderPane = FXResource.loadFXML(this);

        this.ctx = canvas.getGraphicsContext2D();
        this.eventQueue = new LinkedBlockingDeque<>();
        this.debuggerView = new DebuggerView(this);


        this.setWidth(900);
        this.setHeight(600);
        this.setResizable(false);
        this.setScene(new Scene(borderPane));
        this.getScene().getStylesheets().add(FXResource.loadStyleSheet("common.css"));


        this.setOnCloseRequest(event -> this.dispose(null));

        this.getScene().addEventHandler(KeyEvent.ANY, event -> {
            var code = event.getCode();
            var eventType = event.getEventType();
            if (!(eventType == KeyEvent.KEY_PRESSED || eventType == KeyEvent.KEY_RELEASED)) {
                return;
            }
            for (KeyMapper keyMapper : INes.config.getMapper()) {
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

    @FXML
    public void debugger() {
        this.debuggerView.show();
    }

    public void execute(File file) {
        this.dispose(null);

        this.service = TaskService.execute(new Task<>() {
            @Override
            protected Void call() {
                GameWorld.this.instance = NES.NESBuilder
                        .newBuilder()
                        .file(file)
                        .player(NativePlayer.class)
                        .debugger(GameWorld.this.debuggerView)
                        .gameLoopCallback(GameWorld.this::gameLoopCallback)
                        .build();
                GameWorld.this.instance.execute();
                return null;
            }
        });

        service.setOnFailed(event -> this.dispose(event.getSource().getException()));

        this.show();
        this.setTitle(StrUtil.getFileName(file));
    }


    private void dispose(Throwable t) {
        if (this.service != null) {
            this.service.cancel();
            this.service = null;
        }
        if (this.instance != null) {
            this.instance.stop();
            this.instance = null;
        }
        if (t != null) {
            var dialog = new ExceptionDialog(t);
            dialog.setHeaderText(INes.localeValue("nes4j.game.error"));
            dialog.showAndWait();
        }
    }

    private void gameLoopCallback(Frame frame, JoyPad joyPad, JoyPad joyPad1) {
        var wp = 3;
        var hp = 3;
        var w = frame.getWidth();
        var h = frame.getHeight();
        var image = new WritableImage(w * wp, h * hp);
        var arr = new int[wp * hp];
        var writer = image.getPixelWriter();
        var format = PixelFormat.getIntArgbInstance();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                var pixel = frame.getPixel(y * w + x);
                for (int k = 0; k < wp * hp; k++) {
                    arr[k] = pixel;
                }
                writer.setPixels(x * wp, y * hp, wp, hp, format, IntBuffer.wrap(arr), 1);
            }
        }

        var event = eventQueue.poll();
        if (event != null) {
            joyPad.updateBtnStatus(event.btn(), event.event() == KeyEvent.KEY_PRESSED);
        }
        Platform.runLater(() -> {
            this.setWidth(image.getWidth());
            this.setHeight(image.getHeight() + this.menuBar.getHeight());
            this.ctx.fillRect(0, 0, image.getWidth(), image.getHeight());

            this.ctx.drawImage(image, 0, 0);
        });
    }
}
