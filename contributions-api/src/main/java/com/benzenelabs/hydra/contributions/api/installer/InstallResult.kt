package com.benzenelabs.hydra.contributions.api.installer

import com.benzenelabs.hydra.contributions.api.InstalledContribution
import com.benzenelabs.hydra.contributions.api.Permission
import com.benzenelabs.hydra.contributions.api.SemanticVersion

/** Outcome of a [ContributionInstaller.install] or [ContributionInstaller.upgrade] operation. */
sealed class InstallResult {

    /** The contribution was installed successfully. */
    data class Success(val installed: InstalledContribution) : InstallResult()

    /** The installation failed for one of the following reasons. */
    sealed class Failure : InstallResult() {

        /** The manifest in the package is structurally invalid. */
        data class ManifestInvalid(val reason: String) : Failure()

        /** A contribution with the same id is already installed. */
        data class AlreadyInstalled(val existing: InstalledContribution) : Failure()

        /** The new version is lower than the currently installed version. */
        data class VersionDowngrade(
                val currentVersion: SemanticVersion,
                val attemptedVersion: SemanticVersion
        ) : Failure()

        /** The user denied one or more required permissions. */
        data class PermissionDenied(val deniedPermissions: Set<Permission>) : Failure()

        /** An I/O or filesystem error occurred during installation. */
        data class StorageError(val cause: Throwable) : Failure()

        /** The package requires a higher host version than currently running. */
        data class HostVersionIncompatible(
                val requiredMin: SemanticVersion,
                val hostVersion: SemanticVersion
        ) : Failure()

        /** An unexpected error occurred. */
        data class Unknown(val cause: Throwable) : Failure()
    }
}
