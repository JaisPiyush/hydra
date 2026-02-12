package com.benzenelabs.hydra.contributions.api

/**
 * A namespaced identifier for a contribution, in the form `<author>.<name>`.
 *
 * Both segments must be lowercase alphanumeric, hyphens, or underscores. Examples:
 * `"acme.web-search"`, `"org_x.tool_1"`.
 */
@JvmInline
value class ContributionId(val value: String) {
    init {
        require(value.matches(Regex("^[a-z0-9_-]+\\.[a-z0-9_-]+$"))) {
            "ContributionId must be in format '<author>.<name>', got: $value"
        }
    }

    /** The author segment before the dot. */
    val author: String
        get() = value.substringBefore('.')

    /** The name segment after the dot. */
    val name: String
        get() = value.substringAfter('.')

    override fun toString(): String = value
}
