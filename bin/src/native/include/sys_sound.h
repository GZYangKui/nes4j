#ifndef NATIVE_SYS_SOUND_H
#define NATIVE_SYS_SOUND_H

#include "type.h"

/**
 *
 * 查询音频实例如果不存在是否创建由{@param auto_create}决定
 *
 */
extern SoundHardware *Nes4j_find_hardware(int has_code, bool auto_create);


/**
 *
 * 播放音频
 *
 */
extern void Nes4j_apu_play(SoundHardware *hardware, const int *buffer, usize length);

/**
 *
 * 停止正在播放音频资源
 *
 */
extern void Nes4j_apu_stop(SoundHardware *hardware);

#endif //NATIVE_SYS_SOUND_H
