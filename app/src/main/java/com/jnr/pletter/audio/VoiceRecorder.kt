package com.jnr.pletter.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/** 원본 음성을 앱 내부 저장소에 AAC(m4a)로 녹음합니다. 상시 녹음은 하지 않습니다. */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var output: File? = null

    val isRecording: Boolean get() = recorder != null

    @Throws(Exception::class)
    fun start() {
        cancel()
        val dir = File(context.filesDir, "letters").apply { mkdirs() }
        val out = File(dir, "${UUID.randomUUID()}.m4a")
        val r = newRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            out.delete()
            throw e
        }
        recorder = r
        output = out
    }

    /** 녹음을 끝내고 파일을 돌려줍니다. 너무 짧아 실패하면 null. */
    fun stop(): File? {
        val r = recorder ?: return null
        val out = output
        recorder = null
        output = null
        return try {
            r.stop()
            out
        } catch (e: RuntimeException) {
            out?.delete()
            null
        } finally {
            r.release()
        }
    }

    /** 진행 중인 녹음을 버립니다. 진행 중이 아니면 아무 일도 하지 않습니다. */
    fun cancel() {
        val r = recorder ?: return
        val out = output
        recorder = null
        output = null
        runCatching { r.stop() }
        r.release()
        out?.delete()
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
}
