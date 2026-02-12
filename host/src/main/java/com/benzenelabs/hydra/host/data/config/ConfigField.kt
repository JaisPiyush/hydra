package com.benzenelabs.hydra.host.data.config

import com.benzenelabs.hydra.host.data.secret.SecretKey

/**
 * Descriptor for a single configuration field.
 */
data class ConfigField(
    val name: String,
    val defaultValue: String? = null,
    val isSecret: Boolean = false,
    val description: String = ""
) {
    init {
        require(name.isNotBlank()) { "ConfigField name must not be blank" }
        if (isSecret) {
            require(defaultValue == null) {
                "Secret ConfigField '$name' must not have a defaultValue"
            }
        }
    }

    /** The [SecretKey] used when this field is stored in the vault. */
    val secretKey: SecretKey
        get() = SecretKey(name)
}
