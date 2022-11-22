#ifndef NATIVE_SYS_SOUND_H
#define NATIVE_SYS_SOUND_H

#include "type.h"

/**
 *
 * 播放音频
 *
 */
extern void Nes4j_apu_play(const double *buffer, int length);

/**
 *
 * 停止正在播放音频资源
 *
 */
extern void Nes4j_apu_stop();

#endif //NATIVE_SYS_SOUND_H
