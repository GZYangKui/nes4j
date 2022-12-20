package cn.navclub.nes4j.app.assets;

import cn.navclub.nes4j.app.INes;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    /**
     * 加载FXML视图文件
     *
     * @param controller FXML控制器
     * @param <T>        FXML视图类型
     * @return FXML实例
     */
    public static <T extends Parent> T loadFXML(Object controller) {
        var name = controller.getClass().getSimpleName();
        var file = String.format("fxml/%s.fxml", name);
        var loader = new FXMLLoader();
        loader.setController(controller);
        loader.setResources(INes.RESOURCE_BUNDLE);
        loader.setLocation(FXResource.class.getResource(file));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
