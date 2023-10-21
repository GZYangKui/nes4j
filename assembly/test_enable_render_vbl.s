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
.include       "common/nes.inc"


.segment      "CHARS"
.incbin      "common/ascii.chr"

.segment      "STARTUP"


start:
    SEI
    CLC
    LDA #$80                        ; enabl vbl flag
    STA PPU_CTRL
    LDA #%00001010                  ; enable background render
    STA PPU_MASK

waitvbl:
    jmp waitvbl

forever:
    jmp forever

nmi:
;    LDA #$00                        ;
;    LDA #%00001010                  ; enable background render
;    STA PPU_MASK
    LDA #$20                        ; set vram address to $2021
    STA PPU_ADDR
    LDA #$41
    STA PPU_ADDR
    LDX #$00                        ; 14*3=42 ppu cycle
wait257:
    INX                             ; 2
    CPX #$0a                        ; 2
    BNE wait257                     ; 2
    LDX #$1e                        ; Write 30 times
RENDER_CHAR:
    LDA #$21                        ; write 'a' char
    STA PPU_DATA
    DEX
    CPX #$00
    BNE RENDER_CHAR                 ; set vram addr to $3f01
    LDA #$3f
    STA PPU_ADDR
    LDA #$01
    STA PPU_ADDR
    LDA #$01
RENDER_PAL:                         ; Render palatte
    STA PPU_DATA
    ADC #$01
    CMP #$20
    BNE RENDER_PAL
    LDA #%10000000
    STA PPU_CTRL
;    LDA #%00001010
;    STA PPU_MASK
    LDA #$00
    STA PPU_SCROLL
    LDA #$00
    STA PPU_SCROLL
    JMP forever

irq:
    jmp forever
