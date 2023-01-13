package cn.navclub.nes4j.app.dialog;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Custom exception dialog
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@SuppressWarnings("all")
public class ExceptionDialog extends Dialog<ButtonType> {
    private final TextArea textArea;
    private final Throwable throwable;


    public ExceptionDialog(final Throwable throwable, String headerText) {
        this.setResizable(true);

        this.throwable = throwable;
        this.textArea = new TextArea();
        this.textArea.setEditable(false);
        this.textArea.setText(throwable2Str(throwable));

        this.setHeaderText(headerText);
        this.setTitle(INes.localeValue("nes4j.error"));

        this.getDialogPane().setContent(this.textArea);
        this.getDialogPane().setContentText(throwable.getMessage());
        this.getDialogPane().getButtonTypes().add(ButtonType.OK);
        this.getDialogPane().getStylesheets().add(FXResource.loadStyleSheet("DException.css"));
    }


    private String throwable2Str(Throwable throwable) {
        try (var sw = new StringWriter();
             var pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return throwable2Str(new RuntimeException("Log exception parser error!"));
        }
    }

    public static ExceptionDialog create(Throwable throwable, String headerText) {
        return new ExceptionDialog(throwable, headerText);
    }

    public static void showAndWait(Throwable throwable, String headerText) {
        create(throwable, headerText).showAndWait();
    }
}
