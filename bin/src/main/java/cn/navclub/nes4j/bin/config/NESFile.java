package cn.navclub.nes4j.bin.config;


import cn.navclub.nes4j.bin.model.NESHeader;
import lombok.Data;

@Data
public class NESFile {
    private NESHeader header;
    private final byte[] rgb;
    private final byte[] ch;
    private final byte[] train;
    private final byte[] misCellaneous;

    public NESFile(NESHeader header, byte[] buffer) {
        this.header = header;

        var cSize = header.getChSize();
        var rSize = header.getRgbSize();
        var trainSize = header.trainAreaSize();
        var left = buffer.length - cSize - rSize - trainSize - NESHeader.HEADER_SIZE;

        ch = new byte[cSize];
        rgb = new byte[rSize];
        train = new byte[trainSize];
        misCellaneous = new byte[left];

        if (trainSize > 0) {
            System.arraycopy(buffer, NESHeader.HEADER_SIZE, train, 0, trainSize);
        }
        var offset = NESHeader.HEADER_SIZE + trainSize;
        System.arraycopy(buffer, offset, rgb, 0, rSize);
        if (cSize > 0) {
            offset += rSize;
            System.arraycopy(buffer, offset, ch, 0, cSize);
        }
        if (left > 0) {
            offset += cSize;
            System.arraycopy(buffer, offset, this.misCellaneous, 0, left);
        }
    }
}
