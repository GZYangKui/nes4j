#include "include/sys_sound.h"

#ifdef __linux__

#include "alsa/asoundlib.h"

snd_pcm_t *handle = NULL;
/* playback device */
static char *device = "default";

static void Nes4j_apu_play_linux(const double *buffer, int length);

#endif


extern void Nes4j_apu_play(const double *buffer, int length) {
#ifdef __linux__
    Nes4j_apu_play_linux(buffer, length);
#endif
}

extern void Nes4j_apu_stop() {
#ifdef __linux__
    if (handle == NULL) {
        return;
    }
    int err = snd_pcm_drop(handle);
    if (err < 0)
        printf("snd_pcm_drop failed: %s\n", snd_strerror(err));
    snd_pcm_close(handle);
    handle = NULL;
#endif
}


static void Nes4j_apu_play_linux(const double *buffer, int length) {
    int err;

    if (handle == NULL) {
        if ((err = snd_pcm_open(&handle, device, SND_PCM_STREAM_PLAYBACK, SND_PCM_NONBLOCK)) < 0) {
            printf("Playback open error: %s\n", snd_strerror(err));
            exit(EXIT_FAILURE);
        }
        if ((err = snd_pcm_set_params(handle,
                                      SND_PCM_FORMAT_FLOAT,
                                      SND_PCM_ACCESS_RW_INTERLEAVED,
                                      1,
                                      48000,
                                      1,
                                      0)) < 0) {
            printf("Playback open error: %s\n", snd_strerror(err));
            exit(EXIT_FAILURE);
        }
    }

    snd_pcm_sframes_t frames;

    for (int i = 0; i < 16; ++i) {
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
    }
}

