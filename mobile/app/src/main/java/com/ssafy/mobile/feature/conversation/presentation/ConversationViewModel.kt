package com.ssafy.mobile.feature.conversation.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mobile.core.audio.AndroidAudioRecorder
import com.ssafy.mobile.core.audio.AudioPlayer
import com.ssafy.mobile.core.audio.TtsPlayer
import com.ssafy.mobile.core.model.SignRecognitionEvent
import com.ssafy.mobile.core.network.NetworkMonitor
import com.ssafy.mobile.core.stt.SttEngine
import com.ssafy.mobile.core.stt.SttEvent
import com.ssafy.mobile.core.vision.SignRecognitionEngine
import com.ssafy.mobile.core.vision.landmark.LandmarkFrameResult
import com.ssafy.mobile.feature.conversation.domain.model.ChatMessage
import com.ssafy.mobile.feature.conversation.domain.model.MessageStatus
import com.ssafy.mobile.feature.conversation.domain.model.SenderType
import com.ssafy.mobile.feature.conversation.domain.repository.TranslateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SessionState {
    Idle,
    Active,
    Stopping,
}

@Suppress("TooManyFunctions")
@HiltViewModel
class ConversationViewModel
    @Inject
    constructor(
        private val signRecognitionEngine: SignRecognitionEngine,
        private val translateRepository: TranslateRepository,
        private val audioPlayer: AudioPlayer,
        private val ttsPlayer: TtsPlayer,
        private val sttEngine: SttEngine,
        private val networkMonitor: NetworkMonitor,
        private val androidAudioRecorder: AndroidAudioRecorder,
    ) : ViewModel() {
        private val _sessionState = MutableStateFlow(SessionState.Idle)
        val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

        private val _isOnline = MutableStateFlow(true)
        val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

        private val _lastGlosses = MutableStateFlow<List<String>>(emptyList())
        val lastGlosses: StateFlow<List<String>> = _lastGlosses.asStateFlow()

        private val _translatedText = MutableStateFlow("")
        val translatedText: StateFlow<String> = _translatedText.asStateFlow()

        private val _sttText = MutableStateFlow("")
        val sttText: StateFlow<String> = _sttText.asStateFlow()

        private var isTtsPlaying = false

        private val _micVolume = MutableStateFlow(0f)
        val micVolume: StateFlow<Float> = _micVolume.asStateFlow()

        private var translationJob: Job? = null
        private var completionTimerJob: Job? = null
        private var sttJob: Job? = null
        private var cloudSttJob: Job? = null
        private var currentSttSessionId = 0
        private var isResultsReceived = false
        private var isStoppedReceived = false

        init {
            // 네트워크 상태 모니터링
            viewModelScope.launch {
                networkMonitor.isOnline.collect { online ->
                    val wasOnline = _isOnline.value
                    _isOnline.value = online
                    if (
                        wasOnline != online &&
                        _sessionState.value == SessionState.Active &&
                        !isTtsPlaying
                    ) {
                        stopRecordingForStt()
                        startRecordingForStt()
                    }
                }
            }

            viewModelScope.launch {
                signRecognitionEngine.events.collect { event ->
                    if (_sessionState.value == SessionState.Active) {
                        handleEvent(event)
                    }
                }
            }
        }

        private fun handleEvent(event: SignRecognitionEvent) {
            if (event !is SignRecognitionEvent.Prediction) return

            val currentGlosses = _lastGlosses.value + event.gloss
            _lastGlosses.value = currentGlosses

            // 타이머 재시작 로직 인라인화
            completionTimerJob?.cancel()
            completionTimerJob =
                viewModelScope.launch {
                    kotlinx.coroutines.delay(COMPLETION_THRESHOLD_MS)
                    if (currentGlosses.isNotEmpty()) {
                        requestTranslation(currentGlosses)
                    }
                }
        }

        private fun requestTranslation(words: List<String>) {
            translationJob?.cancel()
            if (_isOnline.value) {
                // [온라인 모드] 기존 클라우드 번역 로직
                performCloudTranslation(words)
            } else {
                // [오프라인 모드] Fallback 로직
                performOfflineFallback(words)
            }
        }

        private fun performCloudTranslation(words: List<String>) {
            // 0. 교정 중인 부모 메시지 추가
            addOrUpdateMessage(
                text = "교정 중...",
                isFinal = false,
                senderType = SenderType.PARENT,
            )

            translationJob =
                viewModelScope.launch {
                    translateRepository
                        .translateSignToSpeech(words)
                        .onSuccess { response ->
                            _translatedText.value = response.correctedText
                            addOrUpdateMessage(
                                text = response.correctedText,
                                isFinal = true,
                                senderType = SenderType.PARENT,
                            )

                            response.audioBase64?.let { base64Audio ->
                                handleTtsSuccess(base64Audio)
                            } ?: run {
                                startRecordingForStt()
                            }

                            _lastGlosses.value = emptyList()
                        }.onFailure {
                            addOrUpdateMessage(
                                text = "번역에 실패했습니다. 다시 시도해 주세요.",
                                isFinal = true,
                                senderType = SenderType.PARENT,
                            )
                            startRecordingForStt()
                        }
                }
        }

        private fun performOfflineFallback(words: List<String>) {
            val fallbackText = words.joinToString(" ")
            _translatedText.value = fallbackText

            // 오프라인이므로 즉시 최종 메시지로 추가
            addOrUpdateMessage(
                text = fallbackText,
                isFinal = true,
                senderType = SenderType.PARENT,
            )

            // 시스템 TTS로 재생
            sttEngine.stopListening()
            isTtsPlaying = true
            ttsPlayer.speak(
                text = fallbackText,
                onComplete = { resumeListeningAfterTts() },
                onError = { resumeListeningAfterTts() },
            )

            _lastGlosses.value = emptyList()
        }

        private fun startRecordingForStt() {
            if (_sessionState.value != SessionState.Active || isTtsPlaying) return

            // 새 세션 시작 시 상태 초기화 (오염 방지)
            isResultsReceived = false
            isStoppedReceived = false

            if (_isOnline.value) {
                startCloudRecordingLoop()
            } else {
                startLocalSttListening()
            }
        }

        private fun startLocalSttListening() {
            cloudSttJob?.cancel()
            cloudSttJob = null
            androidAudioRecorder.stop()
            currentSttSessionId++
            sttEngine.startListening(currentSttSessionId)
        }

        private fun startCloudRecordingLoop() {
            if (cloudSttJob?.isActive == true) return

            sttEngine.stopListening()
            currentSttSessionId++

            cloudSttJob =
                viewModelScope.launch {
                    while (isActive && canUseCloudStt()) {
                        val fileName = "stt_audio_$currentSttSessionId"
                        val started = androidAudioRecorder.start(fileName)
                        if (!started) {
                            Log.w(TAG, "Cloud STT recorder start failed")
                            delay(CLOUD_STT_RETRY_DELAY_MS)
                            continue
                        }

                        Log.d(TAG, "Cloud STT recording started: $fileName")
                        val audioFile = waitForCloudSpeechFile()
                        if (audioFile != null && canUseCloudStt()) {
                            _sttText.value = "대화 내용을 분석 중입니다..."
                            performCloudStt(audioFile)
                            return@launch
                        }

                        delay(CLOUD_STT_RETRY_DELAY_MS)
                    }
                }
        }

        private suspend fun waitForCloudSpeechFile(): File? {
            val startedAt = System.currentTimeMillis()
            var speechDetected = false
            var lastVoiceAt = startedAt
            var peakAmplitude = 0
            var stopReason = CLOUD_STT_STOP_REASON_CANCELLED

            while (currentCoroutineContext().isActive && canUseCloudStt()) {
                delay(CLOUD_STT_POLL_INTERVAL_MS)

                val amplitude = androidAudioRecorder.getMaxAmplitude()
                peakAmplitude = maxOf(peakAmplitude, amplitude)
                _micVolume.value = amplitude.toFloat()

                val now = System.currentTimeMillis()
                val recordingDuration = now - startedAt
                if (amplitude >= CLOUD_STT_VOICE_THRESHOLD) {
                    speechDetected = true
                    lastVoiceAt = now
                }

                val currentStopReason =
                    getCloudRecordingStopReason(
                        speechDetected,
                        recordingDuration,
                        now - lastVoiceAt,
                    )
                if (currentStopReason != null) {
                    stopReason = currentStopReason
                    break
                }
            }

            val file = androidAudioRecorder.stop()
            val recordingDuration = System.currentTimeMillis() - startedAt
            Log.d(
                TAG,
                "Cloud STT recording stopped: path=${file?.absolutePath}, " +
                    "size=${file?.length()}, speechDetected=$speechDetected, " +
                    "peakAmplitude=$peakAmplitude, duration=$recordingDuration, " +
                    "reason=$stopReason",
            )

            return file?.takeIf {
                shouldUploadCloudAudioFile(
                    file = it,
                    speechDetected = speechDetected,
                    peakAmplitude = peakAmplitude,
                )
            } ?: run {
                Log.d(
                    TAG,
                    "Cloud STT recording discarded: speechDetected=$speechDetected, " +
                        "peakAmplitude=$peakAmplitude, size=${file?.length()}",
                )
                null
            }
        }

        private fun canUseCloudStt(): Boolean =
            _sessionState.value == SessionState.Active && _isOnline.value && !isTtsPlaying

        private fun getCloudRecordingStopReason(
            speechDetected: Boolean,
            recordingDuration: Long,
            silenceDuration: Long,
        ): String? =
            when {
                speechDetected &&
                    recordingDuration >= CLOUD_STT_MIN_RECORDING_MS &&
                    silenceDuration >= CLOUD_STT_SILENCE_TIMEOUT_MS ->
                    CLOUD_STT_STOP_REASON_SILENCE
                recordingDuration >= CLOUD_STT_MAX_RECORDING_MS ->
                    CLOUD_STT_STOP_REASON_MAX_DURATION
                !speechDetected &&
                    recordingDuration >= CLOUD_STT_NO_SPEECH_TIMEOUT_MS ->
                    CLOUD_STT_STOP_REASON_NO_SPEECH
                else -> null
            }

        private fun isValidCloudAudioFile(file: File): Boolean =
            file.exists() && file.length() >= CLOUD_STT_MIN_FILE_BYTES

        private fun shouldUploadCloudAudioFile(
            file: File,
            speechDetected: Boolean,
            peakAmplitude: Int,
        ): Boolean =
            isValidCloudAudioFile(file) &&
                (speechDetected || peakAmplitude >= CLOUD_STT_FALLBACK_VOICE_THRESHOLD)

        private suspend fun checkAndRestartStt() {
            if (isResultsReceived && isStoppedReceived) {
                // 잦은 재시작으로 인한 BUSY 에러 방지
                kotlinx.coroutines.delay(STT_RESTART_DELAY_MS)
                startRecordingForStt()
            }
        }

        private fun stopRecordingForStt(): File? {
            cloudSttJob?.cancel()
            cloudSttJob = null
            sttEngine.stopListening()
            return androidAudioRecorder.stop()
        }

        fun startSession() {
            _sessionState.value = SessionState.Active
            signRecognitionEngine.start()

            // 1. 기존 STT 수집이 있다면 취소
            sttJob?.cancel()

            // 2. STT 수집 시작
            sttJob =
                viewModelScope.launch {
                    sttEngine.events.collect { event ->
                        if (event.sessionId != currentSttSessionId || _isOnline.value) {
                            return@collect
                        }

                        when (event) {
                            is SttEvent.PartialResults -> {
                                isResultsReceived = false
                                isStoppedReceived = false
                                updateOrAddChildMessage(
                                    event.text,
                                    isFinal = false,
                                )
                            }
                            is SttEvent.Results -> {
                                updateOrAddChildMessage(
                                    event.text,
                                    isFinal = true,
                                )
                                isResultsReceived = true
                                checkAndRestartStt()
                            }
                            is SttEvent.EndOfSpeech -> {
                                // 필요 시 UI 처리 (예: "인식 완료, 처리 중...")
                            }
                            is SttEvent.Stopped -> {
                                if (_sessionState.value == SessionState.Active && !isTtsPlaying) {
                                    isStoppedReceived = true
                                    if (!_isOnline.value) {
                                        isResultsReceived = true // 오프라인은 결과 대기 불필요
                                    }
                                    checkAndRestartStt()
                                }
                            }
                            is SttEvent.VolumeChanged -> _micVolume.value = event.db
                            is SttEvent.Error -> {
                                Log.w(TAG, "Local STT error: ${event.message}")
                                isResultsReceived = true
                                isStoppedReceived = false
                                // 에러 처리 (필요시 UI 알림 추가)
                            }
                            else -> {}
                        }
                    }
                }
            startRecordingForStt()
        }

        private fun performCloudStt(audioFile: File) {
            viewModelScope.launch {
                var restartDelayMs = STT_RESTART_DELAY_MS
                Log.d(
                    TAG,
                    "Cloud STT upload started: path=${audioFile.absolutePath}, " +
                        "size=${audioFile.length()}, mime=audio/mp4",
                )
                translateRepository
                    .translateSpeechToText(audioFile, "audio/mp4")
                    .onSuccess { response ->
                        val displayText =
                            buildCloudSttDisplayText(
                                recognizedText = response.recognizedText,
                                correctedText = response.correctedText,
                                corrected = response.corrected,
                            )
                        updateOrAddChildMessage(displayText, isFinal = true)
                        _sttText.value = ""
                        Log.d(
                            TAG,
                            "Cloud STT upload succeeded: recognized=${response.recognizedText}, " +
                                "corrected=${response.correctedText}, isCorrected=${response.corrected}",
                        )
                    }.onFailure {
                        Log.w(TAG, "Cloud STT upload failed", it)
                        restartDelayMs = CLOUD_STT_FAILURE_RETRY_DELAY_MS
                        _sttText.value = "인식에 실패했습니다."
                    }

                if (_sessionState.value == SessionState.Active && !isTtsPlaying) {
                    delay(restartDelayMs)
                    startRecordingForStt()
                }
            }
        }

        private fun buildCloudSttDisplayText(
            recognizedText: String,
            correctedText: String,
            corrected: Boolean,
        ): String {
            val normalizedRecognized = recognizedText.trim()
            val normalizedCorrected = correctedText.trim()
            return if (
                corrected &&
                normalizedCorrected.isNotBlank() &&
                normalizedRecognized != normalizedCorrected
            ) {
                "원문: $normalizedRecognized\n수정: $normalizedCorrected"
            } else {
                normalizedRecognized.ifBlank { normalizedCorrected }
            }
        }

        private fun handleTtsSuccess(base64Audio: String) {
            stopRecordingForStt()
            isTtsPlaying = true
            audioPlayer.playBase64(
                base64Data = base64Audio,
                onComplete = { resumeListeningAfterTts() },
                onError = { resumeListeningAfterTts() },
            )
        }

        private fun resumeListeningAfterTts() {
            isTtsPlaying = false
            startRecordingForStt()
        }

        fun stopSession() {
            _sessionState.value = SessionState.Idle
            signRecognitionEngine.stop()
            stopRecordingForStt()
            isResultsReceived = false
            isStoppedReceived = false
            _lastGlosses.value = emptyList()
            _translatedText.value = ""
            _sttText.value = ""
            _micVolume.value = 0f
            _messages.value = emptyList()
            translationJob?.cancel()
            completionTimerJob?.cancel()
            sttJob?.cancel()
            cloudSttJob?.cancel()
            cloudSttJob = null
            audioPlayer.stop()
            ttsPlayer.stop()
        }

        private fun addOrUpdateMessage(
            text: String,
            isFinal: Boolean,
            senderType: SenderType,
        ) {
            _messages.update { currentList ->
                val lastMessage = currentList.lastOrNull()
                if (lastMessage != null &&
                    lastMessage.senderType == senderType &&
                    lastMessage.status == MessageStatus.PENDING
                ) {
                    currentList.dropLast(1) +
                        lastMessage.copy(
                            text = text,
                            status =
                                if (isFinal) {
                                    MessageStatus.COMPLETED
                                } else {
                                    MessageStatus.PENDING
                                },
                        )
                } else {
                    currentList +
                        ChatMessage(
                            text = text,
                            senderType = senderType,
                            status =
                                if (isFinal) {
                                    MessageStatus.COMPLETED
                                } else {
                                    MessageStatus.PENDING
                                },
                        )
                }
            }
        }

        private fun updateOrAddChildMessage(
            text: String,
            isFinal: Boolean,
        ) {
            _sttText.value = text
            addOrUpdateMessage(text, isFinal, SenderType.CHILD)
        }

        override fun onCleared() {
            super.onCleared()
            audioPlayer.stop()
            ttsPlayer.stop()
            stopRecordingForStt()
        }

        fun onLandmarkFrame(frame: LandmarkFrameResult) {
            if (_sessionState.value != SessionState.Active) return

            signRecognitionEngine.submitFrame(frame)
        }

        companion object {
            private const val TAG = "ConversationViewModel"
            private const val COMPLETION_THRESHOLD_MS = 2000L
            private const val STT_RESTART_DELAY_MS = 500L
            private const val CLOUD_STT_POLL_INTERVAL_MS = 150L
            private const val CLOUD_STT_MIN_RECORDING_MS = 500L
            private const val CLOUD_STT_SILENCE_TIMEOUT_MS = 500L
            private const val CLOUD_STT_MAX_RECORDING_MS = 2800L
            private const val CLOUD_STT_NO_SPEECH_TIMEOUT_MS = 1200L
            private const val CLOUD_STT_RETRY_DELAY_MS = 500L
            private const val CLOUD_STT_FAILURE_RETRY_DELAY_MS = 1500L
            private const val CLOUD_STT_VOICE_THRESHOLD = 5000
            private const val CLOUD_STT_FALLBACK_VOICE_THRESHOLD = 5000
            private const val CLOUD_STT_MIN_FILE_BYTES = 1024L
            private const val CLOUD_STT_STOP_REASON_SILENCE = "silence"
            private const val CLOUD_STT_STOP_REASON_MAX_DURATION = "max_duration"
            private const val CLOUD_STT_STOP_REASON_NO_SPEECH = "no_speech"
            private const val CLOUD_STT_STOP_REASON_CANCELLED = "cancelled"
        }
    }
