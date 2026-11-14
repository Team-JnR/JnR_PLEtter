package com.jnr.pletter.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 편지 재생기. 사용자가 직접 '듣기'를 눌렀을 때만 재생합니다(자동 재생 없음).
 * 오디오 포커스를 잃으면(내비 안내·통화 등) 바로 멈춥니다.
 */
class LetterPlayer(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null

    var isPlaying by mutableStateOf(false)
        private set

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change < 0) stop()
            }
            .build()

    fun play(path: String) {
        stop()
        val granted = audioManager.requestAudioFocus(focusRequest) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!granted) return
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(attributes)
            p.setDataSource(path)
            p.setOnCompletionListener { stop() }
            p.prepare()
            p.start()
            player = p
            isPlaying = true
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        player?.let { p ->
            runCatching { p.stop() }
            p.release()
        }
        player = null
        audioManager.abandonAudioFocusRequest(focusRequest)
        isPlaying = false
    }
}
