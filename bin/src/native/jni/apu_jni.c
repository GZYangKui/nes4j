#include "../include/cn_navclub_nes4j_bin_core_APU.h"

#include "../include/sys_sound.h"

JNIEXPORT void JNICALL Java_cn_navclub_nes4j_bin_core_APU_play(JNIEnv *env, jclass class, jdoubleArray array) {
    jint length = (*env)->GetArrayLength(env, array);
    double dst[length];
    jboolean copy = JNI_FALSE;
    jdouble *temp = (*env)->GetDoubleArrayElements(env, array, &copy);
    if (!temp) {
        fprintf(stderr, "Get double array elements is NULL it was gc recovery?\n");
        return;
    }
    (*env)->ReleaseDoubleArrayElements(env, array, temp, JNI_ABORT);
    Nes4j_apu_play(dst, length);
}

JNIEXPORT jboolean JNICALL Java_cn_navclub_nes4j_bin_core_APU_isSupport(JNIEnv *env, jobject this) {
    jboolean support = JNI_FALSE;

#if defined __linux__
    support = JNI_TRUE;
#endif
    return support;
}

JNIEXPORT void JNICALL Java_cn_navclub_nes4j_bin_core_APU_stop(JNIEnv *env, jclass class) {
    Nes4j_apu_stop();
};