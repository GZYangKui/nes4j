#include "include/sys_sound.h"


#ifdef __linux__

#include "alsa/asoundlib.h"
#include "include/memory.h"

/* playback device */
static char *device = "default";

static void Nes4j_apu_play_linux(SoundHardware *hardware, const double *buffer, usize length);

#endif

LinkedList *linked_list = NULL;

extern void Nes4j_apu_play(SoundHardware *hardware, const double *buffer, usize length) {
#ifdef __linux__
    Nes4j_apu_play_linux(hardware, buffer, length);
#endif
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
            temp = NULL;
        }
    }
    Nes4j_sound_hardware_close(&hardware);
}


static void Nes4j_apu_play_linux(SoundHardware *hardware, const double *buffer, usize length) {
    snd_pcm_sframes_t frames;
    snd_pcm_t *t = hardware->context;
    for (int i = 0; i < 16; ++i) {
        frames = snd_pcm_writei(t, buffer, length);
        if (frames < 0)
            frames = snd_pcm_recover(t, frames, 0);
        if (frames < 0) {
            printf("snd_pcm_writei failed: %s\n", snd_strerror(frames));
            return;
        }
        if (frames > 0 && frames < length) {
            printf("Short write (expected %li, wrote %li)\n", (long) sizeof(buffer), frames);
        }
    }
}


extern SoundHardware *Nes4j_find_hardware(int has_code, bool auto_create) {
    LinkedList *temp = linked_list;
    LinkedList *last = linked_list;
    while (temp) {
        SoundHardware *content = ((SoundHardware *) temp->content);
        int hc = content->hash_code;
        if (has_code == hc) {
            break;
        }
        last = temp;
        temp = (LinkedList *) temp->next;
    }
    SoundHardware *hardware = NULL;
    if (temp)
        hardware = temp->content;
    if (!hardware && auto_create) {
        Object context = NULL;
#ifdef __linux__
        int err;
        snd_pcm_t *handle;
        if ((err = snd_pcm_open(&handle, device, SND_PCM_STREAM_PLAYBACK, SND_PCM_NONBLOCK)) < 0) {
            printf("Playback open error: %s\n", snd_strerror(err));
        }
        if ((err = snd_pcm_set_params(handle,
                                      SND_PCM_FORMAT_FLOAT,
                                      SND_PCM_ACCESS_RW_INTERLEAVED,
                                      1,
                                      48000,
                                      1,
                                      500000)) < 0) {
            printf("Playback open error: %s\n", snd_strerror(err));
        }
        //Open linux sound card fail
        if (err < 0) {
            return NULL;
        }
        context = handle;
#endif
        hardware = Nes4j_new_sound_hardware(has_code, context);
        LinkedList *node = Nes4j_new_linked_list(NULL, NULL, hardware);
        node->content = hardware;
        if (last != NULL) {
            last->next = (struct LinkedList *) node;
        } else {
            linked_list = node;
        }
    }
    return hardware;
}

