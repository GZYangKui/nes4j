package cn.navclub.nes4j.app.util;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.dialog.ExceptionDialog;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;
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

    public static Optional<String> textFieldDialog(String headerTitle) {
        var dialog = new TextInputDialog();
        dialog.setHeaderText(INes.localeValue(headerTitle));
        return dialog.showAndWait();
    }
}
