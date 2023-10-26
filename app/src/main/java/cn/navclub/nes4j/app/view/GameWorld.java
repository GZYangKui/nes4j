package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.audio.JavaXAudio;
import cn.navclub.nes4j.app.control.IconPopup;
import cn.navclub.nes4j.app.service.TaskService;
import cn.navclub.nes4j.app.dialog.DHandle;
import cn.navclub.nes4j.app.event.FPSTracer;
import cn.navclub.nes4j.app.event.GameEventWrap;
import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.util.UIUtil;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.ppu.Frame;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
    private Canvas canvas;
    @FXML
    private StackPane stackPane;

    //Pixel scale level
    @SuppressWarnings("all")
    private final int scale;

    private final FPSTracer tracer;
    private final IntBuffer intBuffer;
    private final GraphicsContext ctx;
    private final WritableImage image;
    private final BlockingQueue<GameEventWrap> eventQueue;

    private NES instance;
    private volatile int fps;
    private Debugger debugger;
    private TaskService<Void> service;
    private final IconPopup speedPopup;

    public GameWorld() {
        var scene = new Scene(FXResource.loadFXML(this));

        this.scale = 3;
        this.eventQueue = new LinkedBlockingDeque<>();

        this.tracer = new FPSTracer(it -> this.fps = it);
        this.speedPopup = new IconPopup(FXResource.loadImage("speed.png"));

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
    }

    @SuppressWarnings("all")
    @FXML
    public void debugger() {
        if (this.debugger == null) {
            this.debugger = new Debugger(this);
            if (this.instance != null) {
                this.instance.setDebugger(this.debugger);
            }
        }
        this.debugger.show();
    }

    public void execute(File file) {
        this.service = TaskService.execute(new Task<>() {
            @Override
            protected Void call() {
                GameWorld.this.instance = NES.NESBuilder
                        .newBuilder()
                        .file(file)
                        .player(JavaXAudio.class)
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
        this.debugDispose();

        if (this.service != null)
            this.service.cancel();

        if (this.instance != null)
            this.instance.stop();

        if (t != null) {
            log.fatal(INes.localeValue("nes4j.game.error"), t);
            UIUtil.showError(t, INes.localeValue("nes4j.game.error"), v -> this.close());
        }

        this.tracer.stop();

        this.ctx.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    public void debugDispose() {
        this.debugger = null;
        if (this.instance != null)
            this.instance.setDebugger(null);

        System.gc();
    }

    @FXML
    public void handle() {
        var dialog = new DHandle(INes.config().getMapper());
        var optional = dialog.showAndWait();
        optional.ifPresent(keyMappers -> {
            INes.config().setMapper(keyMappers);
            INes.config().save();
        });
    }

    @FXML
    public void reset() {
        if (this.instance == null) {
            return;
        }
        this.fillDefaultBG();
        this.instance.SWReset();
    }

    @FXML
    public void ppuViewer() {
        if (this.instance == null) {
            return;
        }
        new PPUViewer(this.instance);
    }

    private void gameLoopCallback(Frame frame, JoyPad joyPad, JoyPad joyPad1, Long nano) {
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

        var event = eventQueue.poll();
        if (event != null) {
            joyPad.updateBtnStatus(event.btn(), event.event() == KeyEvent.KEY_PRESSED);
        }

        this.tracer.increment();


        Platform.runLater(() -> {
            //Clear whole canvas
            this.ctx.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
            //Draw image
            this.ctx.drawImage(image, 0, 0);
            //Draw fps
            this.fbl.setText(String.format("fps:%d", this.fps));
        });
    }

    private void keyEventHandler(KeyEvent event) {
        var code = event.getCode();
        var eventType = event.getEventType();
        if (!(eventType == KeyEvent.KEY_PRESSED || eventType == KeyEvent.KEY_RELEASED)) {
            return;
        }
        for (KeyMapper keyMapper : INes.config().getMapper()) {
            if (keyMapper.getKeyCode() == code) {
                try {
                    this.eventQueue.put(new GameEventWrap(eventType, keyMapper.getButton()));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //Change emulator speed
        if (code == KeyCode.ADD || code == KeyCode.SUBTRACT) {
            if (log.isDebugEnabled()) {
                log.debug("Change ppu output frame action:{}", code);
            }
            this.instance.speed(code == KeyCode.ADD ? -1 : 1);
            this.speedPopup.show(this);
        }
    }

    private void fillDefaultBG() {
        this.ctx.setStroke(Color.BLACK);
        this.ctx.fillRect(0, 0, image.getWidth(), image.getHeight());
    }


    public static void run(File file) {
        new GameWorld().execute(file);
    }
}
