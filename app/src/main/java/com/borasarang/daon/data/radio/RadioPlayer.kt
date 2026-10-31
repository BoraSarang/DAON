package com.borasarang.daon.data.radio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource

/**
 * 라디오 한 채널을 재생하는 ExoPlayer 래퍼.
 *
 * - 화면과 무관하게 플레이어 하나만 유지한다. 화면을 넘겨도 소리가 이어진다.
 * - 오디오 포커스는 ExoPlayer 에 맡긴다(전화·다른 앱이 오면 스스로 멈추고 돌아오면 다시 튼다).
 * - 모든 호출은 메인 스레드에서만 한다(ViewModel 의 viewModelScope).
 */
class RadioPlayer(private val context: Context) {

    private var player: ExoPlayer? = null

    /** 연결 중. */
    var onBuffering: (() -> Unit)? = null

    /** 실제로 소리가 나기 시작했다. */
    var onPlaying: (() -> Unit)? = null

    /** 방송이 끝났다(라이브에서는 드물다). */
    var onEnded: (() -> Unit)? = null

    /** 재생 실패. 값은 로그용 오류 설명이다(코드 이름 + 원인 클래스). */
    var onError: ((String) -> Unit)? = null

    fun play(url: String, volumeStep: Int) {
        val exo = ensurePlayer()
        exo.volume = volumeFraction(volumeStep)
        val source = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
        exo.setMediaSource(source)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun stop() {
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
    }

    /** [step] 은 0..[MAX_STEP] 범위. 넘치면 잘라 쓴다. */
    fun setVolume(step: Int) {
        player?.volume = volumeFraction(step)
    }

    fun release() {
        player?.release()
        player = null
    }

    private val dataSourceFactory: DefaultHttpDataSource.Factory by lazy {
        DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val exo = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exo.addListener(listener)
        player = exo
        return exo
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> onBuffering?.invoke()
                Player.STATE_READY -> if (player?.isPlaying == true) onPlaying?.invoke()
                Player.STATE_ENDED -> onEnded?.invoke()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) onPlaying?.invoke()
        }

        override fun onPlayerError(error: PlaybackException) {
            val cause = generateSequence(error.cause) { it.cause }.lastOrNull()
            onError?.invoke(
                "${error.errorCodeName} cause=${cause?.javaClass?.simpleName} " +
                    "msg=${cause?.message ?: error.message}",
            )
        }
    }

    companion object {
        const val MAX_STEP = 10

        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android) DAON/0.3"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000

        fun volumeFraction(step: Int): Float =
            step.coerceIn(0, MAX_STEP) / MAX_STEP.toFloat()
    }
}
