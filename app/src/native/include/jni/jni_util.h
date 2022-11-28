#ifndef NATIVE_JNI_UTIL_H
#define NATIVE_JNI_UTIL_H

#include <jni.h>
#include "../type.h"

/**
 *
 * 获取java对象hash code
 *
 */
extern jint Nes4j_jni_hash_code(JNIEnv *, jobject);


extern void Nes4j_jni_runtime_exception(JNIEnv *, String);

extern void Nes4j_jni_throw_exception(JNIEnv *,String,String);

#endif //NATIVE_JNI_UTIL_H
