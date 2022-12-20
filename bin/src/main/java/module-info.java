import cn.navclub.nes4j.bin.apu.Player;

module cn.navclub.nes4j.bin {
    requires static lombok;
    requires org.slf4j;

    exports cn.navclub.nes4j.bin;
    exports cn.navclub.nes4j.bin.core;
    exports cn.navclub.nes4j.bin.util;
    exports cn.navclub.nes4j.bin.ppu;
    exports cn.navclub.nes4j.bin.config;
    exports cn.navclub.nes4j.bin.debug;
    exports cn.navclub.nes4j.bin.function;
    exports cn.navclub.nes4j.bin.apu.impl;
    exports cn.navclub.nes4j.bin.io;
    exports cn.navclub.nes4j.bin.ppu.register;
    exports cn.navclub.nes4j.bin.apu;
    exports cn.navclub.nes4j.bin.eventbus;

    uses Player;
}