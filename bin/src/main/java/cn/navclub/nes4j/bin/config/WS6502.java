package cn.navclub.nes4j.bin.config;


public record WS6502(byte openCode, int size, int cycle, AddressMode addrMode, Instruction instruction) {
}