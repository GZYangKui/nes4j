#include "../include/jni/jni_util.h"

extern jint Nes4j_jni_hash_code(JNIEnv *env, jobject this) {
    jclass class = (*env)->GetObjectClass(env, this);
    jmethodID id = (*env)->GetMethodID(env, class, "hashCode", "()I");
    jint hash_code = (*env)->CallIntMethod(env, this, id);
    return hash_code;
}

extern void Nes4j_jni_runtime_exception(JNIEnv *env, String msg) {
    Nes4j_jni_throw_exception(env, "java/lang/RuntimeException", msg);
}

extern void Nes4j_jni_throw_exception(JNIEnv *env, String class, String msg) {
    jclass clazz = (*env)->FindClass(env, (const char *) clazz);
    jint code = (*env)->ThrowNew(env, clazz, msg);
    if (code != 0) {
        fprintf(stderr, "JNI Throw exception fail errcode:%d\n", code);
    }
}
