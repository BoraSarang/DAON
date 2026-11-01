package com.borasarang.daon.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

/**
 * 유튜브 라이브 한 채널을 유튜브 공식 임베드 플레이어로 재생하는 WebView.
 *
 * 유튜브는 HLS/DASH 매니페스트 주소를 앱에 주지 않는다(실측: watch 페이지에
 * `adaptiveFormats` 만 있고 `hlsManifestUrl`·`dashManifestUrl` 이 없다). 그래서 ExoPlayer 로는
 * 재생할 수 없고, `assets/yt_player.html` 의 IFrame Player API 를 WebView 에 띄운다. 공식
 * 플레이어만 쓰므로 약관 문제가 없고, 방송사가 임베드를 막은 영상은 오류로 안내한다.
 *
 * WebView 는 채널을 처음 볼 때 만들고 재생을 멈추거나 화면을 떠나면 [release] 로 반납한다
 * (메모리 예산 250MB 보호). 재생 상태는 `DaonYT` 자바스크립트 브리지로 받는다.
 */
class YouTubePlayer(context: Context) {

    /** 실제 재생이 시작됐을 때. */
    var onStarted: (() -> Unit)? = null

    /** 재생하지 못했을 때. 값은 로그용 사유다. */
    var onError: ((String) -> Unit)? = null

    private val hasPage: Boolean by lazy {
        runCatching { context.assets.open(ASSET).close() }.isSuccess
    }

    /**
     * 로컬 자산을 `https://appassets.androidplatform.net/assets/...` 로 내려준다.
     *
     * `loadDataWithBaseURL` 로 띄우면 유튜브가 Referer 를 확인하지 못해 플레이어 오류
     * `152` 를 준다(실측). 실제 https 출처로 내려주면 Referer·origin 이 맞는다.
     */
    private val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    private val bridge = object {
        @JavascriptInterface
        fun onState(state: String) {
            if (state == "1") onStarted?.invoke()
        }

        @JavascriptInterface
        fun onError(reason: String) {
            onError?.invoke(reason)
        }
    }

    val webView: WebView = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        overScrollMode = View.OVER_SCROLL_NEVER
        isLongClickable = false
        setOnLongClickListener { true }
        webViewClient = StrictClient()
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // 키오스크라 사용자가 누르지 않아도 바로 재생돼야 한다.
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = false
            loadWithOverviewMode = false
        }
        addJavascriptInterface(bridge, BRIDGE)
    }

    /** 채널을 바꿀 때마다 새 영상으로 페이지를 다시 만든다. */
    fun load(videoId: String) {
        if (!hasPage) {
            onError?.invoke("asset missing")
            return
        }
        webView.loadUrl("$BASE_URL/assets/$ASSET?v=$videoId")
    }

    /** WebView 와 재생 코덱을 반납한다. 반드시 메인 스레드에서 부른다. */
    fun release() {
        onStarted = null
        onError = null
        runCatching {
            webView.stopLoading()
            webView.removeJavascriptInterface(BRIDGE)
            webView.loadUrl("about:blank")
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    /** 유튜브 밖으로 나가는 링크(설정·공유·다른 영상)는 키오스크에서 열지 않는다. */
    private inner class StrictClient : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            val host = request.url.host?.lowercase() ?: return true
            if (host == ASSET_HOST) return false
            return host != "youtube.com" && !host.endsWith(".youtube.com")
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            // 메인 문서는 데이터 URL 이라 여기 걸리지 않는다. 걸린다면 네트워크 문제다.
            if (request.isForMainFrame) onError?.invoke("http ${error.errorCode}")
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: RenderProcessGoneDetail,
        ): Boolean {
            // 렌더러가 죽어도 앱은 살린다. 화면에는 다시 시도를 안내한다.
            onError?.invoke("renderer gone crash=${detail.didCrash()}")
            return true
        }
    }

    private companion object {
        const val ASSET = "yt_player.html"
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val BASE_URL = "https://appassets.androidplatform.net"
        const val BRIDGE = "DaonYT"
    }
}

/**
 * [YouTubePlayer] 의 WebView 를 화면에 붙인다.
 *
 * 전체 화면 전환 때 Compose 가 새 컨테이너를 만들기 때문에, 같은 WebView 를 옮겨 붙여
 * 스트림이 처음부터 다시 시작되지 않게 한다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeSurface(player: YouTubePlayer, modifier: Modifier) {
    AndroidView(
        factory = { context -> FrameLayout(context) },
        update = { frame ->
            val view = player.webView
            if (view.parent !== frame) {
                (view.parent as? ViewGroup)?.removeView(view)
                frame.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        modifier = modifier,
    )
}
