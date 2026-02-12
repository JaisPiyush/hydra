package com.benzenelabs.hydra.contributions.api

/**
 * A semantic version with [major], [minor], and [patch] components.
 *
 * Implements [Comparable] so versions can be compared naturally: `1.2.3 < 1.2.4 < 1.3.0 < 2.0.0`.
 */
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) :
        Comparable<SemanticVersion> {

    init {
        require(major >= 0 && minor >= 0 && patch >= 0) {
            "Version components must be non-negative"
        }
    }

    override fun compareTo(other: SemanticVersion): Int =
            compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Parses a version string of the form `"major.minor.patch"`.
         *
         * @throws IllegalArgumentException if the string is not a valid three-part version.
         * @throws IllegalStateException if any component is not a valid integer.
         */
        fun parse(version: String): SemanticVersion {
            val parts = version.trim().split(".")
            require(parts.size == 3) { "Invalid semantic version: $version" }
            return SemanticVersion(
                    major = parts[0].toIntOrNull() ?: error("Invalid major: ${parts[0]}"),
                    minor = parts[1].toIntOrNull() ?: error("Invalid minor: ${parts[1]}"),
                    patch = parts[2].toIntOrNull() ?: error("Invalid patch: ${parts[2]}")
            )
        }
    }
}
