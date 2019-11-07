package com.dming.smallScan

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Vibrator
import java.io.Closeable
import java.io.IOException


/**
 * 管理声音和震动
 */
class BeepVibrateManager(
    private val activity: Activity,
    private val playBeep: Boolean,
    private val vibrate: Boolean
) : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, Closeable {
    private var mediaPlayer: MediaPlayer? = null

    init {
        this.mediaPlayer = null
        updatePrefs()
    }

    @Synchronized
    private fun updatePrefs() {
        if (playBeep && mediaPlayer == null) {
            mediaPlayer = buildMediaPlayer(activity)
        }
    }

    /**
     * 开启响铃和震动
     */
    @Synchronized
    fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            val vibrator = activity
                .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(200L)
        }
    }

    private fun buildMediaPlayer(activity: Context): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        // 监听是否播放完成
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
        // 配置播放资源
        try {
            val file = activity.resources
                .openRawResourceFd(R.raw.beep)
            try {
                mediaPlayer.setDataSource(
                    file.fileDescriptor,
                    file.startOffset, file.length
                )
            } finally {
                file.close()
            }
            // 设置音量
            mediaPlayer.prepare()
            return mediaPlayer
        } catch (ioe: IOException) {
            mediaPlayer.release()
            return null
        }

    }

    override fun onCompletion(mp: MediaPlayer) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0)
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required
            // and finish
            activity.finish()
        } else {
            // possibly media player error, so release and recreate
            mp.release()
            mediaPlayer = null
            updatePrefs()
        }
        return true
    }

    override fun close() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

}
