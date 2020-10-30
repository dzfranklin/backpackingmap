package com.backpackingmap.backpackingmap

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/**
 * No guarantees as to ordering of reads and writes.
 *
 * Multiple write calls with the same prefix and key but different values will lead to one of the values
 * being cached, with no guarantee as to which.
 *
 * Pending writes and last accessed updates aren't persisted and may be lost if the application
 * shuts down.
 *
 * Singleton as assumes it can use the entire cache allocated to the app
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class FileCache private constructor(
    private val minCacheShrinkInterval: Duration,
    private val storageManager: StorageManager,
    private val cacheDir: File,
) : CoroutineScope {
    override val coroutineContext = CoroutineScope(Dispatchers.IO).coroutineContext

    // Begin public API

    fun read(key: String): ByteArray? {
        val file = fileFor(key)
        return try {
            recordReadLater(file, file.readBytes())
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IOException) {
            Timber.w(e, "Exception trying to read file")
            null
        }
    }

    fun writeLater(key: String, value: ByteArray) {
        queue.sendBlocking(Msg.Write(key, value))
    }

    // End public API

    private fun <T> recordReadLater(file: File, out: T?): T? {
        if (out == null) {
            return out
        }

        queue.sendBlocking(Msg.RecordRead(Clock.System.now(), file))

        return out
    }

    private val queue = Channel<Msg>(Channel.UNLIMITED)

    private val shrinkQueue = MutableSharedFlow<Unit>(0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST)

    init {
        createQueueWorkers()
        createShrinkQueueWorker()
    }

    @OptIn(ExperimentalTime::class, FlowPreview::class)
    private fun createShrinkQueueWorker() =
        launch {
            shrinkQueue
                .debounce(minCacheShrinkInterval)
                .collect {
                    shrinkIfNecessary()
                }
        }

    private fun createQueueWorkers() {
        for (n in 1..MAX_WORKERS) {
            launch {
                for (msg in queue) {
                    handleMsg(msg)
                    shrinkQueue.emit(Unit)
                }
            }
        }
    }

    private sealed class Msg {
        data class RecordRead(
            val at: Instant,
            val file: File,
        ) : Msg()

        data class Write(
            val key: String,
            val bytes: ByteArray,
        ) : Msg() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Write

                if (key != other.key) return false
                if (!bytes.contentEquals(other.bytes)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = key.hashCode()
                result = 31 * result + bytes.contentHashCode()
                return result
            }
        }
    }

    private fun handleMsg(msg: Msg) =
        when (msg) {
            is Msg.RecordRead -> handleRecordReadMsg(msg)
            is Msg.Write -> handleWriteMsg(msg)
        }

    private fun handleRecordReadMsg(msg: Msg.RecordRead) {
        val succeeded = msg.file.setLastModified(msg.at.toEpochMilliseconds())
        if (!succeeded) {
            Timber.w("Failed to record read %s", msg)
        }
    }

    private fun handleWriteMsg(msg: Msg.Write) {
        val file = fileFor(msg.key)
        try {
            file.writeBytes(msg.bytes)
        } catch (e: IOException) {
            Timber.w(e, "Failed to write ByteArray to file. Msg: %s", msg)
        }
    }

   /** Must not be executed in parallel. Can be executed while cache dir is modified
     *
     * Uses the last modified date set as of some time between when called and when exits. If last
     * modified is changed while executing the new data may or may not be used.
     */
    private fun shrinkIfNecessary() {
        Timber.i("shrinkIfNecessary called")

        val quota = getAppCacheQuotaBytes()
        val usage = getAppCacheUsageBytes()
        val overage = usage - quota

        if (overage < 0) {
            return
        }

        val files = cacheDir
            .walk()
            .map { file -> file.lastModified() to file }
            .sortedByDescending { (modified, _) -> modified } // earliest at the end
            .toMutableList()

        if (files.isEmpty()) {
            Timber.w("Cache %s over quota %s, but no files in cache dir", overage, quota)
            return
        }

        val totalFiles = files.size

        var remainingOverage = overage
        while (remainingOverage > 0) {
            if (files.isEmpty()) {
                Timber.w(
                    "Cache is still %s over quota %s (initial usage %s), but no files left to remove. Removed %s files.",
                    remainingOverage, quota, usage, files.size
                )
                return
            }

            val (_, file) = files.removeLast()
            remainingOverage -= file.length()
            file.delete()
        }

        Timber.i(
            "Done shrinking cache usage. Removed %s files to bring usage from %s to quota %s",
            totalFiles - files.size, usage, quota
        )
    }

    private fun getAppCacheQuotaBytes(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                storageManager.getCacheQuotaBytes(cacheStorageUuid())
            } catch (e: IOException) {
                Timber.w(e, "Using fallback quota size because of error")
                FALLBACK_QUOTA_SIZE_BYTES
            }
        } else {
            Timber.w("Using fallback quota size as version: %s", Build.VERSION.SDK_INT)
            FALLBACK_QUOTA_SIZE_BYTES
        }

    private fun getAppCacheUsageBytes(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                storageManager.getCacheSizeBytes(cacheStorageUuid())
            } catch (e: IOException) {
                Timber.w(e, "Using fallback app cache usage calculation because of error")
                fallbackAppCacheUsageCalculation()
            }
        } else {
            Timber.w("Using fallback app cache usage calculation as version: %s",
                Build.VERSION.SDK_INT)
            fallbackAppCacheUsageCalculation()
        }

    private fun fallbackAppCacheUsageCalculation(): Long {
        return cacheDir.walk()
            .map { file ->
                if (file.isFile) {
                    file.length()  // NOTE: .length() returns 0L on error
                } else {
                    0
                }
            }
            .sum()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun cacheStorageUuid() = storageManager.getUuidForPath(cacheDir)

    private fun fileFor(key: String) =
        File(cacheDir, "$key.$EXT")

    companion object {
        private val DEFAULT_MIN_CACHE_SHRINK_INTERVAL = 10.minutes

        private var INSTANCE: FileCache? = null

        /** Get instance of singleton.
         *
         * Parameters are only considered if the singleton isn't constructed yet.
         */
        fun get(
            context: Context,
            minCacheShrinkInterval: Duration = DEFAULT_MIN_CACHE_SHRINK_INTERVAL,
        ): FileCache {
            val cached = INSTANCE
            if (cached != null) {
                return cached
            }

            synchronized(this) {
                val storageManager =
                    context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val new = FileCache(minCacheShrinkInterval, storageManager, context.cacheDir)
                INSTANCE = new
                return new
            }
        }

        private const val EXT = "filecache"

        private const val MAX_WORKERS = 10

        // Used if the api for getting a cache size from the system isn't supported
        private const val FALLBACK_QUOTA_SIZE_BYTES: Long = 100_000_000L // 1e8 == 0.1 GB
    }
}
