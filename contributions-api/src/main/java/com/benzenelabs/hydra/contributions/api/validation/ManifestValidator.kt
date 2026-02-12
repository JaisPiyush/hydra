package com.benzenelabs.hydra.contributions.api.validation

/**
 * Contract for validating raw manifest data (e.g., parsed from JSON) into a typed
 * [com.benzenelabs.hydra.contributions.api.ContributionManifest].
 *
 * Implemented by the `contributions-installer` or `host` module.
 */
interface ManifestValidator {

    /**
     * Validates a raw manifest map (e.g., parsed from JSON) and returns a [ValidationResult].
     *
     * @param raw key-value map representing the deserialized JSON.
     */
    fun validate(raw: Map<String, Any?>): ValidationResult
}
