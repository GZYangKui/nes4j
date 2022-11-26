//
// Created by yangkui on 2022/11/22.
//

#ifndef NATIVE_MEMORY_H
#define NATIVE_MEMORY_H

#include "type.h"


extern SoundHardware *Nes4j_new_sound_hardware(int id, Object context);


extern SoundHardware *Nes4j_sound_hardware_clone(SoundHardware *,Object);

extern LinkedList *Nes4j_new_linked_list(Object pre, Object next, Object content);

extern void Nes4j_linked_list_lose(LinkedList **);

extern void Nes4j_sound_hardware_close(SoundHardware **);

#endif //NATIVE_MEMORY_H
