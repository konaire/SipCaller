package io.codebeavers.sipcaller.util

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Vibrator
import android.provider.Settings

/**
 * Created by Evgeny Eliseyev on 23/02/2018.
 */

class SoundManager private constructor() {
    private lateinit var context: Context
    private var ringtone: Ringtone? = null
    private var tone: ToneGenerator? = null

    companion object {
        private val instance: SoundManager by lazy { SoundManager() }

        fun getInstance(context: Context): SoundManager {
            instance.context = context
            return instance
        }
    }

    fun startTone(code: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)

        if (tone == null) {
            tone = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100)
        }

        am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
        am.isSpeakerphoneOn = false
        tone!!.startTone(code)
    }

    fun startRinging() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        vibrator.vibrate(longArrayOf(0, 1000, 1000), 1)

        if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {
            val ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI.toString()
            ringtone = RingtoneManager.getRingtone(context, Uri.parse(ringtoneUri))

            ringtone!!.play()
        }
    }

    fun stopTone() {
        tone?.stopTone()
        tone?.release()
        tone = null
    }

    fun stopRinging() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
        ringtone?.stop()
        ringtone = null
    }
}