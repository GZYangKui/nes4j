package cn.navclub.nes4j.app.config;

import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.app.util.IOUtil;
import cn.navclub.nes4j.app.util.JsonUtil;
import cn.navclub.nes4j.bin.core.JoyPad;
import javafx.scene.input.KeyCode;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Optional;

@Setter
public class NESConfig {
    private static final KeyMapper[] DEFAULT_KEY_MAPPER = {
            new KeyMapper(JoyPad.JoypadButton.BTN_A, KeyCode.A),
            new KeyMapper(JoyPad.JoypadButton.BTN_B, KeyCode.S),
            new KeyMapper(JoyPad.JoypadButton.BTN_UP, KeyCode.UP),
            new KeyMapper(JoyPad.JoypadButton.BTN_DN, KeyCode.DOWN),
            new KeyMapper(JoyPad.JoypadButton.BTN_SE, KeyCode.SPACE),
            new KeyMapper(JoyPad.JoypadButton.BTN_ST, KeyCode.ENTER),
            new KeyMapper(JoyPad.JoypadButton.BTN_LF, KeyCode.LEFT),
            new KeyMapper(JoyPad.JoypadButton.BTN_RT, KeyCode.RIGHT)

    };
    /**
     * Config File path
     */
    @Setter
    private Path path;

    /**
     * Handle mapper
     */
    private KeyMapper[] mapper;


    public KeyMapper[] getMapper() {
        return Optional.ofNullable(this.mapper).orElse(DEFAULT_KEY_MAPPER);
    }

    public void save() {
        var text = JsonUtil.toJsonStr(this);
        IOUtil.writeStr(this.path, text);
    }
}
