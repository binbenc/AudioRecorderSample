package com.cxb.android.recordingsample

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import com.blankj.utilcode.util.PermissionUtils
import com.cxb.android.recordingsample.record.AudioRecorder
import com.cxb.android.recordingsample.record.RecordStreamListener
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), RecordStreamListener {

    private lateinit var mAudioRecorder: AudioRecorder

    private lateinit var mStartButton: AppCompatButton
    private lateinit var mPauseButton: AppCompatButton
    private lateinit var mPcmListButton: AppCompatButton
    private lateinit var mWavListButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verifyPermissions()

        mStartButton = findViewById(R.id.bt_start)
        mPauseButton = findViewById(R.id.bt_pause)
        mPcmListButton = findViewById(R.id.bt_pcm_list)
        mWavListButton = findViewById(R.id.bt_wav_list)

        mStartButton.setOnClickListener {
            handleStartButtonLogic()
        }

        mPauseButton.setOnClickListener {
            handleStopButtonLogic()
        }
    }

    private fun verifyPermissions() {
        PermissionUtils.permission(Manifest.permission.RECORD_AUDIO)
            .callback { isAllGranted, granted, deniedForever, denied ->
                if (isAllGranted) {
                    mAudioRecorder = AudioRecorder.getInstance()
                }
            }.request()
    }

    private fun handleStartButtonLogic() {
        if (!PermissionUtils.isGranted(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "没有录音权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (mAudioRecorder.status == AudioRecorder.Status.STATUS_NO_READY) {
            // 初始化录音
            mAudioRecorder.createDefaultAudio(SimpleDateFormat("yyyyMMddhhmmss").format(Date()))
            mAudioRecorder.startRecord(this)

            mStartButton.text = "停止录音"

            mPauseButton.isVisible = true
        } else {
            // 停止录音
            mAudioRecorder.stopRecord()
            mStartButton.text = "开始录音"
            mPauseButton.text = "暂停录音"

            mPauseButton.isVisible = false
        }
    }

    private fun handleStopButtonLogic() {
        if (!PermissionUtils.isGranted(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "没有录音权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (mAudioRecorder.status == AudioRecorder.Status.STATUS_START) {
            // 暂停录音
            mAudioRecorder.pauseRecord()
            mPauseButton.text = "暂停录音"
        } else {
            mAudioRecorder.startRecord(null)
            mPauseButton.text = "暂停录音"
        }
    }

    override fun recordOfByte(data: ByteArray, begin: Int, end: Int) {
        Log.d("xiaobin","当前分贝 ${calculateVolume(data)}")
    }

    private fun calculateVolume(buffer: ByteArray): Double {
        var sumVolume = 0.0
        var avgVolume = 0.0
        var volume = 0.0

        for (i in buffer.indices step 2) {
            val v1 = buffer[i].toInt() and 0xFF
            val v2 = buffer[i + 1].toInt() and 0xFF
            var temp = v1 + (v2 shl 8) // 小端

            if (temp >= 0x8000) {
                temp = 0xFFFF - temp
            }

            sumVolume += Math.abs(temp)
        }

        avgVolume = sumVolume / buffer.size / 2
        volume = Math.log10(1 + avgVolume) * 10

        return volume
    }
}