package com.zjp.ndkproject.screen_live;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoCodec extends Thread {

    private Surface inputSurface;
    private MediaCodec mediaCodec;
    private boolean isLiving;
    private long timestamp;
    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;
    private long startTime;
    private ScreenLive screenLive;

    public VideoCodec(ScreenLive screenLive) {
        this.screenLive = screenLive;
    }

    public void startLive(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        isLiving = true;
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 360, 640);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = mediaCodec.createInputSurface();
            virtualDisplay = mediaProjection.createVirtualDisplay("screen-codec", 360, 640, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, inputSurface, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }

    @Override
    public void run() {
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (isLiving) {

            //手动刷新关键帧
            if (timestamp != 0) {
                if (System.currentTimeMillis() - timestamp >= 2_000) {
                    Bundle bundle = new Bundle();
                    bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                    mediaCodec.setParameters(bundle);
                    timestamp = System.currentTimeMillis();
                }
            } else {
                timestamp = System.currentTimeMillis();
            }

            //开始获取编码数据
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 10);//微妙
            if (index >= 0) {
                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(index);
                byte[] buffer = new byte[bufferInfo.size];
                byteBuffer.get(buffer);

                //TODO:将编码好的数据送去rtmp格式进行封包,再发送
                if (startTime == 0) {
                    startTime = System.nanoTime() / 1000;
                }
                RTMPPackage rtmpPackage = new RTMPPackage();
                rtmpPackage.setTms(bufferInfo.presentationTimeUs - startTime);
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_AUDIO_DATA);
                rtmpPackage.setBuffer(buffer);
                screenLive.addPackage(rtmpPackage);
                System.out.println("add video packet:" + buffer.length);
                //释放index对应的buffer空间  render 解码才会用到,会将解码数据发送到surface
                mediaCodec.releaseOutputBuffer(index, false);
            }
        }
        //释放
        isLiving = false;
        mediaCodec.stop();
        mediaCodec.release();
        virtualDisplay.release();
        mediaProjection.stop();
        startTime = 0;
    }

    public void stopLive() {
        isLiving = false;
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
