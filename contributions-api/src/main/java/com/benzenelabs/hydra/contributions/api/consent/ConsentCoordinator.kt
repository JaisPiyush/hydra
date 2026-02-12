package com.benzenelabs.hydra.contributions.api.consent

import com.benzenelabs.hydra.contributions.api.consent.ConsentRequest
import com.benzenelabs.hydra.contributions.api.consent.ConsentResult

/**
 * Contract for presenting consent UI and collecting user decisions.
 *
 * Presents the appropriate consent UI (inline banner for LOW, UPI-style overlay for HIGH/CRITICAL)
 * and suspends until the user responds.
 *
 * Implemented by the `host` or `ui` module.
 */
interface ConsentCoordinator {

    /**
     * Presents the consent UI for the given [request] and suspends until the user responds.
     *
     * @return [ConsentResult] representing the user's decision.
     */
    suspend fun requestConsent(request: ConsentRequest): ConsentResult
}
