package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.enums.PStatus;
import cn.navclub.nes4j.bin.test.BaseTest;
import cn.navclub.nes4j.bin.util.ByteUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class PPUTest extends BaseTest {
    @Test
    void test_vram_write() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.writeAddr((byte) 0x23);
        ppu.writeAddr((byte) 0x05);
        ppu.write(0, (byte) 0x66);

        Assertions.assertEquals(ppu.getVram()[0x0305], 0x66);
    }

    @Test
    void test_ppu_vram_reads() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.writeCtr((byte) 0);
        ppu.getVram()[0x0305] = 0x66;

        ppu.writeAddr((byte) 0x23);
        ppu.writeAddr((byte) 0x05);

        ppu.read(0);

        Assertions.assertEquals(ppu.getAddrVal(), 0x2306);
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66);
    }

    @Test
    void test_ppu_vram_reads_cross_page() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.writeCtr((byte) 0);
        ppu.getVram()[0x01ff] = 0x66;
        ppu.getVram()[0x0200] = 0x77;

        ppu.writeAddr((byte) 0x21);
        ppu.writeAddr((byte) 0xff);

        ppu.read(0);

        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66);
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x77);
    }

    @Test
    void test_ppu_vram_reads_step_32() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.writeCtr((byte) 0b100);
        ppu.getVram()[0x01ff] = 0x66;
        ppu.getVram()[0x01ff + 32] = 0x77;
        ppu.getVram()[0x01ff + 64] = (byte) 0x88;

        ppu.writeAddr((byte) 0x21);
        ppu.writeAddr((byte) 0xff);

        ppu.read(0);
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66);
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x77);
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x88);
    }

    // Horizontal: https://wiki.nesdev.com/w/index.php/Mirroring
    //   [0x2000 A ] [0x2400 a ]
    //   [0x2800 B ] [0x2C00 b ]
    @Test
    void test_vram_horizontal_mirror() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.writeAddr((byte) 0x24);
        ppu.writeAddr((byte) 0x05);

        ppu.write(0, (byte) 0x66); //write to a

        ppu.writeAddr((byte) 0x28);
        ppu.writeAddr((byte) 0x05);

        ppu.write(0, (byte) 0x77); //write to B

        ppu.writeAddr((byte) 0x20);
        ppu.writeAddr((byte) 0x05);

        ppu.read(0); //load into buffer
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66); //read from A

        ppu.writeAddr((byte) 0x2C);
        ppu.writeAddr((byte) 0x05);

        ppu.read(0); //load into buffer
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x77); //read from b
    }


    // Vertical: https://wiki.nesdev.com/w/index.php/Mirroring
    //   [0x2000 A ] [0x2400 B ]
    //   [0x2800 a ] [0x2C00 b ]

    @Test
    void test_vram_vertical_mirror() {
        var ppu = new PPU(NameMirror.HORIZONTAL);

        ppu.writeAddr((byte) 0x20);
        ppu.writeAddr((byte) 0x05);

        ppu.write(0, (byte) 0x66); //write to A

        ppu.writeAddr((byte) 0x2C);
        ppu.writeAddr((byte) 0x05);

        ppu.write(0, (byte) 0x77); //write to b

        ppu.writeAddr((byte) 0x28);
        ppu.writeAddr((byte) 0x05);

        ppu.read(0); //load into buffer
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66); //read from a

        ppu.writeAddr((byte) 0x24);
        ppu.writeAddr((byte) 0x05);

        ppu.read(0); //load into buffer
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x77); //read from B
    }


    @Test
    void test_read_status_resets_latch() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.getVram()[0x0305] = 0x66;

        //latch
        ppu.writeAddr((byte) 0x21);
        //non-latch
        ppu.writeAddr((byte) 0x23);
        //latch
        ppu.writeAddr((byte) 0x05);

        //current addr:0x0523

        ppu.read(0); //load_into_buffer
        Assertions.assertNotEquals(ppu.read(0) & 0xff, 0x66);

        ppu.readStatus();

        ppu.writeAddr((byte) 0x23);
        ppu.writeAddr((byte) 0x05);

        ppu.read(0); //load_into_buffer
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66);
    }


    @Test
    void test_ppu_vram_mirroring() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.writeCtr((byte) 0);
        ppu.getVram()[0x0305] = 0x66;

        ppu.writeAddr((byte) 0x63); //0x6305 -> 0x2305
        ppu.writeAddr((byte) 0x05);

        ppu.read(0); //load into_buffer
        Assertions.assertEquals(ppu.read(0) & 0xff, 0x66);
        // assert_eq!(ppu.addr.read(), 0x0306)
    }

    @Test
    void test_read_status_resets_vblank() {
        var ppu = new PPU(NameMirror.HORIZONTAL);
        ppu.getStatus().set(PStatus.V_BLANK_OCCUR);

        var status = ppu.readStatus() & 0xff;

        Assertions.assertEquals(status >> 7, 1);
        Assertions.assertEquals(ppu.getStatus().getBits() & 0xff >> 7, 0);
    }

    @Test
    void test_oam_read_write() {
        var ppu = new PPU(NameMirror.HORIZONTAL);

        ppu.writeOamAddr((byte) 0x10);
        ppu.writeOamByte((byte) 0x66);
        ppu.writeOamByte((byte) 0x77);

        ppu.writeOamAddr((byte) 0x10);
        Assertions.assertEquals(ppu.readOam() & 0xff, 0x66);

        ppu.writeOamByte((byte) 0x11);
        Assertions.assertEquals(ppu.readOam() & 0xff, 0x77);
    }

    @Test
    void test_oam_dma() {
        var ppu = new PPU(NameMirror.HORIZONTAL);

        var data = new byte[256];
        Arrays.fill(data, (byte) 0x66);
        data[0] = 0x77;
        data[255] = (byte) 0x88;

        ppu.writeOamAddr((byte) 0x10);
        ppu.writeOam(data);

        ppu.writeOamAddr((byte) 0xf); //wrap around
        Assertions.assertEquals(ppu.readOam() & 0xff, 0x88);

        ppu.writeOamAddr((byte) 0x10);
        ppu.writeOamAddr((byte) 0x77);
        ppu.writeOamAddr((byte) 0x11);
        ppu.writeOamAddr((byte) 0x66);
    }


}
