//
// Created by yangkui on 2022/11/22.
//

#include "include/sys_sound.h"

#ifdef __linux__

#include "alsa/asoundlib.h"

/* playback device */
static char *device = "default";

static void Nes4j_apu_play_linux(const double *buffer, int length);

#endif


extern void Nes4j_apu_play(const double *buffer, int length) {
#ifdef __linux__
    Nes4j_apu_play_linux(buffer, length);
#endif
}


static void Nes4j_apu_play_linux(const double *buffer, int length) {
    int err;
    snd_pcm_t *handle;
    snd_pcm_sframes_t frames;

    if ((err = snd_pcm_open(&handle, device, SND_PCM_STREAM_PLAYBACK, 0)) < 0) {
        printf("Playback open error: %s\n", snd_strerror(err));
        exit(EXIT_FAILURE);
    }
    if ((err = snd_pcm_set_params(handle,
                                  SND_PCM_FORMAT_FLOAT,
                                  SND_PCM_ACCESS_RW_INTERLEAVED,
                                  1,
                                  48000,
                                  1,
//                                  500000 /* 0.5sec */
                                  0)) < 0) {
        printf("Playback open error: %s\n", snd_strerror(err));
        exit(EXIT_FAILURE);
    }

    frames = snd_pcm_writei(handle, buffer, length);
    if (frames < 0)
        frames = snd_pcm_recover(handle, frames, 0);
    if (frames < 0) {
        printf("snd_pcm_writei failed: %s\n", snd_strerror(frames));
        return;
    }
    if (frames > 0 && frames < length) {
        printf("Short write (expected %li, wrote %li)\n", (long) sizeof(buffer), frames);
    }
    /* pass the remaining samples, otherwise they're dropped in close */
    err = snd_pcm_drain(handle);
    if (err < 0)
        printf("snd_pcm_drain failed: %s\n", snd_strerror(err));
    snd_pcm_close(handle);
}

