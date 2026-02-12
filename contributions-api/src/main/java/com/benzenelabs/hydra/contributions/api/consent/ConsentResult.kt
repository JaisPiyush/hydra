package com.benzenelabs.hydra.contributions.api.consent

import com.benzenelabs.hydra.contributions.api.Permission

/** Outcome of a [ConsentCoordinator.requestConsent] operation. */
sealed class ConsentResult {

    /** User granted all requested permissions. */
    data class Granted(val grantedPermissions: Set<Permission>) : ConsentResult()

    /** User denied one or more permissions. */
    data class Denied(
            val grantedPermissions: Set<Permission>,
            val deniedPermissions: Set<Permission>
    ) : ConsentResult()

    /** User dismissed the consent UI without making a decision. */
    data object Dismissed : ConsentResult()

    /** Biometric authentication failed for HIGH/CRITICAL tier requests. */
    data class BiometricFailed(val reason: String) : ConsentResult()
}
