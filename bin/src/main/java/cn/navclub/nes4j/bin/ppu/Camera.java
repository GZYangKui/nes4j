package cn.navclub.nes4j.bin.ppu;

/**
 * Current camera visible range</p>
 * <pre>
 *         +++++++++++++++++++++++++++++++++++++++(x1/y1)
 *         +                                     +
 *         +                                     +
 *         +                                     +
 *         +            visible area             +
 *         +                                     +
 *         +                                     +
 *         +                                     +
 *         +                                     +
 *  (x0/y0)+++++++++++++++++++++++++++++++++++++++
 *  </pre>
 *
 */
public record Camera(int x0, int y0, int x1, int y1) {

}
