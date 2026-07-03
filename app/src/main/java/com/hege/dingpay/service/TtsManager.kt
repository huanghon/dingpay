package com.hege.dingpay.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.hege.dingpay.data.BroadcastLanguage
import com.hege.dingpay.data.ParsedPayment
import com.hege.dingpay.data.PaymentRule
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val ready = AtomicBoolean(false)
    private val failed = AtomicBoolean(false)
    private val pendingPhrases = ArrayDeque<PendingPhrase>()
    private val tts = TextToSpeech(appContext, this)
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onInit(status: Int) {
        runOnMain {
            handleInit(status)
        }
    }

    private fun handleInit(status: Int) {
        val initialized = status == TextToSpeech.SUCCESS
        ready.set(initialized)
        failed.set(!initialized)
        if (initialized) {
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build()
            )
            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        abandonAudioFocus()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        abandonAudioFocus()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.w(TAG, "TextToSpeech utterance failed id=$utteranceId error=$errorCode")
                        abandonAudioFocus()
                    }
                }
            )
            flushPending()
        } else {
            Log.w(TAG, "TextToSpeech initialization failed status=$status")
        }
    }

    fun speakPayment(rule: PaymentRule, payment: ParsedPayment, language: BroadcastLanguage): TtsSpeakResult {
        val amount = "%.2f".format(payment.amount)
        val zhPhrase = "${rule.title} ${payment.currency}$amount"
        val esPhrase = "Pago recibido ${payment.currency}$amount"
        val phrase = when (language) {
            BroadcastLanguage.ZH -> zhPhrase
            BroadcastLanguage.ES -> esPhrase
        }
        val fallbackPhrase = when (language) {
            BroadcastLanguage.ZH -> esPhrase
            BroadcastLanguage.ES -> zhPhrase
        }
        val fallbackLanguage = when (language) {
            BroadcastLanguage.ZH -> BroadcastLanguage.ES
            BroadcastLanguage.ES -> BroadcastLanguage.ZH
        }
        return speak(phrase, language, fallbackPhrase, fallbackLanguage)
    }

    fun test(language: BroadcastLanguage): TtsSpeakResult {
        val zhPhrase = "到账语音提醒测试"
        val esPhrase = "Prueba de aviso de pago recibido"
        val phrase = if (language == BroadcastLanguage.ZH) zhPhrase else esPhrase
        val fallbackPhrase = if (language == BroadcastLanguage.ZH) esPhrase else zhPhrase
        val fallbackLanguage = if (language == BroadcastLanguage.ZH) BroadcastLanguage.ES else BroadcastLanguage.ZH
        return speak(phrase, language, fallbackPhrase, fallbackLanguage)
    }

    fun isReady(): Boolean = ready.get()

    fun shutdown() {
        runOnMain {
            tts.stop()
            tts.shutdown()
            abandonAudioFocus()
        }
    }

    private fun speak(
        phrase: String,
        language: BroadcastLanguage,
        fallbackPhrase: String,
        fallbackLanguage: BroadcastLanguage
    ): TtsSpeakResult {
        if (failed.get()) {
            return TtsSpeakResult.failed("TTS 引擎初始化失败，请检查系统语音设置")
        }
        if (!ready.get()) {
            enqueuePending(phrase, language, fallbackPhrase, fallbackLanguage)
            Log.d(TAG, "Queue speech while TextToSpeech initializes")
            return TtsSpeakResult.queued("TTS 正在初始化，已加入播报队列")
        }
        return callOnMain {
            speakNow(phrase, language, fallbackPhrase, fallbackLanguage)
        }
    }

    private fun enqueuePending(
        phrase: String,
        language: BroadcastLanguage,
        fallbackPhrase: String,
        fallbackLanguage: BroadcastLanguage
    ) {
        synchronized(pendingPhrases) {
            if (pendingPhrases.size >= MAX_PENDING_PHRASES) {
                pendingPhrases.removeFirst()
            }
            pendingPhrases.addLast(PendingPhrase(phrase, language, fallbackPhrase, fallbackLanguage))
        }
    }

    private fun flushPending() {
        val phrases = synchronized(pendingPhrases) {
            buildList {
                while (pendingPhrases.isNotEmpty()) {
                    add(pendingPhrases.removeFirst())
                }
            }
        }
        phrases.forEach { speakNow(it.phrase, it.language, it.fallbackPhrase, it.fallbackLanguage) }
    }

    private fun speakNow(
        phrase: String,
        language: BroadcastLanguage,
        fallbackPhrase: String,
        fallbackLanguage: BroadcastLanguage
    ): TtsSpeakResult {
        val speech = chooseSpeech(phrase, language, fallbackPhrase, fallbackLanguage)
            ?: return TtsSpeakResult.failed("TTS 缺少可用语音包，请在系统 TTS 设置里安装中文或西语语音")
        val started = runCatching {
            SpeechPlaybackService.start(
                appContext,
                speech.phrase,
                speech.language,
                speech.fallbackPhrase,
                speech.fallbackLanguage
            )
        }.getOrElse { error ->
            Log.e(TAG, "Failed to start speech playback service", error)
            false
        }
        if (started) {
            Log.d(TAG, "Submit speech playback service language=${speech.language}")
            return TtsSpeakResult.submitted("TTS 已提交前台播报服务")
        }
        Log.w(TAG, "Foreground speech service unavailable; fallback to in-process TTS")
        return speakInProcess(speech.phrase)
    }

    /**
     * 前台播报服务启动失败时的降级路径：
     * 直接用本进程的 TTS 引擎 speak（监听进程由系统绑定保活，可直接发声）。
     * chooseSpeech 已把引擎语言设置为选中语言，这里无需再切换。
     */
    private fun speakInProcess(phrase: String): TtsSpeakResult {
        requestAudioFocus()
        val utteranceId = UUID.randomUUID().toString()
        val status = tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, utteranceId)
        return if (status == TextToSpeech.SUCCESS) {
            TtsSpeakResult.submitted("前台播报服务不可用，已降级为进程内直接播报")
        } else {
            abandonAudioFocus()
            Log.w(TAG, "In-process TTS fallback failed status=$status")
            TtsSpeakResult.failed("前台播报服务不可用，进程内直接播报也失败（status=$status）")
        }
    }

    private fun chooseSpeech(
        phrase: String,
        language: BroadcastLanguage,
        fallbackPhrase: String,
        fallbackLanguage: BroadcastLanguage
    ): PendingPhrase? {
        val locale = localeOf(language)
        val languageStatus = tts.setLanguage(locale)
        if (isLanguageAvailable(languageStatus)) {
            return PendingPhrase(phrase, language, fallbackPhrase, fallbackLanguage)
        }

        val fallbackLocale = localeOf(fallbackLanguage)
        val fallbackStatus = tts.setLanguage(fallbackLocale)
        if (isLanguageAvailable(fallbackStatus)) {
            Log.w(TAG, "TextToSpeech language unavailable: $locale status=$languageStatus; fallback=$fallbackLocale")
            return PendingPhrase(fallbackPhrase, fallbackLanguage, phrase, language)
        }

        val defaultLocale = Locale.getDefault()
        val defaultStatus = tts.setLanguage(defaultLocale)
        if (isLanguageAvailable(defaultStatus)) {
            Log.w(TAG, "TextToSpeech fallback language unavailable: $fallbackLocale status=$fallbackStatus; default=$defaultLocale")
            return PendingPhrase(phrase, language, fallbackPhrase, fallbackLanguage)
        }

        Log.w(TAG, "TextToSpeech has no usable language status=$languageStatus fallbackStatus=$fallbackStatus defaultStatus=$defaultStatus")
        return null
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

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                .build()
                .also { audioFocusRequest = it }
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        runOnMain {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun callOnMain(block: () -> TtsSpeakResult): TtsSpeakResult {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }
        val latch = CountDownLatch(1)
        val result = AtomicReference<TtsSpeakResult>()
        val posted = mainHandler.post {
            try {
                result.set(block())
            } finally {
                latch.countDown()
            }
        }
        return if (posted) {
            if (latch.await(MAIN_THREAD_RESULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                result.get() ?: TtsSpeakResult.failed("TTS 主线程没有返回播报结果")
            } else {
                TtsSpeakResult.submitted("TTS 已切到主线程提交播报")
            }
        } else {
            TtsSpeakResult.failed("TTS 主线程调度失败")
        }
    }

    private data class PendingPhrase(
        val phrase: String,
        val language: BroadcastLanguage,
        val fallbackPhrase: String,
        val fallbackLanguage: BroadcastLanguage
    )

    data class TtsSpeakResult(
        val accepted: Boolean,
        val queued: Boolean,
        val message: String
    ) {
        companion object {
            fun submitted(message: String) = TtsSpeakResult(accepted = true, queued = false, message = message)
            fun queued(message: String) = TtsSpeakResult(accepted = true, queued = true, message = message)
            fun failed(message: String) = TtsSpeakResult(accepted = false, queued = false, message = message)
        }
    }

    private companion object {
        const val TAG = "DingPayTts"
        const val MAX_PENDING_PHRASES = 5
        const val MAIN_THREAD_RESULT_TIMEOUT_MS = 1500L
    }
}
