#include <stdlib.h>
#include "include/sys_sound.h"

int main(int argc, char **argv) {
    double buffer[1024];
    usize length = sizeof(buffer) / sizeof(double);

    for (int i = 0; i < length; ++i) {
        long r = random();
        double a = r / (double) 0xffffffff;
        buffer[i] = a;
    }
    SoundHardware *hardware = Nes4j_find_hardware(1, True);
    Nes4j_apu_play(hardware, buffer, length);

    Nes4j_apu_stop(hardware);
    return 0;
}