package dev.bit.dupix.data.scanner

import android.content.ContentResolver
import dev.bit.dupix.domain.engine.StreamOpener
import dev.bit.dupix.domain.model.FileItem
import java.io.InputStream

/** [StreamOpener] backed by the Android [ContentResolver]. */
class ContentStreamOpener(private val resolver: ContentResolver) : StreamOpener {
    override fun open(item: FileItem): InputStream =
        resolver.openInputStream(item.uri) ?: error("Cannot open stream for ${item.uri}")
}
