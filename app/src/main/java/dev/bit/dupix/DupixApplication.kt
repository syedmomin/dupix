package dev.bit.dupix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import dev.bit.dupix.util.CrashLogger

@HiltAndroidApp
class DupixApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }

    // Register the video-frame decoder so video thumbnails render, and cap image memory
    // (RGB_565 + bounded cache) so the many thumbnails from a deep scan don't OOM.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .allowRgb565(true)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .build()
}
