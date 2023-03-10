#include <stdlib.h>
#include <unistd.h>
#include <alsa/asoundlib.h>
#include "include/sys_sound.h"

int main(int argc, char **argv) {
    SoundHardware config = {
            1, 44100, 500000, 1, "default"
    };

    SoundHardware *hardware = NULL;
    bool success = Nes4j_init_hardware(&config, &hardware);
    if (!success) {
        fprintf(stderr, "Sound hardware init fail.");
        return 0;
    }
    usize i = 0;
    float buffer[1];
    usize length = sizeof(buffer) / sizeof(int);
    while (i < 16 * 1024) {
        for (int i = 0; i < length; ++i) {
            long r = rand();
            buffer[i] = r / (float) 0x7fffffff;
        }
        Nes4j_apu_play(hardware, buffer, length);
        i++;
    }
    Nes4j_apu_stop(hardware);

    return 0;
}