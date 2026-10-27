package org.bakasu.bakasu.domain.model

data class SuSFSStatus(
    val enabled: Boolean,
    val version: String,
    val enabledFeatures: String,
    val supercallSupported: Boolean = true,
    val guardModule: String? = null,
    val guardLoader: String? = null,
)
