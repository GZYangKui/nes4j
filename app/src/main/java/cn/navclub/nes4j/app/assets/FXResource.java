package cn.navclub.nes4j.app.assets;

import javafx.scene.image.Image;

import java.io.IOException;

public class FXResource {
    public static String loadStyleSheet(String name) {
        var url = FXResource.class.getResource("css/" + name);
        if (url == null) {
            return "";
        }
        return url.toExternalForm();
    }

    public static Image loadImage(String name) {
        var url = FXResource.class.getResource("img/" + name);
        if (url == null) {
            throw new RuntimeException("Target image:" + name + " not exist.");
        }
        try {
            return new Image(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
