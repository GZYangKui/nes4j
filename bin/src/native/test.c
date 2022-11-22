#include "include/sys_sound.h"

int main(int argc, char **argv) {
    double buffer[1] = {1.5f};
    Nes4j_apu_play(buffer, sizeof(buffer));
    return 0;
}