package com.hege.dingpay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hege.dingpay.R
import com.hege.dingpay.data.BroadcastLanguage
import java.io.File
import java.util.Locale
import java.util.UUID

class SpeechPlaybackService : Service(), TextToSpeech.OnInitListener {
    private val pendingRequests = ArrayDeque<SpeechRequest>()
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var ttsReady = false
    private var active = false

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = SpeechRequest.fromIntent(intent) ?: return START_NOT_STICKY
        pendingRequests.addLast(request)
        startForeground(NOTIFICATION_ID, buildNotification("正在准备语音播报"))
        drainQueue()
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (!ttsReady) {
            Log.w(TAG, "TextToSpeech initialization failed status=$status")
            stopAfterCurrent()
            return
        }
        tts?.setAudioAttributes(speechAudioAttributes())
        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    val file = synthesizedFile(utteranceId.orEmpty())
                    runCatching { playSynthesizedFile(file) }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to play synthesized speech", error)
                            stopAfterCurrent()
                        }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.w(TAG, "TextToSpeech synthesis failed id=$utteranceId")
                    stopAfterCurrent()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TextToSpeech synthesis failed id=$utteranceId error=$errorCode")
                    stopAfterCurrent()
                }
            }
        )
        drainQueue()
    }

    private fun drainQueue() {
        if (active || !ttsReady) return
        val request = pendingRequests.removeFirstOrNull() ?: run {
            stopAfterCurrent()
            return
        }
        active = true
        synthesize(request)
    }

    private fun synthesize(request: SpeechRequest) {
        val engine = tts ?: run {
            stopAfterCurrent()
            return
        }
        val selected = chooseSpeech(engine, request) ?: run {
            Log.w(TAG, "No usable TTS language for request=$request")
            stopAfterCurrent()
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        val file = synthesizedFile(utteranceId)
        file.parentFile?.mkdirs()
        file.delete()
        startForeground(NOTIFICATION_ID, buildNotification("正在语音播报"))
        val status = engine.synthesizeToFile(selected.phrase, null, file, utteranceId)
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech synthesizeToFile failed status=$status")
            stopAfterCurrent()
        }
    }

    private fun chooseSpeech(engine: TextToSpeech, request: SpeechRequest): SelectedSpeech? {
        val languageStatus = engine.setLanguage(localeOf(request.language))
        if (isLanguageAvailable(languageStatus)) {
            return SelectedSpeech(request.phrase, request.language)
        }

        val fallbackStatus = engine.setLanguage(localeOf(request.fallbackLanguage))
        if (isLanguageAvailable(fallbackStatus)) {
            return SelectedSpeech(request.fallbackPhrase, request.fallbackLanguage)
        }

        val defaultStatus = engine.setLanguage(Locale.getDefault())
        return if (isLanguageAvailable(defaultStatus)) {
            SelectedSpeech(request.phrase, request.language)
        } else {
            null
        }
    }

    private fun playSynthesizedFile(file: File) {
        requestAudioFocus()
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(speechAudioAttributes())
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                releasePlayer()
                file.delete()
                active = false
                drainQueue()
            }
            setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer failed what=$what extra=$extra")
                releasePlayer()
                file.delete()
                active = false
                drainQueue()
                true
            }
            prepare()
            start()
        }
    }

    private fun requestAudioFocus() {
        val manager = audioManager()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(speechAudioAttributes())
                .build()
                .also { audioFocusRequest = it }
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        abandonAudioFocus()
    }

    private fun stopAfterCurrent() {
        releasePlayer()
        active = false
        if (pendingRequests.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            drainQueue()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DingPay 语音播报",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "到账语音播报运行状态"
            setSound(null, null)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("DingPay")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun synthesizedFile(utteranceId: String): File {
        return File(cacheDir, "speech/$utteranceId.wav")
    }

    private fun speechAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
    }

    private fun audioManager(): AudioManager = getSystemService(AudioManager::class.java)

    private fun notificationManager(): NotificationManager = getSystemService(NotificationManager::class.java)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releasePlayer()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private data class SelectedSpeech(
        val phrase: String,
        val language: BroadcastLanguage
    )

    private data class SpeechRequest(
        val phrase: String,
        val language: BroadcastLanguage,
        val fallbackPhrase: String,
        val fallbackLanguage: BroadcastLanguage
    ) {
        fun toIntent(context: Context): Intent {
            return Intent(context, SpeechPlaybackService::class.java)
                .putExtra(EXTRA_PHRASE, phrase)
                .putExtra(EXTRA_LANGUAGE, language.name)
                .putExtra(EXTRA_FALLBACK_PHRASE, fallbackPhrase)
                .putExtra(EXTRA_FALLBACK_LANGUAGE, fallbackLanguage.name)
        }

        companion object {
            fun fromIntent(intent: Intent?): SpeechRequest? {
                if (intent == null) return null
                val phrase = intent.getStringExtra(EXTRA_PHRASE).orEmpty()
                val fallbackPhrase = intent.getStringExtra(EXTRA_FALLBACK_PHRASE).orEmpty()
                if (phrase.isBlank() || fallbackPhrase.isBlank()) return null
                return SpeechRequest(
                    phrase = phrase,
                    language = intent.getStringExtra(EXTRA_LANGUAGE).toLanguage(),
                    fallbackPhrase = fallbackPhrase,
                    fallbackLanguage = intent.getStringExtra(EXTRA_FALLBACK_LANGUAGE).toLanguage()
                )
            }
        }
    }

    companion object {
        private const val TAG = "DingPaySpeechService"
        private const val CHANNEL_ID = "dingpay_speech"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_PHRASE = "phrase"
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_FALLBACK_PHRASE = "fallback_phrase"
        private const val EXTRA_FALLBACK_LANGUAGE = "fallback_language"

        /**
         * 尝试启动前台播报服务。
         * Android 12+ 后台 FGS 限制会抛 ForegroundServiceStartNotAllowedException
         * （IllegalStateException 子类），省电策略也可能抛 SecurityException。
         * 返回 false 表示启动失败，调用方应走进程内降级播报。
         */
        fun start(
            context: Context,
            phrase: String,
            language: BroadcastLanguage,
            fallbackPhrase: String,
            fallbackLanguage: BroadcastLanguage
        ): Boolean {
            val request = SpeechRequest(phrase, language, fallbackPhrase, fallbackLanguage)
            return try {
                ContextCompat.startForegroundService(context, request.toIntent(context))
                true
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Foreground speech service start not allowed", error)
                false
            } catch (error: SecurityException) {
                Log.w(TAG, "Foreground speech service start rejected", error)
                false
            } catch (error: RuntimeException) {
                Log.w(TAG, "Foreground speech service start failed", error)
                false
            }
        }

        private fun String?.toLanguage(): BroadcastLanguage {
            return runCatching { BroadcastLanguage.valueOf(orEmpty()) }.getOrDefault(BroadcastLanguage.ZH)
        }

        private fun isLanguageAvailable(status: Int): Boolean {
            return status != TextToSpeech.LANG_MISSING_DATA && status != TextToSpeech.LANG_NOT_SUPPORTED
        }

        private fun localeOf(language: BroadcastLanguage): Locale {
            return when (language) {
                BroadcastLanguage.ZH -> Locale.CHINESE
                BroadcastLanguage.ES -> Locale.forLanguageTag("es-ES")
            }
        }
    }
}
