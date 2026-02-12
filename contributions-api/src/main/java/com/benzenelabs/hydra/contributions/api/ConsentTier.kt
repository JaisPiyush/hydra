package com.benzenelabs.hydra.contributions.api

/**
 * Risk level of a permission or operation.
 *
 * [HIGH] and [CRITICAL] tiers require biometric authentication before granting consent. The [level]
 * values are strictly increasing from [NONE] (0) to [CRITICAL] (4).
 */
enum class ConsentTier(val level: Int, val requiresBiometric: Boolean) {
    NONE(0, false),
    LOW(1, false),
    MEDIUM(2, false),
    HIGH(3, true),
    CRITICAL(4, true);

    /** Returns `true` if this tier is at least as high as [other]. */
    fun isAtLeast(other: ConsentTier): Boolean = this.level >= other.level
}
