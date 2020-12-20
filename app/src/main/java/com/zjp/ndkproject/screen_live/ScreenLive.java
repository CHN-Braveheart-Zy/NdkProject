package com.zjp.ndkproject.screen_live;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import java.util.concurrent.LinkedBlockingDeque;

import static android.app.Activity.RESULT_OK;

public class ScreenLive implements Runnable {
    static {
        System.loadLibrary("native-lib");
    }
    public static final int REQUEST_CODE = 0X01;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private String url;
    private boolean isLiving;
    private LinkedBlockingDeque<RTMPPackage> queue = new LinkedBlockingDeque<>();

    public void startLive(String url, ScreenLiveActivity activity) {
        this.url = url;
        projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = projectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(screenCaptureIntent, REQUEST_CODE);
    }

    public void stopLive() {
        addPackage(RTMPPackage.EMPTY_PACKAGE);
        isLiving = false;
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            //mediaProjection.createVirtualDisplay()
            //连接rtmp服务器
            //获取数据编码
            AsyncTask.THREAD_POOL_EXECUTOR.execute(this);
        }
    }

    @Override
    public void run() {
        if (!connect(url)) {
            return;
        }
        isLiving = true;
        VideoCodec videoCodec = new VideoCodec(this);
        videoCodec.startLive(mediaProjection);

        AudioCodec audioCodec = new AudioCodec(this);
        audioCodec.startLive();
        boolean isSend = true;
        while (isLiving && isSend) {
            //发送数据包
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (Exception e) {
            }
            if (rtmpPackage == null) {
                break;
            }
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length > 0) {
                isSend = sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer().length,
                        rtmpPackage.getType(), rtmpPackage.getTms());
            }
        }
        isLiving = false;
        videoCodec.stopLive();
        audioCodec.stopLive();
        queue.clear();
        disconnect();
    }

    public native boolean connect(String url);

    public native boolean sendData(byte[] buffer, int len, int type, long tms);

    private native void disconnect();

    public void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }
}
