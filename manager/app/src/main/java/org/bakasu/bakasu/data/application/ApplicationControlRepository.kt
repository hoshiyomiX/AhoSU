package org.bakasu.bakasu.data.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.Natives
import org.bakasu.bakasu.data.shell.KsuCliRepository

class ApplicationControlRepository(
    private val ksuCliRepository: KsuCliRepository,
) {
    suspend fun ensureManagerInstalled(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Natives.isFullFeatured() && ksuCliRepository.rootAvailable()) {
                ksuCliRepository.install()
            }
        }
    }

    suspend fun reboot(reason: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // B3: a false result from the CLI layer is a real failure (e.g.
            // ksud soft-reboot exiting non-zero on a UAPI mismatch) — turn it
            // into Result.failure so the UI stops reporting silent success.
            if (!ksuCliRepository.reboot(reason)) {
                error(
                    "Soft reboot failed: ksud exited non-zero " +
                        "(kernel/manager version mismatch?)",
                )
            }
        }
    }
}
