package cn.navclub.nes4j.bin.util;

public class PatternTableUtil {

    public static byte[][] tiles(byte[] arr) {
        if (arr.length != 0x10) {
            throw new RuntimeException("Byte array size must equal 0x10 byte.");
        }
        var tiles = new byte[0x08][0x08];
        for (int i = 0; i < 0x08; i++) {
            var left = arr[i];
            var right = arr[i + 0x08];
            var tile = new byte[0x08];
            for (int j = 0; j < 8; j++) {
                var l = ByteUtil.fixBit(left, j, (byte) 1, (byte) 0);
                var r = ByteUtil.fixBit(right, j, (byte) 2, (byte) 0);
                tile[j] = (byte) (l + r);
            }
            tiles[i] = tile;
        }
        return tiles;
    }

    public static String tiles2Str(byte[] arr) {
        var temp = tiles(arr);
        var sb = new StringBuilder();
        for (byte[] bytes : temp) {
            for (byte aByte : bytes) {
                sb.append(aByte).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
