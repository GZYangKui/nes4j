#include <malloc.h>
#include "../include/cn_navclub_nes4j_bin_core_APU.h"

#include "../include/sys_sound.h"

static SoundHardware *Nes4j_find_hardware_obj_hash_code(JNIEnv *, jobject, bool);

JNIEXPORT void JNICALL Java_cn_navclub_nes4j_bin_core_APU_play(JNIEnv *env, jobject this, jfloatArray array) {
    SoundHardware *hardware = Nes4j_find_hardware_obj_hash_code(env, this, False);
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

JNIEXPORT jboolean JNICALL Java_cn_navclub_nes4j_bin_core_APU_create(JNIEnv *env, jobject this) {
    SoundHardware *hardware = Nes4j_find_hardware_obj_hash_code(env, this, True);
    return hardware != NULL;

}

JNIEXPORT void JNICALL Java_cn_navclub_nes4j_bin_core_APU_stop(JNIEnv *env, jobject this) {
    SoundHardware *hardware = Nes4j_find_hardware_obj_hash_code(env, this, False);
    Nes4j_apu_stop(hardware);
}

static SoundHardware *Nes4j_find_hardware_obj_hash_code(JNIEnv *env, jobject this, bool auto_create) {
    jclass class = (*env)->GetObjectClass(env, this);
    jmethodID id = (*env)->GetMethodID(env, class, "hashCode", "()I");
    jint hash_code = (*env)->CallIntMethod(env, this, id);
    SoundHardware *hardware = (SoundHardware *) Nes4j_find_hardware(hash_code, auto_create);
    return hardware;
}



