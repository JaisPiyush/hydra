package com.benzenelabs.hydra.contributions.api

/**
 * Persistent record of a successfully installed contribution stored by the host.
 *
 * Invariants:
 * - [installedAt] must be a positive epoch-millis timestamp.
 * - [updatedAt] must be >= [installedAt].
 * - [grantedPermissions] must be a subset of [manifest]`.permissions`.
 */
data class InstalledContribution(
        val manifest: ContributionManifest,
        val installedAt: Long,
        val updatedAt: Long,
        val isEnabled: Boolean,
        val grantedPermissions: Set<Permission>,
        val installPath: String
) {
    init {
        require(installedAt > 0) { "installedAt must be a positive epoch millis timestamp" }
        require(updatedAt >= installedAt) { "updatedAt must be >= installedAt" }
        require(installPath.isNotBlank()) { "installPath must not be blank" }
        require(grantedPermissions.all { it in manifest.permissions }) {
            "grantedPermissions must be a subset of manifest.permissions"
        }
    }

    /** The contribution's unique identifier, delegated to the manifest. */
    val id: ContributionId
        get() = manifest.id

    /** The contribution's version, delegated to the manifest. */
    val version: SemanticVersion
        get() = manifest.version

    /** The contribution's type, delegated to the manifest. */
    val type: ContributionType
        get() = manifest.type
}
