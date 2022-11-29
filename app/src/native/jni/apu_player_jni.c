#include <malloc.h>
#include "../include/jni/cn_navclub_nes4j_app_audio_NativePlayer.h"

#include "../include/sys_sound.h"
#include "../include/jni/jni_util.h"

static SoundHardware *Nes4j_hardware_instance(JNIEnv *, jobject);

JNIEXPORT void JNICALL
Java_cn_navclub_nes4j_app_audio_NativePlayer_play(JNIEnv *env, jobject this, jfloatArray array) {
    SoundHardware *hardware = Nes4j_hardware_instance(env, this);
    if (hardware == NULL) {
        fprintf(stderr, "Call before Please init SoundHardware.\n");
        return;
    }
    jint length = (*env)->GetArrayLength(env, array);
    jboolean copy = JNI_FALSE;
    jfloat *temp = (*env)->GetFloatArrayElements(env, array, &copy);
    if (!temp) {
        fprintf(stderr, "Get double array elements is NULL it was gc recovery?\n");
        return;
    }
    Nes4j_apu_play(hardware, temp, length);
    (*env)->ReleaseFloatArrayElements(env, array, temp, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_cn_navclub_nes4j_app_audio_NativePlayer_stop(JNIEnv *env, jobject this) {
    SoundHardware *hardware = Nes4j_hardware_instance(env, this);
    if (hardware != NULL)
        Nes4j_apu_stop(hardware);
}

JNIEXPORT void JNICALL
Java_cn_navclub_nes4j_app_audio_NativePlayer_config(JNIEnv *env, jobject this, jstring device, jint channel,
                                                    jint rate, jint latency) {
    SoundHardware *hardware = Nes4j_hardware_instance(env, this);
    if (hardware) {
        Nes4j_jni_runtime_exception(env, "Please not repeat config audio hardware.");
        return;
    }
    jboolean copy = False;
    const char *str = (*env)->GetStringUTFChars(env, device, &copy);
    SoundHardware config = {
            Nes4j_jni_hash_code(env, this),
            rate,
            latency,
            channel,
            str,
            NULL
    };
    bool success = Nes4j_init_hardware(&config, NULL);
    (*env)->ReleaseStringUTFChars(env, device, str);
    if (!success) {
        Nes4j_jni_runtime_exception(env, "Audio hardware init fail.");
    }
}

static SoundHardware *Nes4j_hardware_instance(JNIEnv *env, jobject this) {
    jint id = Nes4j_jni_hash_code(env, this);
    SoundHardware *hardware = (SoundHardware *) Nes4j_find_hardware(id);
    return hardware;
}



