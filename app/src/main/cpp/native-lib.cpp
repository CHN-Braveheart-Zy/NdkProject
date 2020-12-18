#include <jni.h>
#include <string>
#include "librtmp/rtmp.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_zjp_ndkproject_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    RTMP_Alloc();
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
