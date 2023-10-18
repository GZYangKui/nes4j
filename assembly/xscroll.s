.include "common/nes.inc"

.segment           "CHARS"
   .incbin        "common/ascii.chr"

.segment            "STARTUP"

start:
  sei
  clc
  lda #$80
  sta PPU_CTRL       ;Enable val flag
  jmp waitvbl

irq:
    jmp waitvbl

nmi:
    lda #$00
    sta PPU_MASK
    lda PPU_STATUS  ; Reset w to 0
    lda #$20
    sta PPU_ADDR
    lda #$00
    sta PPU_ADDR   ; Set VRAM address was $2000
    ldx #$00
fill:
    lda CONST_TEXT,x
    sta PPU_DATA
    INX
    CPX #$0C
    BNE fill
    lda #%00001010   ;Enable background show
    sta PPU_MASK
    jmp forever


 forever:
     jmp forever

 waitvbl:
     jmp waitvbl

CONST_TEXT:
   .byte 'h','e','l','l','0',',','w','o','r','l','d','.'
