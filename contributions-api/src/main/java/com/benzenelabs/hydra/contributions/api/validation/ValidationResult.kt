package com.benzenelabs.hydra.contributions.api.validation

import com.benzenelabs.hydra.contributions.api.ContributionManifest

/** Outcome of a [ManifestValidator.validate] operation. */
sealed class ValidationResult {

    /** The raw manifest map was valid and produced a [ContributionManifest]. */
    data class Valid(val manifest: ContributionManifest) : ValidationResult()

    /** The raw manifest map contained one or more errors. */
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
}
