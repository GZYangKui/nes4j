#include "include/sys_sound.h"


#ifdef __linux__

#include "alsa/asoundlib.h"
#include "include/memory.h"

static usize Nes4j_apu_play_linux(SoundHardware *hardware, const float *sample, usize length);

#endif

LinkedList *linked_list = NULL;

extern bool Nes4j_init_hardware(SoundHardware *config, SoundHardware **dst) {
    Object context = NULL;
#ifdef __linux__
    int err;
    snd_pcm_t *handle;
    if ((err = snd_pcm_open(&handle, config->device, SND_PCM_STREAM_PLAYBACK, SND_PCM_NONBLOCK)) < 0) {
        printf("Playback open error: %s\n", snd_strerror(err));
        return False;
    }
    if ((err = snd_pcm_set_params(handle,
                                  SND_PCM_FORMAT_FLOAT,
                                  SND_PCM_ACCESS_RW_INTERLEAVED,
                                  config->channel,
                                  config->rate,
                                  1,
                                  config->latency)) < 0) {
        printf("Playback open error: %s\n", snd_strerror(err));
    }
    //Open linux sound card fail
    if (err < 0) {
        return False;
    }
    context = handle;
#endif
    SoundHardware *hardware = Nes4j_sound_hardware_clone(config, context);
    LinkedList *node = Nes4j_new_linked_list(NULL, NULL, hardware);
    LinkedList *temp = linked_list;
    while (temp && temp->next) {
        temp = (LinkedList *) linked_list->next;
    }
    if (temp == NULL) {
        linked_list = node;
    } else {
        temp->next = (struct LinkedList *) node;
    }
    if (dst != NULL)
        *dst = hardware;
    return True;
}

extern usize Nes4j_apu_play(SoundHardware *hardware, const float *sample, usize length) {
    usize size = 0;
#ifdef __linux__
    size = Nes4j_apu_play_linux(hardware, sample, length);
#endif
    return size;
}

extern void Nes4j_apu_stop(SoundHardware *hardware) {
    if (hardware == NULL) {
        return;
    }
    LinkedList *temp = linked_list;
    while (temp) {
        Object obj = temp->content;
        if (obj == hardware) {
            LinkedList *pre = (LinkedList *) temp->pre;
            LinkedList *next = (LinkedList *) temp->next;
            if (next != NULL) {
                if (pre != NULL)
                    pre->next = (struct LinkedList *) next;
                else
                    linked_list = next;
            } else {
                if (pre == NULL) {
                    linked_list = NULL;
                }
            }
            break;
        }
        temp = (LinkedList *) temp->next;
    }
    if (temp != NULL) {
        Nes4j_linked_list_lose(&temp);
    }
    Nes4j_sound_hardware_close(&hardware);
}


static usize Nes4j_apu_play_linux(SoundHardware *hardware, const float *sample, usize length) {
    snd_pcm_sframes_t frames;
    snd_pcm_t *t = hardware->context;
    snd_pcm_sframes_t left = snd_pcm_avail(t);
    if (left <= 0) {
        return 0;
    }
    frames = snd_pcm_writei(t, sample, length);
    if (frames < 0)
        frames = snd_pcm_recover(t, frames, 0);
    if (frames < 0) {
        fprintf(stderr, "snd_pcm_writei failed：%s\n", snd_strerror(frames));
        return 0;
    }
    return frames;
}


extern SoundHardware *Nes4j_find_hardware(int id) {
    LinkedList *temp = linked_list;
    LinkedList *last = linked_list;
    while (temp) {
        SoundHardware *content = ((SoundHardware *) temp->content);
        int id0 = content->id;
        if (id == id0) {
            break;
        }
        last = temp;
        temp = (LinkedList *) temp->next;
    }
    return last ? last->content : NULL;
}

