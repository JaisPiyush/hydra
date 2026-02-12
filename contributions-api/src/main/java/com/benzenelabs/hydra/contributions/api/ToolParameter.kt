package com.benzenelabs.hydra.contributions.api

/**
 * Describes a single parameter of a [ToolDeclaration].
 *
 * Parameter [name] must be a valid identifier (letter or underscore, then
 * alphanumerics/underscores). A [required] parameter must not have a [defaultValue].
 */
data class ToolParameter(
        val name: String,
        val type: ParameterType,
        val description: String,
        val required: Boolean = true,
        val defaultValue: String? = null
) {
    init {
        require(name.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            "Parameter name must be a valid identifier, got: $name"
        }
        require(description.isNotBlank()) { "Parameter description must not be blank" }
        if (required) {
            require(defaultValue == null) {
                "Required parameter '$name' must not have a defaultValue"
            }
        }
    }
}

/** The JSON-compatible type of a [ToolParameter]. */
enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY
}
