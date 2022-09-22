package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.NESFile;
import cn.navclub.nes4j.bin.core.VirtualCPU;
import cn.navclub.nes4j.bin.model.NESHeader;
import cn.navclub.nes4j.bin.util.IOUtil;

import java.io.File;
import java.util.Objects;

public class NES {
    private final NESFile nesFile;

    private NES(NESBuilder builder) {
        var buffer = Objects.requireNonNullElseGet(
                builder.buffer,
                () -> IOUtil.readFileAllByte(builder.file)
        );
        this.nesFile = new NESFile(new NESHeader(buffer), buffer);
    }

    public void execute() {
        var cpu = new VirtualCPU();
        cpu.loadRun(this.nesFile.getRgb());
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
