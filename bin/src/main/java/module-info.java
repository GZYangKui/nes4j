module cn.navclub.nes4j.bin {
    requires static lombok;
    requires org.slf4j;

    exports cn.navclub.nes4j.bin;
    exports cn.navclub.nes4j.bin.core;
    exports cn.navclub.nes4j.bin.util;
    exports cn.navclub.nes4j.bin.core.impl;
    exports cn.navclub.nes4j.bin.screen;
    exports cn.navclub.nes4j.bin.enums;
    exports cn.navclub.nes4j.bin.debug;
    exports cn.navclub.nes4j.bin.function;
    exports cn.navclub.nes4j.bin.apu.impl;

    uses cn.navclub.nes4j.bin.Player;
}