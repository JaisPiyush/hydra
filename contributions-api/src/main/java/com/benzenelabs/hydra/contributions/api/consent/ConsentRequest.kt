package com.benzenelabs.hydra.contributions.api.consent

import com.benzenelabs.hydra.contributions.api.ConsentTier
import com.benzenelabs.hydra.contributions.api.ContributionId
import com.benzenelabs.hydra.contributions.api.Permission

/**
 * A request to obtain user consent for a set of permissions.
 *
 * The [tier] must equal the maximum [ConsentTier] among [requestedPermissions].
 */
data class ConsentRequest(
        val contributionId: ContributionId,
        val requestedPermissions: Set<Permission>,
        val tier: ConsentTier,
        val rationale: String
) {
    init {
        require(rationale.isNotBlank()) { "Consent rationale must not be blank" }
        val expectedTier = requestedPermissions.maxOfOrNull { it.consentTier } ?: ConsentTier.NONE
        require(tier == expectedTier) {
            "ConsentRequest.tier ($tier) must equal the max tier of requestedPermissions ($expectedTier)"
        }
    }
}
