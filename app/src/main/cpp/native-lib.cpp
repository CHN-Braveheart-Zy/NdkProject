#include <jni.h>
#include <string>
#include "librtmp/rtmp.h"
#include "utils.h"

Live *live = nullptr;

int sendVideo(jbyte *data, jint len, jlong tms);

int sendAudio(jbyte *data, jint len, jint type, jlong tms);

int sendPacket(RTMPPacket *pPacket);

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_zjp_ndkproject_screen_1live_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    int ret;
    do {
        live = (Live *) (malloc(sizeof(Live)));
        memset(live, 0, sizeof(live));
        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        if (!(ret = RTMP_SetupURL(live->rtmp, (char *) url))) break;
        LOGI("connect %s", url);
        RTMP_EnableWrite(live->rtmp);
        LOGI("RTMP_Connect");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) break;
        LOGI("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) break;
        LOGI("connect success");
    } while (false);
    if (!ret && live) {
        free(live);
        live = nullptr;
    }
    env->ReleaseStringUTFChars(_url, url);
    return ret;
}




extern "C"
JNIEXPORT jboolean JNICALL
Java_com_zjp_ndkproject_screen_1live_ScreenLive_sendData(JNIEnv *env, jobject thiz,
                                                         jbyteArray _buffer, jint len, jint type,
                                                         jlong tms) {
    jbyte *data = env->GetByteArrayElements(_buffer, NULL);

    int ret;
    switch (type) {
        case 0:
            ret = sendVideo(data, len, tms);
            break;
        default:
            ret = sendAudio(data, len, type, tms);
            break;
    }
    env->ReleaseByteArrayElements(_buffer, data, 0);
    return ret;
}

int sendAudio(jbyte *data, jint len, jint type, jlong tms) {
    int ret;
    do {
        RTMPPacket *audioPacket = createAudioPacket(data, len, tms, type);
        ret = sendPacket(audioPacket);
    } while (0);
    return ret;
}

int sendPacket(RTMPPacket *packet) {
    return 0;
}

int sendVideo(jbyte *buf, jint len, jlong tms) {
    int ret;
    do {
        if (buf[4] == 0x67) {//sps pps
            if (live && (!live->sps || !live->pps)) {   //准备视频
                prepareVideo(buf,len,tms,live);
            }
        } else {
            if (buf[4] == 0x65) {//关键帧
                RTMPPacket *packet = createVideoPackage(live);
                if (!(ret = sendPacket(packet))) {
                    break;
                }
            }
            //将编码之后的数据 按照 flv、rtmp的格式 拼好之后
            RTMPPacket *packet = createVideoPackage(buf, len, tms, live);
            ret = sendPacket(packet);
        }
    } while (0);
    return ret;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_zjp_ndkproject_screen_1live_ScreenLive_disconnect(JNIEnv *env, jobject thiz) {
    if (live) {
        if (live->sps) {
            free(live->sps);
        }
        if (live->pps) {
            free(live->pps);
        }
        if (live->rtmp) {
            RTMP_Close(live->rtmp);
            RTMP_Free(live->rtmp);
        }
        free(live);
        live = nullptr;
    }
}