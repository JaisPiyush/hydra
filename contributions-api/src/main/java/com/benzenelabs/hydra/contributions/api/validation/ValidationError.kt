package com.benzenelabs.hydra.contributions.api.validation

/** A single validation error describing which [field] failed and why. */
data class ValidationError(val field: String, val message: String)
