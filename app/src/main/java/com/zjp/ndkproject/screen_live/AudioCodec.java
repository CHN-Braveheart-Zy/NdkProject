package com.zjp.ndkproject.screen_live;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioCodec extends Thread {

    private AudioRecord audioRecord;
    private boolean isRecording;
    private MediaCodec mediaCodec;
    private int minBufferSize;
    private long startTime;
    private ScreenLive screenLive;

    public AudioCodec(ScreenLive screenLive) {
        this.screenLive = screenLive;
    }

    public void startLive() {
        try {
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                    44100, 1);
            //编码规格
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //码率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128_000);
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        minBufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

        start();
    }

    @Override
    public void run() {
        isRecording = true;
        audioRecord.startRecording();
        mediaCodec.start();

        //音频编码包
        RTMPPackage rtmpPackage = new RTMPPackage();
        rtmpPackage.setBuffer(new byte[]{0x12, 0x08});
        rtmpPackage.setType(RTMPPackage.RTMP_PACKET_AUDIO_HEAD);
        rtmpPackage.setTms(0);
        screenLive.addPackage(rtmpPackage);

        byte[] buffer = new byte[minBufferSize];
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (isRecording) {
            int len = audioRecord.read(buffer, 0, buffer.length);
            if (len <= 0) {
                continue;
            }
            //得到输入队列下标, 这是要交给编码器编码的数据,假设输入队列满了,就不等,继续执行后面的代码
            int index = mediaCodec.dequeueInputBuffer(0);
            if (index >= 0) {
                //根据索引获取输入队列的缓冲区
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                //清空缓存区
                inputBuffer.clear();
                //将读取到的pcm数据添加到待编码的队列
                inputBuffer.put(buffer, 0, len);
                //通知容器,已准备好待编码的数据,可以进行编码了
                mediaCodec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0);
            }
            //获取已经编码好的数据 送去编码
            index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            //每次从编码器取完,再往解码器塞数据
            while (index >= 0 && isRecording) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                //已编码好的数据
                byte[] output = new byte[bufferInfo.size];
                outputBuffer.get(output);
                //todo:送去给rtmp封包
                if (startTime == 0) {
                    startTime = System.nanoTime() / 1000;
                }
                long timeUs = bufferInfo.presentationTimeUs / 1000;
                rtmpPackage = new RTMPPackage();
                rtmpPackage.setBuffer(output);
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_AUDIO_DATA);
                rtmpPackage.setTms(timeUs - startTime); //相对时间
                screenLive.addPackage(rtmpPackage);
                //释放内存
                mediaCodec.releaseOutputBuffer(index, false);
                //继续获取未编码完的数据
                index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        startTime = 0;

        isRecording = false;
    }

    public void stopLive() {
        isRecording = false;
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
