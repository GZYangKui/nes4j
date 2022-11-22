#include <stdlib.h>
#include <stdio.h>
#include "include/sys_sound.h"

int main(int argc, char **argv) {
    double buffer[10];
    usize length = sizeof(buffer) / sizeof(double);

    for (int i = 0; i < length; ++i) {
        long r = random();
        double a = r / (double) 0xffffffff;
        buffer[i] = a;
    }

    Nes4j_apu_play(buffer, length);

    Nes4j_apu_stop();
    return 0;
}