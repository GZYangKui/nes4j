package cn.navclub.nes4j.app.event;

import cn.navclub.nes4j.bin.core.JoyPad;
import javafx.event.EventType;
import javafx.scene.input.KeyEvent;

public record GameEventWrap(EventType<KeyEvent> event, JoyPad.JoypadButton btn) {

}
