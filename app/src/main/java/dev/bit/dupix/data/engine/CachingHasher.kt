package dev.bit.dupix.data.engine

import dev.bit.dupix.data.local.HashCacheDao
import dev.bit.dupix.data.local.HashCacheEntity
import dev.bit.dupix.domain.engine.Hasher
import dev.bit.dupix.domain.model.FileItem

/**
 * Decorates a delegate [Hasher] with a Room-backed cache keyed by (path, size,
 * lastModified). A file that hasn't changed is never re-hashed across scans.
 */
class CachingHasher(
    private val delegate: Hasher,
    private val dao: HashCacheDao,
) : Hasher {

    override fun partialHash(item: FileItem): String {
        dao.findBlocking(item.path, item.size, item.lastModified)?.let { return it.partialHash }
        val ph = delegate.partialHash(item)
        dao.upsertBlocking(
            HashCacheEntity(item.path, item.size, item.lastModified, partialHash = ph, fullHash = null)
        )
        return ph
    }

    override fun fullHash(item: FileItem): String {
        val cached = dao.findBlocking(item.path, item.size, item.lastModified)
        cached?.fullHash?.let { return it }
        val fh = delegate.fullHash(item)
        // Preserve any known partial hash; compute one if we somehow lack it.
        val ph = cached?.partialHash ?: delegate.partialHash(item)
        dao.upsertBlocking(
            HashCacheEntity(item.path, item.size, item.lastModified, partialHash = ph, fullHash = fh)
        )
        return fh
    }
}
