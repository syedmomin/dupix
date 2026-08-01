package dev.bit.dupix.engine

import android.net.Uri
import dev.bit.dupix.domain.engine.DuplicateFinder
import dev.bit.dupix.domain.engine.FileHasher
import dev.bit.dupix.domain.engine.StreamOpener
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Test

/**
 * Pure-JVM tests for the staged duplicate pipeline. Files are in-memory byte arrays keyed
 * by path, so no Android I/O is involved. The Uri on each FileItem is a Mockito mock
 * because the finder never dereferences it.
 */
class DuplicateFinderTest {

    private val store = HashMap<String, ByteArray>()
    private val opener = StreamOpener { item -> ByteArrayInputStream(store.getValue(item.path)) }
    private val finder = DuplicateFinder(FileHasher(opener))

    private fun file(path: String, bytes: ByteArray, lastModified: Long = 0L): FileItem {
        store[path] = bytes
        return FileItem(
            // Uri is irrelevant to the finder (it hashes via StreamOpener keyed by path);
            // a mock avoids Uri.parse's @NonNull assertion under plain JVM unit tests.
            uri = Mockito.mock(Uri::class.java),
            displayName = path.substringAfterLast('/'),
            path = path,
            size = bytes.size.toLong(),
            lastModified = lastModified,
            category = FileCategory.DOCUMENT,
        )
    }

    @Test
    fun `identical content is grouped as a duplicate`() = runTest {
        val content = "hello world".toByteArray()
        val files = listOf(
            file("a.txt", content, lastModified = 100),
            file("b.txt", content.copyOf(), lastModified = 200),
        )
        val groups = finder.find(files)
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].files.size)
    }

    @Test
    fun `same size but different bytes are not duplicates`() = runTest {
        val files = listOf(
            file("a.bin", byteArrayOf(1, 2, 3, 4)),
            file("b.bin", byteArrayOf(4, 3, 2, 1)),
        )
        val groups = finder.find(files)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `different sizes are never compared`() = runTest {
        val files = listOf(
            file("a.bin", byteArrayOf(1, 2, 3)),
            file("b.bin", byteArrayOf(1, 2, 3, 4)),
        )
        val groups = finder.find(files)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `keep-best keeps the oldest copy`() = runTest {
        val content = ByteArray(200_000) { (it % 7).toByte() } // > partial chunk to exercise tail
        val files = listOf(
            file("new.dat", content, lastModified = 5_000),
            file("original.dat", content.copyOf(), lastModified = 1_000),
            file("mid.dat", content.copyOf(), lastModified = 3_000),
        )
        val groups = finder.find(files)
        assertEquals(1, groups.size)
        assertEquals("original.dat", groups[0].keep.displayName)
        assertEquals(2, groups[0].duplicates.size)
    }

    @Test
    fun `empty files are ignored`() = runTest {
        val files = listOf(
            file("a.txt", ByteArray(0)),
            file("b.txt", ByteArray(0)),
        )
        val groups = finder.find(files)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `three way group reports correct reclaimable bytes`() = runTest {
        val content = ByteArray(1000) { 42 }
        val files = listOf(
            file("x1", content, lastModified = 1),
            file("x2", content.copyOf(), lastModified = 2),
            file("x3", content.copyOf(), lastModified = 3),
        )
        val groups = finder.find(files)
        assertEquals(1, groups.size)
        // keep 1, delete 2 => 2 * 1000 bytes reclaimable.
        assertEquals(2000L, groups[0].reclaimableBytes)
    }

    // Silence unused import warning for InputStream in some toolchains.
    private fun unused(s: InputStream) = s
}
