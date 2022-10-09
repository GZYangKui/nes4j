package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.Bus;
import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.core.NESFile;
import cn.navclub.nes4j.bin.model.NESHeader;
import cn.navclub.nes4j.bin.util.IOUtil;
import lombok.Getter;

import java.io.File;
import java.util.Objects;

public class NES {
    private final Bus bus;
    private final CPU cpu;
    @Getter
    private final NESFile nesFile;


    private NES(NESBuilder builder) {
        var buffer = Objects.requireNonNullElseGet(
                builder.buffer,
                () -> IOUtil.readFileAllByte(builder.file)
        );
        this.nesFile = new NESFile(new NESHeader(buffer), buffer);
        this.bus = new Bus(this.nesFile);
        this.cpu = new CPU(this.bus);
    }

    public void execute() {
        this.cpu.reset();
        while (true) {
            try {
                this.cpu.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
