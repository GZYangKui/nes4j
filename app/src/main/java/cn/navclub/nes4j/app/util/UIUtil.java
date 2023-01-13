package cn.navclub.nes4j.app.util;

import cn.navclub.nes4j.app.dialog.ExceptionDialog;
import javafx.application.Platform;

import java.util.function.Consumer;

public class UIUtil {
    public static void showError(Throwable t, String headerText, Consumer<Void> consumer) {
        Platform.runLater(() -> {
            ExceptionDialog.showAndWait(t, headerText);

            if (consumer != null) {
                consumer.accept(null);
            }
        });
    }
}
