package org.bakasu.bakasu.data.file

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Process
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import org.bakasu.bakasu.data.shell.KsuCliRepository

/** Raised when the single bounded staging copy of a picked module archive cannot be completed. */
class InstallArchiveException(message: String) : IOException(message)

/**
 * Stages a picked module archive into manager cache with ONE bounded copy, so every later step
 * of the native install flow (name extraction, script peek, info parse, flash) reads a local
 * file instead of re-opening the same SAF uri.
 *
 * On providers whose blocking read can stall forever, the copy is bounded by a timeout and, when
 * the uri is an external-storage document, rescued through a root shell that reads the
 * underlying `/data/media` file directly, bypassing the per-app FUSE view entirely.
 */
class StagedInstallArchive(
    private val application: Application,
    private val ksuCliRepository: KsuCliRepository,
) {
    private companion object {
        const val TAG = "StagedInstallArchive"
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        const val STAGE_DIR = "staged-archives"
        const val TEMP_PREFIX = "module-"
        const val TEMP_SUFFIX = ".zip"
        const val SWEEP_AGE_MS = 24L * 60 * 60 * 1000
        val SAF_COPY_TIMEOUT = 30.seconds
        val ROOT_COPY_TIMEOUT = 60.seconds
    }

    /** Returns [uri] as a local file uri, staging content uris once with a bounded copy. */
    suspend fun stage(uri: String): String = stage(uri.toUri())

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun stage(uri: Uri): String {
        // Already-local archives (registry downloads, previously staged files) pass through
        // untouched: the flash step re-verifies registry files by their store path.
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.toString()
        sweepStale()
        val target = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, stageDirectory())
        try {
            try {
                withTimeout(SAF_COPY_TIMEOUT) {
                    runInterruptible(Dispatchers.IO) { copyViaSaf(uri, target) }
                }
                requireZip(target)
                return Uri.fromFile(target).toString()
            } catch (safFailure: Throwable) {
                Log.e(TAG, "SAF staging failed for $uri: ${safFailure.message}")
                if (target.exists()) target.delete()

                val rootSource = rootSourceFor(uri)
                    ?: throw InstallArchiveException(
                        "archive copy failed after ${SAF_COPY_TIMEOUT.inWholeSeconds}s: ${safFailure.message}",
                    )
                val rescued = runCatching {
                    withTimeout(ROOT_COPY_TIMEOUT) {
                        runInterruptible(Dispatchers.IO) {
                            ksuCliRepository.copyFileAsRoot(rootSource, target.absolutePath)
                        }
                    }
                }.getOrDefault(false)
                if (!rescued) {
                    throw InstallArchiveException(
                        "archive copy failed: ${safFailure.message}; root rescue failed for $rootSource",
                    )
                }
                requireZip(target)
                return Uri.fromFile(target).toString()
            }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
    }

    private fun stageDirectory(): File = File(application.cacheDir, STAGE_DIR).apply { mkdirs() }

    private fun copyViaSaf(uri: Uri, target: File) {
        val input = application.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
    }

    /** Deletes staged files from earlier sessions so cache usage stays bounded. */
    private fun sweepStale() {
        runCatching {
            val now = System.currentTimeMillis()
            stageDirectory().listFiles()?.forEach { file ->
                if (now - file.lastModified() > SWEEP_AGE_MS) file.delete()
            }
        }
    }

    /** Rejects partial or truncated copies by checking the ZIP local-file-header magic. */
    private fun requireZip(target: File) {
        val magic = ByteArray(4)
        try {
            DataInputStream(target.inputStream()).use { input -> input.readFully(magic) }
        } catch (e: IOException) {
            throw InstallArchiveException("staged archive is unreadable: ${e.message}")
        }
        val expected = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        if (!magic.contentEquals(expected)) {
            throw InstallArchiveException("staged archive is not a ZIP file")
        }
    }

    /**
     * Real on-disk path for external-storage document uris, where a root read can bypass the
     * per-app FUSE view. Primary-volume files live under `/data/media/<user>` on the data
     * partition; secondary volumes are plain mounts rooted at `/storage/<volume>`.
     */
    private fun rootSourceFor(uri: Uri): String? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) return null
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }
            .getOrElse { runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() }
            ?: return null
        val separator = documentId.indexOf(':')
        if (separator <= 0) return null
        val volume = documentId.substring(0, separator)
        val relativePath = documentId.substring(separator + 1)
        if (relativePath.isBlank()) return null
        return if (volume == "primary") {
            "/data/media/${Process.myUid() / 100_000}/$relativePath"
        } else {
            "/storage/$volume/$relativePath"
        }
    }
}
