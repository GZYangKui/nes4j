module cn.navclub.nes4j.bin {
    requires org.slf4j;
    requires static lombok;

    exports cn.navclub.nes4j.bin;
    exports cn.navclub.nes4j.bin.core;
    exports cn.navclub.nes4j.bin.util;
    exports cn.navclub.nes4j.bin.screen;
    exports cn.navclub.nes4j.bin.enums;
}