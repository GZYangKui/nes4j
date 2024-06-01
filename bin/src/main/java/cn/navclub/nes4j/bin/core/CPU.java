package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.*;
import cn.navclub.nes4j.bin.core.register.CPUStatus;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.util.BinUtil;
import cn.navclub.nes4j.bin.util.internal.ScriptUtil;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static cn.navclub.nes4j.bin.util.BinUtil.*;

/**
 * 6502 cpu instance
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class CPU {
    private final static Map<Byte, WS6502> MWS6502;
    private final static LoggerDelegate logger = LoggerFactory.logger(CPU.class);

    static {
        MWS6502 = new HashMap<>();
        try (var is = CPU.class.getResourceAsStream("6502.txt");
             var buffer = new BufferedReader(new InputStreamReader(is))) {
            String line;
            var lineNum = 0;
            var pattern = "(\\w)+( )*(\\w)+( )*(-)?(\\d)+( )*(\\d)+( )*(\\d)+";
            while ((line = buffer.readLine()) != null) {
                lineNum++;
                line = line.trim();
                //Skip comment and blank line
                if (line.startsWith(";") || line.isBlank()) {
                    continue;
                }
                if (!line.matches(pattern)) {
                    logger.warning("Unknown text line format in line {},please keep follow regex express:\n{}", lineNum, pattern);
                    continue;
                }

                var arr = line.split("( )+");
                var ins = Instruction.valueOf(arr[0]);
                var addrMode = AddressMode.valueOf(arr[1]);
                var opec = Byte.parseByte(arr[2]);
                var cycle = Integer.parseInt(arr[3]);
                var size = Integer.parseInt(arr[4]);
                if (MWS6502.containsKey(opec)) {
                    logger.warning("Repeat define opencode 0x{} in line {}?", BinUtil.toBinStr(opec), lineNum);
                }
                MWS6502.put(opec, new WS6502(opec, size, cycle, addrMode, ins));
            }
        } catch (Exception e) {
            throw new RuntimeException("6502 cpu instruction init fail:%s".formatted(e.getMessage()));
        }
    }


    //Stack offset
    public static final int STACK = 0x0100;
    //Program counter reset offset
    private static final int PC_RESET = 0xfffc;
    //Stack reset offset
    private static final int STACK_RESET = 0xfd;

    //Accumulator register
    @Getter
    private int ra;
    //X register
    @Getter
    private int rx;
    //Y register
    @Getter
    private int ry;
    //Program counter
    @Getter
    protected int pc;
    @Getter
    //Stack pointer
    private int sp;
    //Record game execute instruction number
    @Getter
    private long instructions;
    private final CPUStatus status;
    private final NesConsole console;
    private final MemoryBusAdapter bus;

    public CPU(NesConsole console) {
        this.console = console;
        this.status = new CPUStatus();
        this.bus = new MemoryBusAdapter(this, console);
    }


    /**
     * Reset cpu all status
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.ra = 0;
        this.sp = STACK_RESET;
        this.instructions = 0;
        this.pc = this.bus.readInt(PC_RESET);
        this.status.setBits(int8(0b000100));
    }


    public void push(byte data) {
        this.bus.write(STACK + this.sp, data);
        this.sp = u8sbc(this.sp, 1);
    }

    public void pushInt(int data) {
        var lsb = uint8(data);
        var msb = uint8(data >> 8);

        this.push(int8(msb));
        this.push(int8(lsb));
    }

    public byte pop() {
        this.sp = u8add(this.sp, 1);
        return this.bus.read(STACK + this.sp);
    }

    public int popInt() {
        var lsb = uint8(this.pop());
        var msb = uint8(this.pop());
        return lsb | msb << 8;
    }


    private void LDAImpl(AddressMode mode) {
        var address = this.bus.getAbsAddr(mode);
        var value = this.bus.ReadU8(address);
        this.raUpdate(value);
    }

    private void raUpdate(int value) {
        value = uint8(value);
        //Set ra
        this.ra = value;
        //Update Zero and negative flag
        this.NZUpdate(value);
    }

    /**
     * Check CPU Negative and Zero flag
     */
    private void NZUpdate(int result) {
        var b = uint8(result);
        this.status.update(ICPUStatus.ZERO, (b == 0));
        this.status.update(ICPUStatus.NEGATIVE, (b >> 7) == 1);
    }

    /**
     * Or and not operator
     */
    private void LogicImpl(Instruction instruction, AddressMode addressMode) {
        var address = this.bus.getAbsAddr(addressMode);
        var a = this.ra;
        var b = this.bus.ReadU8(address);
        var c = switch (instruction) {
            case EOR -> a ^ b;
            case ORA -> a | b;
            case AND -> a & b;
            default -> a;
        };
        this.raUpdate(c);
    }

    private void LSRImpl(AddressMode mode) {
        var addr = 0;
        var operand = mode == AddressMode.Accumulator
                ? this.ra
                : this.bus.ReadU8(addr = this.bus.getAbsAddr(mode));

        this.status.update(ICPUStatus.CARRY, (operand & 1) == 1);
        operand >>= 1;
        if (mode == AddressMode.Accumulator) {
            this.raUpdate(operand);
        } else {
            this.bus.WriteU8(addr, operand);
            this.NZUpdate(operand);
        }
    }

    private void ROLImpl(AddressMode mode) {
        var bit = 0;
        var addr = 0;
        var value = 0;
        var updateRA = (mode == AddressMode.Accumulator);
        if (updateRA) {
            value = this.ra;
        } else {
            addr = this.bus.getAbsAddr(mode);
            value = this.bus.ReadU8(addr);
        }
        bit = value >> 7;
        value <<= 1;
        value |= this.status.get(ICPUStatus.CARRY);
        this.status.update(ICPUStatus.CARRY, bit == 1);
        if (updateRA) {
            this.raUpdate(value);
        } else {
            this.bus.WriteU8(addr, value);
            this.NZUpdate(value);
        }
    }

    private void RORImpl(AddressMode mode) {
        var addr = 0;
        var value = this.ra;
        var rora = mode == AddressMode.Accumulator;
        if (!rora) {
            addr = this.bus.getAbsAddr(mode);
            value = this.bus.ReadU8(addr);
        }
        var oBit = value & 1;
        value >>= 1;
        value |= (this.status.get(ICPUStatus.CARRY) << 7);
        this.status.update(ICPUStatus.CARRY, oBit == 1);
        if (rora) {
            this.raUpdate(value);
        } else {
            this.bus.WriteU8(addr, value);
            this.NZUpdate(value);
        }
    }

    private void ASLImpl(AddressMode mode) {
        int b;
        var a = (mode == AddressMode.Accumulator);
        var address = -1;
        if (a) {
            b = this.ra;
        } else {
            address = this.bus.getAbsAddr(mode);
            b = this.bus.ReadU8(address);
        }
        //Check Carry flag
        this.status.update(ICPUStatus.CARRY, (b >> 7) == 1);
        //Left shifter one bit
        b = b << 1;
        if (a) {
            this.raUpdate(b);
        } else {
            this.NZUpdate(b);
            this.bus.WriteU8(address, b);
        }
    }

    private void PUSHImpl(Instruction instruction) {
        if (instruction == Instruction.PHA) {
            this.push(int8(this.ra));
        } else {
            var flags = this.status.copy();
            flags.set(ICPUStatus.BREAK_COMMAND, ICPUStatus.B_FLAG);
            this.push(flags.getBits());
        }
    }

    private void PULLImpl(Instruction instruction) {
        var value = this.pop();
        if (instruction == Instruction.PLA) {
            this.raUpdate(value);
        } else {
            this.status.setBits(value);
            this.status.set(ICPUStatus.B_FLAG);
            this.status.clear(ICPUStatus.BREAK_COMMAND);
        }
    }

    private void CMPImpl(Instruction instruction, AddressMode mode) {
        var val = switch (instruction) {
            case CMP -> this.ra;
            case CPX -> this.rx;
            default -> this.ry;
        };
        var address = this.bus.getAbsAddr(mode);
        var m = this.bus.ReadU8(address);
        //Set carry Flag
        this.status.update(ICPUStatus.CARRY, val >= m);
        //Update cpu status
        this.NZUpdate(u8sbc(val, m));
    }

    private void INCImpl(Instruction instruction, AddressMode mode) {
        final int result;
        if (instruction == Instruction.INC) {
            var address = this.bus.getAbsAddr(mode);
            var m = this.bus.ReadU8(address);
            result = u8add(m, 1);
            this.bus.WriteU8(address, result);
        } else if (instruction == Instruction.INX) {
            this.rx = result = u8add(this.rx, 1);
        } else {
            this.ry = result = u8add(this.ry, 1);
        }
        this.NZUpdate(result);
    }


    private void ADCImpl(AddressMode mode, boolean sbc) {
        var addr = this.bus.getAbsAddr(mode);
        var b = this.bus.read(addr);
        if (sbc) {
            /* The absolute value of a negative number may be larger than the size
             * of the corresponding positive number, so here needs `-b -1` after
             * taking the opposite number.
             * */
            b = int8(-b - 1);
        }
        var value = uint8(b);
        var sum = this.ra + value + this.status.get(ICPUStatus.CARRY);
        this.status.update(ICPUStatus.CARRY, sum > 0xff);
        var result = uint8(sum);
        this.status.update(ICPUStatus.OVERFLOW, (((b & 0xff ^ result) & (result ^ this.ra)) & 0x80) != 0);
        this.raUpdate(result);
    }


    private void LDXYImpl(Instruction instruction, AddressMode mode) {
        var address = this.bus.getAbsAddr(mode);
        var data = this.bus.ReadU8(address);
        if (instruction == Instruction.LDX) {
            this.rx = data;
        } else {
            this.ry = data;
        }
        this.NZUpdate(data);
    }

    private void CheckBranchCondition(boolean condition) {
        //If condition was false not anything.
        if (!condition) {
            return;
        }
        this.bus.increment();

        var b = this.bus.read(this.pc);
        var jump = this.pc + 1 + b;
        var base = this.pc + 1;

        // Page-cross check
        this.bus.pageCross(base, jump);

        this.pc = jump;
    }

    private void BITImpl(AddressMode mode) {
        var value = this.bus.ReadU8(this.bus.getAbsAddr(mode));
        this.status.update(ICPUStatus.ZERO, (this.ra & value) == 0);
        this.status.update(ICPUStatus.NEGATIVE, (value >> 7) == 1);
        this.status.update(ICPUStatus.OVERFLOW, (value >> 6) == 1);
    }

    private void DEYImpl(Instruction instruction, AddressMode mode) {
        var value = switch (instruction) {
            case DEC -> {
                var address = this.bus.getAbsAddr(mode);
                var b = u8sbc(this.bus.ReadU8(address), 1);
                this.bus.WriteU8(address, b);
                yield b;
            }
            case DEX -> this.rx = u8sbc(this.rx, 1);
            default -> this.ry = u8sbc(this.ry, 1);
        };
        this.NZUpdate(value);
    }

    private void RTImpl() {
        this.status.setBits(this.pop());
        this.status.set(ICPUStatus.B_FLAG);
        this.status.clear(ICPUStatus.BREAK_COMMAND);
        this.pc = this.popInt();
    }

    private void LASImpl(AddressMode mode) {
        var addr = this.bus.getAbsAddr(mode);
        var value = this.bus.ReadU8(addr);
        value = value & this.sp;
        this.rx = value;
        this.sp = value;
        this.raUpdate(value);
    }

    private void LAXImpl(AddressMode mode) {
        var addr = this.bus.getAbsAddr(mode);
        var value = this.bus.ReadU8(addr);
        this.raUpdate(value);
        this.rx = value;
    }

    private void SAXImpl(AddressMode mode) {
        var data = this.ra & this.rx;
        var addr = this.bus.getAbsAddr(mode);
        this.bus.WriteU8(addr, data);
    }

    private void RRAImpl(AddressMode mode) {
        this.RORImpl(mode);
        this.ADCImpl(mode, false);
    }

    private void ARRImpl(AddressMode mode) {
        var addr = this.bus.getAbsAddr(mode);
        var b = this.bus.ReadU8(addr);
        this.raUpdate(b & this.ra);
        this.RORImpl(AddressMode.Accumulator);
        var result = this.ra;
        var b5 = (result >> 5 & 1);
        var b6 = (result >> 6 & 1);
        this.status.update(ICPUStatus.CARRY, b6 == 1);
        this.status.update(ICPUStatus.OVERFLOW, (b5 ^ b6) == 1);
        this.NZUpdate(result);
    }

    private void DCPImpl(AddressMode mode) {
        var addr = this.bus.getAbsAddr(mode);
        var value = this.bus.ReadU8(addr);
        value = u8sbc(value, 1);
        this.bus.WriteU8(addr, value);
        if (value <= this.ra) {
            this.status.set(ICPUStatus.CARRY);
        }
        this.NZUpdate(u8sbc(this.ra, value));
    }

    private void XAAImpl(AddressMode mode) {
        this.raUpdate(this.rx);
        var addr = this.bus.getAbsAddr(mode);
        var b = this.bus.ReadU8(addr);
        this.raUpdate(b & this.ra);
    }

    private void SHXImpl(AddressMode mode) {
        var addr = this.bus.getAbsAddr(mode);
        var value = this.rx & u8add(uint8(addr >> 8), 1);
        this.bus.WriteU8(addr, value);
    }

    private void LXAImpl(AddressMode mode) {
        this.LDAImpl(mode);
        this.rx = this.ra;
        this.NZUpdate(this.rx);
    }

    private void SRE_LSRImpl(Instruction instruction, AddressMode mode) {
        this.LSRImpl(mode);
        if (instruction == Instruction.SRE) {
            this.LogicImpl(Instruction.EOR, mode);
        }
    }

    private void TAXImpl() {
        this.rx = this.ra;
        this.NZUpdate(this.rx);
    }

    private void TAYImpl() {
        this.ry = this.ra;
        this.NZUpdate(this.ry);
    }

    private void TSXImpl() {
        this.rx = this.sp;
        this.NZUpdate(this.rx);
    }

    private void JSRImpl() {
        this.pushInt(this.pc + 1);
        this.pc = this.bus.readInt(this.pc);
    }

    private void SLOImpl(AddressMode mode) {
        this.ASLImpl(mode);
        this.LogicImpl(Instruction.ORA, mode);
    }

    private void ISCImpl(AddressMode mode) {
        this.INCImpl(Instruction.INC, mode);
        this.ADCImpl(mode, true);
    }

    private void RLAImpl(AddressMode mode) {
        this.ROLImpl(mode);
        this.ADCImpl(mode, false);
    }

    private void ALRImpl(AddressMode mode) {
        this.LogicImpl(Instruction.AND, mode);
        this.LSRImpl(mode);
    }

    private void ANCImpl(AddressMode mode) {
        this.ADCImpl(mode, false);
        this.status.update(ICPUStatus.CARRY, this.status.contain(ICPUStatus.NEGATIVE));
    }

    private void RTSImpl() {
        this.pc = this.popInt() + 1;
    }

    private void JMPImpl(AddressMode mode) {
        this.pc = this.bus.getAbsAddr(mode);
    }

    private void TXSImpl() {
        this.sp = this.rx;
    }

    private void STA_X_YImpl(Instruction instruction, AddressMode mode) {
        var value = switch (instruction) {
            case STA -> this.ra;
            case STX -> this.rx;
            default -> this.ry;
        };
        this.bus.WriteU8(this.bus.getAbsAddr(mode), value);
    }

    private void CLC_D_I_VImpl(Instruction instruction) {
        switch (instruction) {
            case CLC -> this.status.clear(ICPUStatus.CARRY);
            case CLV -> this.status.clear(ICPUStatus.OVERFLOW);
            case CLD -> this.status.clear(ICPUStatus.DECIMAL_MODE);
            case CLI -> this.status.clear(ICPUStatus.INTERRUPT_DISABLE);
        }
    }

    private void BPL_BMImpl(Instruction instruction) {
        this.CheckBranchCondition((instruction == Instruction.BMI) == this.status.contain(ICPUStatus.NEGATIVE));
    }

    private void BEQ_BNEImpl(Instruction instruction) {
        this.CheckBranchCondition((instruction == Instruction.BEQ) == this.status.contain(ICPUStatus.ZERO));
    }

    private void BVC_BVSImpl(Instruction instruction) {
        this.CheckBranchCondition((instruction == Instruction.BVS) == this.status.contain(ICPUStatus.OVERFLOW));
    }

    private void BCS_BCCImpl(Instruction instruction) {
        this.CheckBranchCondition((instruction == Instruction.BCS) == this.status.contain(ICPUStatus.CARRY));
    }

    private void SEC_D_Impl(Instruction instruction) {
        switch (instruction) {
            case SEC -> this.status.set(ICPUStatus.CARRY);
            case SED -> this.status.set(ICPUStatus.DECIMAL_MODE);
            case SEI -> this.status.set(ICPUStatus.INTERRUPT_DISABLE);
        }
    }

    private void LOG_Impl() {
        var utf8Str = this.bus.readUTF8Str(this.pc);
        var evalStr = ScriptUtil.evalTStr(utf8Str, console);
        var hook = this.console.getHook();
        if (hook == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Read utf8 character sequence:{}", evalStr);
            }
        } else {
            hook.logger(utf8Str, evalStr);
        }
    }

    public int NMI_IRQ_BRKInterrupt(CPUInterrupt interrupt) {
        //When ICPUStatus#INTERRUPT_DISABLE flag was set, all interrupts except the NMI are inhibited.
        if (this.status.contain(ICPUStatus.INTERRUPT_DISABLE) && interrupt == CPUInterrupt.IRQ) {
            return 0;
        }

        this.pushInt(this.pc);
        this.push(this.status.getBits());

//        var flag = this.status.copy();
//
//        flag.set(ICPUStatus.B_FLAG);
//        flag.update(ICPUStatus.BREAK_COMMAND, interrupt == CPUInterrupt.BRK);

//        this.push(flag.getBits());

        this.status.set(ICPUStatus.INTERRUPT_DISABLE);

        this.pc = this.bus.readInt(interrupt.getVector());

        return interrupt.getCycle();
    }

    public byte getStatus() {
        return status.getBits();
    }

    public void next() {
        var openCode = this.bus.read0(this.pc);
        var state = (++this.pc);

        var wrap = MWS6502.get(openCode);
        if (wrap == null) {
            logger.warning("Unknown opecode 0x{} in address 0x{}", Integer.toHexString(uint8(openCode)), Integer.toHexString(state - 1));
            return;
        }
        var mode = wrap.addrMode();
        var instruction = wrap.instruction();

        if (logger.isTraceEnabled()) {
            var operand = "";
            if (mode != AddressMode.Implied && mode != AddressMode.Accumulator && mode != AddressMode.Relative) {
                operand = "0x" + Integer.toHexString(this.bus.getAbsAddr(mode));
            }
            logger.trace(
                    "[0x{}] A:{} X:{} Y:{} S:{} {}({}) {}",
                    Integer.toHexString(this.pc - 1),
                    Integer.toHexString(this.ra),
                    Integer.toHexString(this.rx),
                    Integer.toHexString(this.ry),
                    this.status,
                    instruction,
                    wrap.cycle(),
                    operand
            );
        }

        switch (instruction) {
            case RTI -> this.RTImpl();
            case JSR -> this.JSRImpl();
            case RTS -> this.RTSImpl();
            case TAX -> this.TAXImpl();
            case TAY -> this.TAYImpl();
            case TSX -> this.TSXImpl();
            case TXS -> this.TXSImpl();
            case LOG -> this.LOG_Impl();
            case ASL -> this.ASLImpl(mode);
            case ROL -> this.ROLImpl(mode);
            case ROR -> this.RORImpl(mode);
            case BIT -> this.BITImpl(mode);
            case SLO -> this.SLOImpl(mode);
            case ISC -> this.ISCImpl(mode);
            case RLA -> this.RLAImpl(mode);
            case ALR -> this.ALRImpl(mode);
            case ANC -> this.ANCImpl(mode);
            case XAA -> this.XAAImpl(mode);
            case ARR -> this.ARRImpl(mode);
            case DCP -> this.DCPImpl(mode);
            case LAS -> this.LASImpl(mode);
            case LAX -> this.LAXImpl(mode);
            case SHX -> this.SHXImpl(mode);
            case LXA -> this.LXAImpl(mode);
            case SAX -> this.SAXImpl(mode);
            case RRA -> this.RRAImpl(mode);
            case LDA -> this.LDAImpl(mode);
            case JMP -> this.JMPImpl(mode);
            case TYA -> this.raUpdate(this.ry);
            case TXA -> this.raUpdate(this.rx);
            case SBC -> this.ADCImpl(mode, true);
            case ADC -> this.ADCImpl(mode, false);
            case PHA, PHP -> this.PUSHImpl(instruction);
            case PLA, PLP -> this.PULLImpl(instruction);
            case BPL, BMI -> this.BPL_BMImpl(instruction);
            case BEQ, BNE -> this.BEQ_BNEImpl(instruction);
            case BVC, BVS -> this.BVC_BVSImpl(instruction);
            case BCS, BCC -> this.BCS_BCCImpl(instruction);
            case LDX, LDY -> this.LDXYImpl(instruction, mode);
            case SEC, SED, SEI -> this.SEC_D_Impl(instruction);
            case SRE, LSR -> this.SRE_LSRImpl(instruction, mode);
            case DEC, DEX, DEY -> this.DEYImpl(instruction, mode);
            case CMP, CPX, CPY -> this.CMPImpl(instruction, mode);
            case INC, INX, INY -> this.INCImpl(instruction, mode);
            case BRK -> this.NMI_IRQ_BRKInterrupt(CPUInterrupt.BRK);
            case AND, ORA, EOR -> this.LogicImpl(instruction, mode);
            case STA, STY, STX -> this.STA_X_YImpl(instruction, mode);
            case CLC, CLD, CLI, CLV -> this.CLC_D_I_VImpl(instruction);
        }

        this.instructions++;
        this.bus._finally(wrap);

        //
        // Judge whether it is necessary to change the value of the program counter according to whether the
        // redirection occurs
        //
        if (this.pc == state) {
            this.pc += (wrap.size() - 1);
        }
    }

    public long getCycles() {
        return this.bus.getCycles();
    }

    public static WS6502 IS6502Get(byte openCode) {
        return MWS6502.get(openCode);
    }
}
