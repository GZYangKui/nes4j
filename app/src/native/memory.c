#include "include/memory.h"
#include "include/str_util.h"
#include <malloc.h>

#ifdef __linux__

#include <alsa/asoundlib.h>

#endif

extern SoundHardware *Nes4j_new_sound_hardware(int id, Object context) {
    SoundHardware *hardware = malloc(sizeof(SoundHardware));
    hardware->context = context;
    hardware->id = id;
    return hardware;
}

extern SoundHardware *Nes4j_sound_hardware_clone(SoundHardware *hardware, Object context) {
    SoundHardware *temp = Nes4j_new_sound_hardware(hardware->id, context);

    temp->rate = hardware->rate;
    temp->latency = hardware->latency;
    temp->channel = hardware->channel;
    temp->device = Nes4j_str_clone(hardware->device);

    return temp;

}


extern LinkedList *Nes4j_new_linked_list(Object pre, Object next, Object content) {
    LinkedList *linked_list = malloc(sizeof(LinkedList));
    linked_list->pre = pre;
    linked_list->next = next;
    linked_list->content = content;
    return linked_list;
}

extern void Nes4j_linked_list_lose(LinkedList **node) {
    if (node == NULL || *node == NULL) {
        return;
    }
    (*node)->pre = NULL;
    (*node)->next = NULL;
    (*node)->content = NULL;
    free((*node));
    (*node) = NULL;
}

extern void Nes4j_sound_hardware_close(SoundHardware **ptr) {
    SoundHardware *hardware = *ptr;
#ifdef __linux__
    snd_pcm_t *ctl = hardware->context;
    snd_pcm_drop(ctl);
    snd_pcm_close(ctl);
#endif
    if (hardware->device != NULL) {
        free(hardware->device);
        hardware->device = NULL;
    }
    *ptr = NULL;
}

