.include "common/nes.inc"

.segment           "CHARS"
.incbin        "common/ascii.chr"

.segment            "STARTUP"

start:
    sei
    clc
    lda #$80
    sta PPU_CTRL                    ;Enable val flag
    jmp waitvbl

irq:
    jmp waitvbl

nmi:
    lda #$00
    sta PPU_MASK
    lda PPU_STATUS                  ; Reset w to 0
    lda #$20
    sta PPU_ADDR
    lda #$01
    sta PPU_ADDR                    ; Set VRAM address to $2001
    ldx #$00
VENDOR_RENDER:                      ; Write vendor name(nes4j)
    lda VENDOR_TEXT,x
    sta PPU_DATA
    INX
    CPX #$05
    BNE VENDOR_RENDER
    LDA #$20                        ; Set VRAM address to $2041
    STA PPU_ADDR
    LDA #$41
    STA PPU_ADDR
    LDX #$00
RENDER_AUTHOR:                      ; Write author info
    LDA PAUTHOR,x
    STA PPU_DATA
    INX
    CPX #$1b
    BNE RENDER_AUTHOR
    LDA #$00
    STA PPU_SCROLL
    LDA PPU_STATUS                  ; Reset w to 0
    LDA #$3f                        ; Set VRAM address to $3f01
    STA PPU_ADDR
    LDA #$01
    STA PPU_ADDR
palette:
    STA PPU_DATA
    ADC #$01
    CMP #$0f
    BNE palette
    LDA #$00                              ; Set x scroll was 0
    STA PPU_SCROLL
    lda #$00
;    LDA #%11101000                        ; Scroll y to 31 (31+3)=
    STA PPU_SCROLL
    LDA #$80
    STA PPU_CTRL                    ;Switch to first name table
    lda #%00001010                  ;Enable background show
    sta PPU_MASK
    jmp forever


forever:
    jmp forever

waitvbl:
    jmp waitvbl

VENDOR_TEXT:
; nes4j
.byte $4e,$45,$53,$14,$4a
PAUTHOR:
;Author
.byte $21,$55,$54,$48,$4f,$52,$1a
AUTHOR:                             ;GZYangKui@github
.byte $27,$3a,$39,$41,$4e,$47,$2b,$55,$49,$20,$47,$49,$54,$48,$55,$42

