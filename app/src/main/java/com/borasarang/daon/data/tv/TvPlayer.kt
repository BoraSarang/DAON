package com.borasarang.daon.data.tv

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
 * TV 한 채널을 재생하는 ExoPlayer 래퍼.
 *
 * 라디오와 달리 영상이 있으므로 [exoPlayer] 를 화면의 `PlayerView` 에 붙여 쓴다.
 * 플레이어는 첫 재생 때 만든다(콜드 스타트 예산 보호). 화면을 떠나면 [stop] 한다.
 */
class TvPlayer(private val context: Context) {

    private var player: ExoPlayer? = null

    /** 화면에서 `PlayerView.player` 로 붙일 인스턴스. 첫 재생 전에는 null. */
    val exoPlayer: ExoPlayer?
        get() = player

    var onBuffering: (() -> Unit)? = null
    var onPlaying: (() -> Unit)? = null
    var onEnded: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun play(stream: TvStream.Hls) {
        val exo = ensurePlayer()
        val source = HlsMediaSource.Factory(dataSourceFactory(stream))
            .createMediaSource(MediaItem.fromUri(stream.url))
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

    fun release() {
        player?.release()
        player = null
    }

    private fun dataSourceFactory(stream: TvStream.Hls): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(stream.headers)

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
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
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 9; KFMAWI) DAON/0.4"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000
    }
}
