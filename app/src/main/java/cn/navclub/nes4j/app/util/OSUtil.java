package cn.navclub.nes4j.app.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Optional;

public class OSUtil {
    public static Optional<File> chooseFile(Window owner, String describe, String... extension) {
        var chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(describe, "*.nes", "*.NES"));
        return Optional.ofNullable(chooser.showOpenDialog(owner));
    }
}
