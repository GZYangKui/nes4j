#include "include/memory.h"
#include <malloc.h>

#ifdef __linux__

#include <alsa/asoundlib.h>

#endif

extern SoundHardware *Nes4j_new_sound_hardware(int hash_code, Object context) {
    SoundHardware *hardware = malloc(sizeof(SoundHardware));
    hardware->context = context;
    hardware->hash_code = hash_code;
    return hardware;
}

extern LinkedList *Nes4j_new_linked_list(Object pre, Object next, Object content) {
    LinkedList *linked_list = malloc(sizeof(LinkedList));
    linked_list->content = content;
    linked_list->pre = pre;
    linked_list->next = next;
    return linked_list;
}

extern void Nes4j_sound_hardware_close(SoundHardware **ptr) {
    SoundHardware *hardware = *ptr;
#ifdef __linux__
    snd_pcm_t *ctl = hardware->context;
    snd_pcm_drop(ctl);
    snd_pcm_close(ctl);
#endif
    *ptr = NULL;
}

