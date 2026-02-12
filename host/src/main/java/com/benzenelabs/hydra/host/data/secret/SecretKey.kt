package com.benzenelabs.hydra.host.data.secret

/**
 * A validated, opaque identifier for a secret stored in [SecretVault].
 */
@JvmInline
value class SecretKey(val value: String) {
    init {
        require(value.isNotBlank()) { "SecretKey must not be blank" }
        require(value.length <= MAX_LENGTH) { "SecretKey must be <= $MAX_LENGTH characters" }
        require(value.matches(KEY_PATTERN)) {
            "SecretKey may only contain alphanumeric characters, underscores, and hyphens"
        }
    }

    override fun toString(): String = value

    private companion object {
        private const val MAX_LENGTH = 128
        private val KEY_PATTERN = Regex("^[a-zA-Z0-9_-]+$")
    }
}
