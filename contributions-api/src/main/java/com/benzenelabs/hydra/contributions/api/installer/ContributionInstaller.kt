package com.benzenelabs.hydra.contributions.api.installer

import com.benzenelabs.hydra.contributions.api.ContributionId
import com.benzenelabs.hydra.contributions.api.ContributionPackage

/**
 * Contract for installing, upgrading, and uninstalling contribution packages.
 *
 * Implemented by the `contributions-installer` module.
 */
interface ContributionInstaller {

    /**
     * Validates, consents, and installs a package.
     *
     * @return [InstallResult] indicating success or the specific failure reason.
     */
    suspend fun install(pkg: ContributionPackage): InstallResult

    /**
     * Upgrades an existing installation to the new package version.
     *
     * Fails with [InstallResult.Failure.VersionDowngrade] if the new version is lower.
     */
    suspend fun upgrade(pkg: ContributionPackage): InstallResult

    /** Uninstalls a contribution and cleans up on-device files. */
    suspend fun uninstall(id: ContributionId): UninstallResult
}
