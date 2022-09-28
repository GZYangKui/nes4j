package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.Bus;
import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.core.MemoryMap;
import cn.navclub.nes4j.bin.core.NESFile;
import cn.navclub.nes4j.bin.model.NESHeader;
import cn.navclub.nes4j.bin.util.IOUtil;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Objects;

public class NES {
    private final Bus bus;
    private final CPU cpu;
    private final MemoryMap map;
    @Getter
    private final NESFile nesFile;


    private NES(NESBuilder builder) {
        var buffer = Objects.requireNonNullElseGet(
                builder.buffer,
                () -> IOUtil.readFileAllByte(builder.file)
        );
        this.nesFile = new NESFile(new NESHeader(buffer), buffer);
        this.map = new MemoryMap(this.nesFile.getRgb());
        this.bus = new Bus(this.map);
        this.cpu = new CPU(this.bus);

        try (var output = new FileOutputStream("./output.txt")) {
            output.write(this.nesFile.getRgb());
        }catch (Exception e){

        }
    }

    public void execute() {
        this.cpu.reset();
        while (true) {
            this.cpu.execute();
        }
    }


    public static class NESBuilder {
        private File file;
        private byte[] buffer;

        public NESBuilder buffer(byte[] buffer) {
            this.buffer = buffer;
            return this;
        }

        public NESBuilder file(File file) {
            this.file = file;
            return this;
        }

        public NES build() {
            return new NES(this);
        }

        public static NESBuilder newBuilder() {
            return new NESBuilder();
        }
    }
}
