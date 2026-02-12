package com.benzenelabs.hydra.host.data

import com.benzenelabs.hydra.contributions.api.ContributionId

/**
 * Runtime identity of the entity accessing a storage system.
 *
 * All storage operations are scoped to either the [Agent] or a specific [Extension].
 * Implementations must enforce that an [Extension] cannot access resources owned by
 * a different [Extension] or by [Agent]-private resources.
 */
sealed class StorageScope {

    /**
     * The AI agent runtime. Has access to public DB tables and can write secrets
     * (but not read their values). Cannot access extension-private resources.
     */
    data object Agent : StorageScope()

    /**
     * A specific installed extension identified by its [contributionId].
     * Access is restricted to resources owned by this extension.
     */
    data class Extension(val contributionId: ContributionId) : StorageScope()

    /** Human-readable label for logging and error messages. */
    val label: String
        get() = when (this) {
            is Agent -> "agent"
            is Extension -> "ext:${contributionId.value}"
        }
}
