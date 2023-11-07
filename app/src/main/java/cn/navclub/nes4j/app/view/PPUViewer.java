package cn.navclub.nes4j.app.view;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.util.BinUtil;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.nio.IntBuffer;

@SuppressWarnings("all")
public class PPUViewer extends Stage {
    private final Canvas canvas;
    private final BorderPane borderPane;

    //BLACK-RED-GREEN-BLUE
    private static int[] DEF_FILL_COLOR = {0xff000000, 0xffff0000, 0xff00ff00, 0xff0000ff};

    public PPUViewer(byte[] buffer, int[] colors) {
        this.canvas = new Canvas();
        this.borderPane = new BorderPane(this.canvas);
        this.setScene(new Scene(this.borderPane));

        var intBuf = IntBuffer.allocate(1);
        var image = new WritableImage(256, 128);
        var writer = image.getPixelWriter();
        var pixelFormat = PixelFormat.getIntArgbInstance();
        var stride = 32;
        for (int i = 0; i < 512; i++) {
            var offset = i * 16;
            var index = i % stride;
            var rowNum = i / stride;
            for (int j = 0; j < 8; j++) {
                var l = buffer[offset + j];
                var r = buffer[offset + 8 + j];
                for (int k = 7; k >= 0; k--) {
                    var y = rowNum * 8 + j;
                    var x = index * 8 + (7 - k);
                    var idx = ((l >> k) & 1) | ((r >> k & 1) << 1);
                    writer.setPixels(x, y, 1, 1, pixelFormat, intBuf.put(0, colors[idx]), 256);
                }
            }
        }
        this.canvas.setWidth(256);
        this.canvas.setHeight(128);
        this.canvas.getGraphicsContext2D().drawImage(image, 0, 0);

        // By console print click charactar index
        this.canvas.setOnMouseClicked(event -> {
            var x = event.getSceneX();
            var y = event.getSceneY();
            var h = (int) (y / 8);
            var k = (int) (x / 8);
            var idx = h * stride + k;
            System.out.printf("idx=0x%s\n", BinUtil.toHexStr((int) idx));
        });

        this.setResizable(false);
        this.setTitle("PPU Viewer");

        this.show();
    }

    public PPUViewer(NesConsole console) {
        this(console.getCartridge().getChrom(), DEF_FILL_COLOR);
    }
}
