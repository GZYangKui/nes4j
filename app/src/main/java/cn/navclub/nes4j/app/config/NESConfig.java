package cn.navclub.nes4j.app.config;

import cn.navclub.nes4j.app.model.KeyMapper;
import cn.navclub.nes4j.app.util.IOUtil;
import cn.navclub.nes4j.app.util.JsonUtil;
import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.scene.input.KeyCode;
import lombok.Data;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Data
public class NESConfig {
    private static LoggerDelegate log = LoggerFactory.logger(NESConfig.class);

    private static final String DEFAULT_CONFIG_PATH = OSUtil.workstation() + "config.json";

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

    private static NESConfig instance;

    /**
     * Extra Nes file
     */
    @JsonIgnore
    private File extraNes;
    /**
     * Handle mapper
     */
    private KeyMapper[] mapper;

    public KeyMapper[] getMapper() {
        return Optional.ofNullable(this.mapper).orElse(DEFAULT_KEY_MAPPER);
    }

    public void save() {
        var text = JsonUtil.toJsonStr(this);
        IOUtil.writeStr(Path.of(DEFAULT_CONFIG_PATH), text);
    }

    public boolean isExtraNes() {
        return this.extraNes != null && this.extraNes.exists();
    }


    public static synchronized NESConfig getInstance() {
        if (instance == null) {
            instance = new NESConfig();
            var path = Path.of(DEFAULT_CONFIG_PATH);
            if (Files.exists(path)) {
                try {
                    var jsonStr = Files.readString(path);
                    instance = JsonUtil.parse(jsonStr, NESConfig.class);
                } catch (Exception e) {
                    log.warning("Fail to load program config file.", e);
                }
            }
        }
        return instance;
    }
}
