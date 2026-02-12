package com.benzenelabs.hydra.contributions.api

/**
 * Describes a `__hydra.*` tool that an Extension registers for Skills to invoke.
 *
 * The [namespace] and [name] are combined into a [qualifiedName] of the form
 * `__hydra.<namespace>.<name>`.
 */
data class ToolDeclaration(
        val namespace: String,
        val name: String,
        val description: String,
        val parameters: List<ToolParameter>,
        val consentTier: ConsentTier
) {
    init {
        require(namespace.matches(Regex("^[a-z0-9_]+$"))) {
            "namespace must be lowercase alphanumeric/underscore, got: $namespace"
        }
        require(name.matches(Regex("^[a-z0-9_]+$"))) {
            "name must be lowercase alphanumeric/underscore, got: $name"
        }
        require(description.isNotBlank()) { "Tool description must not be blank" }
    }

    /** Fully qualified tool name as exposed to the AI: `__hydra.<namespace>.<name>`. */
    val qualifiedName: String
        get() = "__hydra.$namespace.$name"
}
