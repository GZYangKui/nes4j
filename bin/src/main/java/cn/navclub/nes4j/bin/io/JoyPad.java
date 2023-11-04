package cn.navclub.nes4j.bin.io;

import cn.navclub.nes4j.bin.util.BinUtil;

import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * <a href="https://www.nesdev.org/wiki/Standard_controller"> Standard controller</a>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class JoyPad {
    private int index;
    //Record if high strobe
    private boolean strobe;
    private volatile byte bits;

    /**
     * <h3>Input ($4016 write)</h3>
     * <pre>
     * 7  bit  0
     * ---- ----
     * xxxx xxxS
     *         |
     *         +- Controller shift register strobe
     * </pre>
     * <note>
     * While S (strobe) is high, the shift registers in the controllers are continuously reloaded
     * from the button states, and reading $4016/$4017 will keep returning the current state of the
     * first button (A). Once S goes low, this reloading will stop. Hence a 1/0 write sequence is
     * required to get the button states, after which the buttons can be read back one at a time.
     * (Note that bits 2-0 of $4016/write are stored in internal latches in the 2A03/07.)
     * </note>
     */
    public void write(byte b) {
        this.strobe = (b & 1) == 1;
        if (this.strobe) {
            this.index = 0;
        }
    }

    /**
     * <h3>Output ($4016/$4017 read)</h3>
     * <pre>
     *     7  bit  0
     * ---- ----
     * xxxx xMES
     *       |||
     *       ||+- Primary controller status bit
     *       |+-- Expansion controller status bit (Famicom)
     *       +--- Microphone status bit (Famicom, $4016 only)
     * </pre>
     * <note>
     * <p>
     * Though both are polled from a write to $4016, controller 1 is read through $4016, and
     * controller 2 is separately read through $4017.
     * </p>
     * <p>
     * Each read reports one bit at a time through D0. The first 8 reads will indicate which buttons
     * or directions are pressed (1 if pressed, 0 if not pressed). All subsequent reads will return 1
     * on official Nintendo brand controllers but may return 0 on third party controllers such as the
     * U-Force.
     * </p>
     * <p>
     * Status for each controller is returned as an 8-bit report in the following order: A, B, Select,
     * Start, Up, Down, Left, Right.
     * </p>
     * <p>
     * In the NES and Famicom, the top three (or five) bits are not driven, and so retain the bits of
     * the previous byte on the bus. Usually this is the most significant byte of the address of the
     * controller port—0x40. Certain games (such as Paperboy) rely on this behavior and require that
     * reads from the controller ports return exactly $40 or $41 as appropriate. See: Controller
     * reading: unconnected data lines.
     * </p>
     * <p>
     * When no controller is connected, the corresponding status bit will report 0. This is due to
     * the presence of internal pull-up resistors, and the internal inverter. (See: Controller reading)
     * </p>
     * </note>
     */
    public byte read() {
        if (this.index > 7) {
            return 1;
        }
        var b = (uint8(this.bits) & (1 << this.index)) >> this.index;
        if (!this.strobe) {
            this.index++;
        }
        return (byte) b;
    }

    public void updateBtnStatus(JoypadButton action, boolean press) {
        final byte tmp;
        var ordinal = action.ordinal();
        if (press) {
            tmp = (byte) (this.bits | (1 << ordinal));
        } else {
            tmp = (byte) (this.bits & (0xff - (int) (Math.pow(2, ordinal))));
        }
        this.bits = tmp;
    }

    @Override
    public String toString() {
        return BinUtil.toBinStr(this.bits);
    }

    public enum JoypadButton {
        //A
        BTN_A,
        //B
        BTN_B,
        //Select
        BTN_SE,
        //Start
        BTN_ST,
        //UP
        BTN_UP,
        //Down
        BTN_DN,
        //Left
        BTN_LF,
        //Right
        BTN_RT;

        @Override
        public String toString() {
            return switch (this) {
                case BTN_A -> "A";
                case BTN_B -> "B";
                case BTN_DN -> "Down";
                case BTN_LF -> "Left";
                case BTN_RT -> "Right";
                case BTN_SE -> "Select";
                case BTN_ST -> "Start";
                case BTN_UP -> "Up";
            };
        }
    }

}
