package dev.bit.dupix.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders the animated `scanner.svg` (from assets) in a transparent WebView so its SMIL
 * scan-beam animation plays — VectorDrawable can't play SMIL, and Android has no native
 * SVG renderer. No JavaScript and no network are used (local asset only).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SvgScanLoader(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isClickable = false
                isFocusable = false
                with(settings) {
                    javaScriptEnabled = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    blockNetworkLoads = true
                }
                loadUrl("file:///android_asset/scanner.svg")
            }
        },
    )
}
