package cn.navclub.nes4j.bin.core.register;

import cn.navclub.nes4j.bin.config.ICPUStatus;
import cn.navclub.nes4j.bin.config.Register;

/**
 * <pre>
 *
 * As instructions are executed a set of processor flags are set or clear to record the results of the operation.
 * This flags and some additional control flags are held in a special status register. Each flag has a single bit
 * within the register.
 *
 * Instructions exist to test the values of the various bits, to set or clear some of them and to push or pull the
 * entire set to or from the stack.
 *
 * <li>Carry Flag</li>
 * The carry flag is set if the last operation caused an overflow from bit 7 of the result or an underflow from bit
 * 0. This condition is set during arithmetic, comparison and during logical shifts. It can be explicitly set using
 * the 'Set Carry Flag' (SEC) instruction and cleared with 'Clear Carry Flag' (CLC).
 *
 * <li>Zero Flag</li>
 * The zero flag is set if the result of the last operation as was zero.
 *
 * <li>Interrupt Disable</li>
 * The interrupt disable flag is set if the program has executed a 'Set Interrupt Disable' (SEI) instruction. While
 * this flag is set the processor will not respond to interrupts from devices until it is cleared by a 'Clear Interrupt
 * Disable' (CLI) instruction.
 *
 * <li>Decimal Mode</li>
 * While the decimal mode flag is set the processor will obey the rules of Binary Coded Decimal (BCD) arithmetic
 * during addition and subtraction. The flag can be explicitly set using 'Set Decimal Flag' (SED) and cleared with
 * 'Clear Decimal Flag' (CLD).
 *
 * <li>Break Command</li>
 * The break command bit is set when a BRK instruction has been executed and an interrupt has been generated to
 * process it.
 *
 * <li>Overflow Flag</li>
 * The overflow flag is set during arithmetic operations if the result has yielded an invalid 2's complement result
 * (e.g. adding to positive numbers and ending up with a negative result: 64 + 64 => -128). It is determined by
 * looking at the carry between bits 6 and 7 and between bit 7 and the carry flag.
 *
 * <li>Negative Flag</li>
 * The negative flag is set if the result of the last operation had bit 7 set to a one.
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class CPUStatus extends Register<ICPUStatus> {
}
