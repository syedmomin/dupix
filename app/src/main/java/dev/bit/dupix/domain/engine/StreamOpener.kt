package dev.bit.dupix.domain.engine

import dev.bit.dupix.domain.model.FileItem
import java.io.InputStream

/**
 * Abstraction over "give me the bytes of this file". Backed by ContentResolver in the
 * app, and by in-memory byte arrays in unit tests. Keeps [DuplicateFinder] pure and
 * testable without Android.
 */
fun interface StreamOpener {
    /** Opens a fresh stream for [item]. Caller closes it. Throws if unreadable. */
    fun open(item: FileItem): InputStream
}
