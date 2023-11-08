package cn.navclub.nes4j.bin.core;

public interface Bus extends Component {
    /**
     * Write unsigned data to target memory address
     *
     * @param address Target memory address
     * @param value   Unsigned byte data
     */
    void WriteU8(int address, int value);

    /**
     * Read unsigned data from target memory address
     *
     * @param address Target memory address
     * @return Unsigned byte data
     */
    int ReadU8(int address);

    /**
     * Little endian read continue two memory address value
     *
     * @param address Memory address offset
     * @return Two address memory value
     */
    int readInt(int address);

    @Override
    default void write(int address, byte b) {

    }

    @Override
    default byte read(int address) {
        return 0;
    }
}
