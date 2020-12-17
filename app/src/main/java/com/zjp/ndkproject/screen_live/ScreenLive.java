package com.zjp.ndkproject.screen_live;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import java.util.concurrent.ThreadPoolExecutor;

import static android.app.Activity.RESULT_OK;

public class ScreenLive implements Runnable{
    public static final int REQUEST_CODE = 0X01;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private String url;

    public void startLive(String url, ScreenLiveActivity activity) {
        this.url = url;
        projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = projectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(screenCaptureIntent, REQUEST_CODE);
    }

    public void stopLive() {

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
        VideoCodec videoCodec = new VideoCodec();
        videoCodec.startLive(mediaProjection);
    }

    public native boolean connect(String url);
}
