package com.benzenelabs.hydra.contributions.api.installer

/** Outcome of a [ContributionInstaller.uninstall] operation. */
sealed class UninstallResult {

    /** The contribution was uninstalled and cleaned up successfully. */
    data object Success : UninstallResult()

    /** The uninstallation failed. */
    data class Failure(val reason: String) : UninstallResult()
}
