package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.audio.JavaXAudio;
import cn.navclub.nes4j.app.config.NESConfig;
import cn.navclub.nes4j.app.control.IconPopup;
import cn.navclub.nes4j.app.service.TaskService;
import cn.navclub.nes4j.app.dialog.DHandle;
import cn.navclub.nes4j.app.event.GameEventWrap;
import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.util.UIUtil;
import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.ppu.Frame;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class GameWorld extends Stage {
    private static final LoggerDelegate log = LoggerFactory.logger(GameWorld.class);
    @FXML
    private Label fbl;
    @FXML
    private Label timer;
    @FXML
    private HBox joyBox;
    @FXML
    private Canvas canvas;
    @FXML
    private AnchorPane stackPane;

    //Pixel scale level
    @SuppressWarnings("all")
    private final int scale;
    private final IntBuffer intBuffer;
    private final GraphicsContext ctx;
    private final WritableImage image;
    private final BlockingQueue<GameEventWrap> eventQueue;

    private NesConsole console;
    private Debugger debugger;
    private final Circle[] joyBtns;
    private TaskService<Void> service;
    private final AnimationTimer animationTimer;

    @SuppressWarnings("all")
    public GameWorld(int scale) {
        var scene = new Scene(FXResource.loadFXML(this));

        this.joyBtns = joyBox.getChildren().toArray(Circle[]::new);

        this.scale = scale;
        this.eventQueue = new LinkedBlockingDeque<>();

        this.intBuffer = IntBuffer.allocate(this.scale * this.scale);
        this.image = new WritableImage(this.scale * Frame.width, this.scale * Frame.height);

        this.ctx = canvas.getGraphicsContext2D();
        this.canvas.setWidth(this.image.getWidth());
        this.canvas.setHeight(this.image.getHeight());
        this.stackPane.heightProperty().addListener(
                (observable, oldValue, newValue) ->
                        this.setHeight(newValue.intValue() + this.image.getHeight())
        );
        //Fill default background color
        this.fillDefaultBG();
        this.setScene(scene);
        this.setResizable(false);
        this.setOnCloseRequest(event -> this.dispose(null));
        this.getScene().addEventHandler(KeyEvent.ANY, this::keyEventHandler);

        var t = System.nanoTime();
        this.animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                var span = (now - t) / 1000_000_000;
                var second = span % 60;
                var minute = span / 60 % 60;
                var hour = span / 60 / 60;
                timer.setText("%02d:%02d:%02d".formatted(hour, minute, second));
            }
        };
        this.animationTimer.start();
    }

    @SuppressWarnings("all")
    @FXML
    public void debugger() {
        if (this.debugger == null) {
            this.debugger = new Debugger(this);
            if (this.console != null) {
                this.console.setDebugger(this.debugger);
            }
        }
        this.debugger.show();
    }

    public void execute(File file) {
        this.service = TaskService.execute(new Task<>() {
            @Override
            protected Void call() {
                GameWorld.this.console = NesConsole.Builder
                        .newBuilder()
                        .file(file)
                        .player(JavaXAudio.class)
                        .gameLoopCallback(GameWorld.this::gameLoopCallback)
                        .build();
                GameWorld.this.console.execute();
                return null;
            }
        });

        service.setOnFailed(event -> this.dispose(event.getSource().getException()));

        this.show();
        this.setTitle(StrUtil.getFileName(file));
    }


    private void dispose(Throwable t) {
        this.debugDispose();

        if (this.service != null)
            this.service.cancel();

        if (this.console != null)
            this.console.stop();

        if (t != null) {
            log.fatal(INes.localeValue("nes4j.game.error"), t);
            UIUtil.showError(t, INes.localeValue("nes4j.game.error"), v -> this.close());
        }

        this.animationTimer.stop();


        this.ctx.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    public void debugDispose() {
        this.debugger = null;
        if (this.console != null)
            this.console.setDebugger(null);

        System.gc();
    }

    @FXML
    public void handle() {
        var config = NESConfig.getInstance();
        var dialog = new DHandle(config.getMapper());
        var optional = dialog.showAndWait();
        optional.ifPresent(keyMappers -> {
            config.setMapper(keyMappers);
            config.save();
        });
    }

    @FXML
    public void reset() {
        if (this.console == null) {
            return;
        }
        this.fillDefaultBG();
        this.console.SWReset();
    }

    @FXML
    public void ppuViewer() {
        if (this.console == null) {
            return;
        }
        new PPUViewer(this.console);
    }

    private void gameLoopCallback(Integer fps, boolean enableRender, Frame frame, JoyPad joyPad, JoyPad joyPad1) {
        //If render enable transport pixel to javafx image otherwise do nothing.
        if (enableRender) {
            var w = Frame.width;
            var h = Frame.height;
            var writer = image.getPixelWriter();
            var format = PixelFormat.getIntArgbInstance();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    var pixel = frame.getPixel(y * w + x);
                    for (int k = 0; k < this.scale * this.scale; k++) {
                        intBuffer.put(k, pixel);
                    }
                    writer.setPixels(x * this.scale, y * this.scale, this.scale, this.scale, format, this.intBuffer, 1);
                }
            }
        }

        //Poll keyword event
        var event = eventQueue.poll();
        if (event != null) {
            joyPad.updateBtnStatus(event.btn(), event.event() == KeyEvent.KEY_PRESSED);
        }

        Platform.runLater(() -> {
            if (enableRender) {
                this.ctx.drawImage(image, 0, 0);
            }
            var span = this.console.TVFps() - fps;
            var color = Color.GREEN;
            if (Math.abs(span) > 3) {
                color = Color.RED;
            }
            this.fbl.setTextFill(color);
            this.fbl.setText(String.format("fps:%02d", fps));
        });
    }

    private void keyEventHandler(KeyEvent event) {
        var code = event.getCode();
        var eventType = event.getEventType();
        if (!(eventType == KeyEvent.KEY_PRESSED || eventType == KeyEvent.KEY_RELEASED)) {
            return;
        }
        for (KeyMapper keyMapper : NESConfig.getInstance().getMapper()) {
            if (keyMapper.getKeyCode() == code) {
                try {
                    var btn = keyMapper.getButton();
                    var color = eventType == KeyEvent.KEY_PRESSED ? Color.RED : Color.GRAY;
                    this.joyBtns[btn.ordinal()].setFill(color);
                    this.eventQueue.put(new GameEventWrap(eventType, btn));
                } catch (InterruptedException e) {
                    log.fatal("Keyword event push into queue fail.", e);
                }
            }
        }
    }

    private void fillDefaultBG() {
        this.ctx.setStroke(Color.BLACK);
        this.ctx.fillRect(0, 0, image.getWidth(), image.getHeight());
    }


    public static void run(File file, int scale) {
        new GameWorld(scale).execute(file);
    }
}
