package org.bakasu.bakasu.domain.usecase

import org.bakasu.bakasu.data.file.ModuleFileRepository
import org.bakasu.bakasu.data.file.StagedInstallArchive

class IsModuleUriAccessibleUseCase(private val repository: ModuleFileRepository) {
    operator fun invoke(uri: String) = repository.isUriAccessible(uri)
}

class TakeModuleUriPermissionUseCase(private val repository: ModuleFileRepository) {
    operator fun invoke(uri: String) = repository.takePersistableUriPermission(uri)
}

class ExtractModuleNameUseCase(private val repository: ModuleFileRepository) {
    operator fun invoke(uri: String) = repository.extractModuleName(uri)
}

class ExtractModuleIdUseCase(private val repository: ModuleFileRepository) {
    operator fun invoke(uri: String) = repository.extractModuleId(uri)
}

/**
 * Stages the picked archive once into manager cache with a bounded copy, returning a local file
 * uri that the rest of the native install flow reads instead of the SAF uri. Throws
 * [org.bakasu.bakasu.data.file.InstallArchiveException] when the copy cannot be completed, so
 * callers can surface the failure instead of hanging silently.
 */
class StageInstallArchiveUseCase(private val repository: StagedInstallArchive) {
    suspend operator fun invoke(uri: String): String = repository.stage(uri)
}
