package com.dming.glScan

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
class BeepVibrateManager(private val activity: Activity) :
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, Closeable {
    private var mMediaPlayer: MediaPlayer? = null
    private var mPlayBeep: Boolean = false
    private var mVibrate: Boolean = false

    fun updateConfigure(playBeep: Boolean, vibrate: Boolean = false) {
        mPlayBeep = playBeep
        mVibrate = vibrate
        if (mPlayBeep && mMediaPlayer == null) {
            mMediaPlayer = buildMediaPlayer(activity)
        }
    }

    /**
     * 开启响铃和震动
     */
    fun playBeepSoundAndVibrate() {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer!!.start()
        }
        if (mVibrate) {
            val vibrator = activity
                .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(200L)
        }
    }

    /**
     * 创建MediaPlayer
     */
    private fun buildMediaPlayer(activity: Context): MediaPlayer? {
        val mMediaPlayer = MediaPlayer()
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        // 监听是否播放完成
        mMediaPlayer.setOnCompletionListener(this)
        mMediaPlayer.setOnErrorListener(this)
        // 配置播放资源
        try {
            val file = activity.resources
                .openRawResourceFd(R.raw.beep)
            file.use { f ->
                mMediaPlayer.setDataSource(
                    f.fileDescriptor,
                    f.startOffset, f.length
                )
            }
            // 设置音量
            mMediaPlayer.prepare()
            return mMediaPlayer
        } catch (ioe: IOException) {
            mMediaPlayer.release()
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
        } else {
            mp.release()
            mMediaPlayer = null
        }
        return true
    }

    override fun close() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

}
