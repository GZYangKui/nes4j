;
; Apache License, Version 2.0
;
; Copyright (c) 2023 杨奎 (Kui Yang)
;
;
; Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
;
;   http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
;
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
    inx
    cpx #$05
    bne VENDOR_RENDER
    lda #$20                        ; Set VRAM address to $2041
    sta PPU_ADDR
    lda #$41
    sta PPU_ADDR
    ldx #$00
RENDER_AUTHOR:                      ; Write author info
    lda PAUTHOR,x
    sta PPU_DATA
    inx
    cpx #$1b
    bne RENDER_AUTHOR
    lda #$00
    sta PPU_SCROLL
    lda PPU_STATUS                  ; Reset w to 0
    lda #$3f                        ; Set VRAM address to $3f01
    sta PPU_ADDR
    lda #$01
    sta PPU_ADDR
palette:
    sta PPU_DATA
    adc #$01
    cmp #$0f
    bne palette
    lda #$00                              ; Set x scroll was 0
    sta PPU_SCROLL
    lda #$00
;    LDA #%11101000                        ; Scroll y to 31 (31+3)=
    sta PPU_SCROLL
    lda #$80
    sta PPU_CTRL                    ;Switch to first name table
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

