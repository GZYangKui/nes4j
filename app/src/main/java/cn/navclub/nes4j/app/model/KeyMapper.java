package cn.navclub.nes4j.app.model;

import cn.navclub.nes4j.bin.io.JoyPad;
import javafx.scene.input.KeyCode;
import lombok.Data;

@Data
public class KeyMapper {
    private KeyCode keyCode;
    private JoyPad.JoypadButton button;

    public KeyMapper(JoyPad.JoypadButton button, KeyCode keyCode) {
        this.button = button;
        this.keyCode = keyCode;
    }

    public KeyMapper() {
    }

    public KeyMapper copy() {
        return new KeyMapper(this.button, this.keyCode);
    }
}
