package cn.navclub.nes4j.app.util;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.dialog.ExceptionDialog;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 封装<b>Javafx</b>UI相关操作工具类
 */
public class UIUtil {
    public static void showError(Throwable t, String headerText, Consumer<Void> consumer) {
        Platform.runLater(() -> {
            ExceptionDialog.showAndWait(t, headerText);

            if (consumer != null) {
                consumer.accept(null);
            }
        });
    }

    public static Optional<String> prompt(String headerTitle) {
        var dialog = new TextInputDialog();
        dialog.setHeaderText(INes.localeValue(headerTitle));
        return dialog.showAndWait();
    }

    /**
     * Show a confirm dialog.
     *
     * @param text Confirm text
     * @return If <code>OK</code> was pressed return {@code true} otherwise {@code false}
     * @apiNote Please ensure call in Javafx UI thread
     */
    public static boolean confirm(String text) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(text);
        var optional = alert.showAndWait();
        return optional.isPresent() && optional.get() == ButtonType.OK;
    }
}
