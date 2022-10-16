package cn.navclub.nes4j.bin.screen;

import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.util.PatternTableUtil;

public class Render {
    private static final int[] SYS_2C02_PALETTE;
    private static final int[] SYS_2C03_05_PALETTE;

    static {
        var palette2C02Str = """
                84  84  84    0  30 116    8  16 144   48   0 136   68   0 100   92   0  48   84   4   0   60  24   0   32  42   0    8  58   0    0  64   0    0  60   0    0  50  60    0   0   0
                                
                152 150 152    8  76 196   48  50 236   92  30 228  136  20 176  160  20 100  152  34  32  120  60   0   84  90   0   40 114   0    8 124   0    0 118  40    0 102 120    0   0   0
                                
                236 238 236   76 154 236  120 124 236  176  98 236  228  84 236  236  88 180  236 106 100  212 136  32  160 170   0  116 196   0   76 208  32   56 204 108   56 180 204   60  60  60
                                
                236 238 236  168 204 236  188 188 236  212 178 236  236 174 236  236 174 212  236 180 176  228 196 144  204 210 120  180 222 120  168 226 144  152 226 180  160 214 228  160 162 160
                """;
        var palette2C0305Str = """
                333,014,006,326,403,503,510,420,320,120,031,040,022,000,000,000,
                555,036,027,407,507,704,700,630,430,140,040,053,044,000,000,000,
                777,357,447,637,707,737,740,750,660,360,070,276,077,000,000,000,
                777,567,657,757,747,755,764,772,773,572,473,276,467,000,000,000,
                """;
        SYS_2C02_PALETTE = paletteStr2Arr(palette2C02Str, ' ');
        SYS_2C03_05_PALETTE = paletteStr2Arr(palette2C0305Str, ',');
    }

    public static void render(PPU ppu, Frame frame) {
        var ctr = ppu.getControl();
        var index = ctr.patternNameAddr();
        var nameTable = ctr.nameTableAddr();
        //读取960个tile背景
        for (int i = 0; i < 960; i++) {
            var x = i % 32;
            var y = i / 32;
            var tile = new byte[16];
            //获取tile编号
            var numbered = ppu.getVram()[i] & 0xff;
            System.arraycopy(ppu.getCh(), index + numbered * 16, tile, 0, 16);
            var arr = PatternTableUtil.tiles(tile);
            for (int h = 0; h < arr.length; h++) {
                var row = arr[h];
                for (int k = 0; k < row.length; k++) {
                    var rgb = switch (row[k]) {
                        case 1 -> 0xff0000;
                        case 2 -> 0x008000;
                        case 3 -> 0x0000ff;
                        default -> 0;
                    };
                    frame.updatePixel(x * 8 + k, y * 8 + h, rgb);
                }
            }
        }
    }

    private static int[] paletteStr2Arr(String paletteStr, char skip) {
        var append = false;
        var index = 0;
        var counter = 0;
        var chr = new byte[3];
        var target = new int[512];
        var arr = paletteStr.getBytes();
        for (byte b : arr) {
            append = (b != skip && b != '\n');
            if (!append) {
                if (index != 0) {
                    var str = new String(chr, 0, index);
                    target[counter++] = Integer.parseInt(str);
                }
                index = 0;
                continue;
            }
            chr[index++] = b;
        }
        var dst = new int[counter];
        System.arraycopy(target, 0, dst, 0, counter);
        return dst;
    }
}
