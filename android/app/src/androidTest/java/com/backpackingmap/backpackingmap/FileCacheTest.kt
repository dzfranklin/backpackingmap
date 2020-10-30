package com.backpackingmap.backpackingmap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTime::class)
class FileCacheTest {
    private lateinit var context: Context

    // NOTE: test isASingleton relies on .get not being primed
    private val subject: FileCache by lazy {
        FileCache.get(context, minCacheShrinkInterval = 1.seconds)
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        clearCacheDir()
    }

    @After
    fun teardown() = runBlocking {
        delay(100)
        clearCacheDir()
    }

    private fun clearCacheDir() {
        for (file in context.cacheDir.walk()) {
            if (file.isFile) {
                file.delete()
            }
        }
    }

    @Test
    fun isASingleton() = runBlocking {
        val subjects = (0..100)
            .map {
                async {
                    FileCache.get(context)
                }
            }
            .awaitAll()

        val first = subjects[0]
        for (other in subjects.subList(1, subjects.size)) {
            assertSame("Subjects are the same instance", other, first)
        }
    }

    @Test
    fun acceptsStringWrites() {
        subject.writeLater("Foo", "foo value".toByteArray())
    }

    @Test
    fun writesCanBeRead() {
        val k = "A Key"
        val v = "A value".toByteArray()
        subject.writeLater(k, v)

        runBlocking { delay(10) }

        assertTrue(subject.read(k).contentEquals(v))
    }

    @Test
    fun readingUnwrittenReturnsNull() {
        assertNull("Unwritten key", subject.read("unwritten key"))
    }

    @Test
    fun oneOfMultipleWritesWins() = runBlocking {
        val k = "k"
        val v1 = "v1".toByteArray()
        val v2 = "v2".toByteArray()
        val v3 = "v3".toByteArray()

        subject.writeLater(k, v1)
        subject.writeLater(k, v2)
        subject.writeLater(k, v3)

        delay(500)

        val value = subject.read(k)

        assertTrue(value.contentEquals(v1) || value.contentEquals(v2) || value.contentEquals(v3))
    }

    @Test
    fun multipleWritesToDifferentKeysWork() = runBlocking {
        for (n in 0..5_000) {
            subject.writeLater("k-$n", n.toString().toByteArray())
        }

        delay(1_000)

        for (n in 0..5_000) {
            val expected = n.toString().toByteArray()
            assertTrue(subject.read("k-$n").contentEquals(expected))
        }
    }

    @Test
    fun returnsNullForUnwrittenKey() {
        assertNull(subject.read("nonexistent"))
    }

    @Test
    fun oldEntriesAreDeleted() = runBlocking {
        val contents = ByteArray(500_000)
        for (n in 0..5_000) {
            subject.writeLater("k-$n", contents)
        }

        delay(10.seconds)

        val retained = (0..5_000)
            .filter { subject.read("k-$it") != null }


        // We only roughly assert the first retained item is large because we don't know the exact
        // order the files were written in
        assertTrue(retained[0] > 4_000)
    }
}