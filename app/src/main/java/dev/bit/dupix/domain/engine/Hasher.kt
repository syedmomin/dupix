package dev.bit.dupix.domain.engine

import dev.bit.dupix.domain.model.FileItem

/** Produces content hashes for files. Implemented by [FileHasher] and decorated by caches. */
interface Hasher {
    fun partialHash(item: FileItem): String
    fun fullHash(item: FileItem): String
}
