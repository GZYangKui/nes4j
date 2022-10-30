package cn.navclub.nes4j.bin.core;


import cn.navclub.nes4j.bin.enums.NESFormat;
import cn.navclub.nes4j.bin.util.ByteUtil;
import cn.navclub.nes4j.bin.util.IOUtil;
import lombok.Data;

import java.io.File;

@Data
public class NESFile {
    private final static int HEADER_SIZE = 16;
    private final byte[] headers;
    private final NESFormat format;
    private final int rgbSize;
    private final int chSize;
    private final int flag6;
    private final int flag7;
    //0->horizontal 1->vertical
    private final int mirrors;

    private final byte[] rgb;
    private final byte[] ch;
    private final byte[] train;
    private final byte[] cellaneous;

    public NESFile(byte[] buffer) {
        this.headers = new byte[HEADER_SIZE];
        //从原始数据中复制Header数据
        System.arraycopy(buffer, 0, headers, 0, HEADER_SIZE);

        this.format = this.parseFormat();
        this.rgbSize = this.calRgbSize();
        this.chSize = this.calChSize();
        this.flag6 = Byte.toUnsignedInt(headers[6]);
        this.flag7 = Byte.toUnsignedInt(headers[7]);
        this.mirrors = (flag6 & 1) != 0 ? 1 : 0;

        var trainSize = this.trainAreaSize();

        ch = new byte[chSize];
        rgb = new byte[rgbSize];
        train = new byte[trainSize];

        if (trainSize > 0) {
            System.arraycopy(buffer, HEADER_SIZE, train, 0, trainSize);
        }
        var offset = HEADER_SIZE + trainSize;
        System.arraycopy(buffer, offset, rgb, 0, rgbSize);
        if (chSize > 0) {
            offset += rgbSize;
            System.arraycopy(buffer, offset, ch, 0, chSize);
        }

        var left = buffer.length - chSize - rgbSize - trainSize - HEADER_SIZE;
        if (this.format == NESFormat.NES_20 && left > 0) {
            cellaneous = new byte[left];
            offset += chSize;
            System.arraycopy(buffer, offset, this.cellaneous, 0, left);
        } else {
            this.cellaneous = new byte[0];
        }
    }

    public NESFile(File file) {
        this(IOUtil.readFileAllByte(file));
    }

    private int calChSize() {
        var lsb = this.headers[5];
        var size = lsb & 0xff;
        var scale = 8 * 1024;
        if (this.format == NESFormat.NES_20) {
            var msb = (this.headers[9]) >>> 4;
            size = (lsb | msb << 8);
            if (msb > 0x0e) {
                scale = 0;
            }
        }
        return size * scale;
    }

    /**
     * 计算RGB-ROM大小
     */
    private int calRgbSize() {
        var lsb = this.headers[4];
        var size = (lsb & 0xff);
        var scale = 16 * 1024;
        if (this.format == NESFormat.NES_20) {
            var msb = this.headers[9] & 0x0f;
            size = ByteUtil.toInt(new byte[]{lsb, ByteUtil.overflow(msb), 0, 0});
            if (msb > 0x0e) {
                scale = 0;
            }
        }
        return size * scale;
    }

    /**
     * 判断当前文件是INES格式还是NES_2.0格式
     */
    private NESFormat parseFormat() {
        if (!(headers[0] == 'N' && headers[1] == 'E' && headers[2] == 'S' && headers[3] == 0X1A)) {
            throw new RuntimeException("Only support ines and nes_2.0 binary format.");
        }
        final NESFormat nesFormat;
        if ((headers[7] & 0x0C) == 0x08) {
            nesFormat = NESFormat.NES_20;
        } else {
            nesFormat = NESFormat.INES;
        }
        return nesFormat;
    }

    /**
     * 判断是否存在Trainer area
     */
    public int trainAreaSize() {
        var has = (this.flag6 & 0b0000_0100) > 0;
        return has ? 512 : 0;
    }
}