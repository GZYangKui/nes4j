#include <stdlib.h>
#include "include/sys_sound.h"

int main(int argc, char **argv) {
    int buffer[1024];
    usize length = sizeof(buffer) / sizeof(int );

    for (int i = 0; i < length; ++i) {
        long r = random();
        buffer[i] = r % 32768;
    }
    SoundHardware *hardware = Nes4j_find_hardware(1, True);
    Nes4j_apu_play(hardware, buffer, length);

    Nes4j_apu_stop(hardware);
    return 0;
}