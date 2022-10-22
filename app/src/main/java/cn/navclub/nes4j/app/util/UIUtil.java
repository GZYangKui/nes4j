package cn.navclub.nes4j.app.util;

import javafx.application.Platform;
import org.controlsfx.dialog.ExceptionDialog;

import java.util.Optional;
import java.util.function.Consumer;

public class UIUtil {
    public static void showError(Throwable t, String title, Consumer<Void> consumer) {
        Platform.runLater(() -> {
            var dialog = new ExceptionDialog(t);
            dialog.setTitle(Optional.ofNullable(title).orElse("未知错误"));
            dialog.showAndWait();
            if (consumer != null) {
                consumer.accept(null);
            }
        });
    }
}
