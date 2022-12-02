package cn.navclub.nes4j.bin.screen;

/**
 * Current camera visible range</p>
 *
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
 *
 */
public record Camera(int x0, int y0, int x1, int y1) {

}
