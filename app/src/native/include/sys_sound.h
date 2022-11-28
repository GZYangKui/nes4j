#ifndef NATIVE_SYS_SOUND_H
#define NATIVE_SYS_SOUND_H

#include "type.h"

/**
 *
 * 查询音频实例
 *
 */
extern SoundHardware *Nes4j_find_hardware(int id);

/**
 * 初始化音频硬件参数
 *
 * @return 如果初始化成功则返回{@code True},否则返回{@code False}
 */
extern bool Nes4j_init_hardware(SoundHardware *, SoundHardware **);


/**
 *
 * 播放音频
 *
 */
extern void Nes4j_apu_play(SoundHardware *hardware, const float *sample, usize length);

/**
 *
 * 停止正在播放音频资源
 *
 */
extern void Nes4j_apu_stop(SoundHardware *hardware);

#endif //NATIVE_SYS_SOUND_H
