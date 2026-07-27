package dev.bit.dupix.domain.engine

import dev.bit.dupix.domain.model.FileItem
import java.security.MessageDigest

/**
 * Computes SHA-256 hashes for files, in two flavours:
 *  - [partialHash]: cheap fingerprint from the first and last [PARTIAL_CHUNK] bytes.
 *  - [fullHash]: full-content hash, used only to confirm a candidate duplicate.
 *
 * Two files with the same size and the same full hash are treated as byte-identical.
 */
class FileHasher(private val opener: StreamOpener) : Hasher {

    override fun partialHash(item: FileItem): String {
        val digest = MessageDigest.getInstance("SHA-256")
        opener.open(item).use { input ->
            val head = input.readNBytesCompat(PARTIAL_CHUNK)
            digest.update(head, 0, head.size)
            // If the file is larger than 2 chunks, fold in the tail as well. We do it by
            // reading fully but only digesting head + tail to stay cheap; for streams we
            // can't seek, so we track the last chunk while draining.
            if (item.size > PARTIAL_CHUNK) {
                val tail = ByteArray(PARTIAL_CHUNK)
                var tailLen = 0
                val buf = ByteArray(PARTIAL_CHUNK)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    if (n >= PARTIAL_CHUNK) {
                        System.arraycopy(buf, n - PARTIAL_CHUNK, tail, 0, PARTIAL_CHUNK)
                        tailLen = PARTIAL_CHUNK
                    } else {
                        // shift existing tail left, append new bytes
                        val keep = PARTIAL_CHUNK - n
                        if (tailLen > 0 && keep > 0) {
                            System.arraycopy(tail, PARTIAL_CHUNK - keep, tail, 0, keep)
                            System.arraycopy(buf, 0, tail, keep, n)
                            tailLen = minOf(PARTIAL_CHUNK, keep + n)
                        } else {
                            System.arraycopy(buf, 0, tail, 0, n)
                            tailLen = n
                        }
                    }
                }
                digest.update(tail, 0, tailLen)
            }
        }
        // Mix the size in so different-sized files never share a partial hash.
        digest.update(item.size.toString().toByteArray())
        return digest.digest().toHex()
    }

    override fun fullHash(item: FileItem): String {
        val digest = MessageDigest.getInstance("SHA-256")
        opener.open(item).use { input ->
            val buf = ByteArray(BUFFER)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().toHex()
    }

    companion object {
        const val PARTIAL_CHUNK = 64 * 1024
        private const val BUFFER = 128 * 1024
    }
}

private fun java.io.InputStream.readNBytesCompat(n: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream(minOf(n, 8192))
    val buf = ByteArray(minOf(n, 8192))
    var remaining = n
    while (remaining > 0) {
        val read = this.read(buf, 0, minOf(buf.size, remaining))
        if (read <= 0) break
        out.write(buf, 0, read)
        remaining -= read
    }
    return out.toByteArray()
}

private fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(HEX[v ushr 4])
        sb.append(HEX[v and 0x0F])
    }
    return sb.toString()
}

private const val HEX = "0123456789abcdef"
