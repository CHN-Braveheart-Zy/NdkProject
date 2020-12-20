package com.zjp.ndkproject.screen_live;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zjp.ndkproject.R;

public class ScreenLiveActivity extends AppCompatActivity {

    private ScreenLive screenLive;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_live);
        screenLive = new ScreenLive();
    }

    public void startLive(View view) {
        screenLive.startLive("rtmp://192.168.56.101:12345/app", this);
    }

    public void stopLive(View view) {
        screenLive.stopLive();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        screenLive.onActivityResult(requestCode, resultCode, data);
    }
}
