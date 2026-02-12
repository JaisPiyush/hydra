package com.benzenelabs.hydra.contributions.api

/**
 * Parsed, validated representation of a `hydra-manifest.json` file.
 *
 * Enforces structural invariants:
 * - Extensions must declare an [entryPoint]; Skills must not.
 * - Skills must not declare [permissions] (permissions are an Extension concern).
 * - [displayName], [description], and [author] must not be blank.
 */
data class ContributionManifest(
        val id: ContributionId,
        val type: ContributionType,
        val version: SemanticVersion,
        val displayName: String,
        val description: String,
        val author: String,
        val minHostVersion: SemanticVersion,
        val permissions: Set<Permission>,
        val entryPoint: String?,
        val toolDeclarations: List<ToolDeclaration>,
        val tags: List<String>
) {
    init {
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(description.isNotBlank()) { "description must not be blank" }
        require(author.isNotBlank()) { "author must not be blank" }
        if (type == ContributionType.EXTENSION) {
            require(!entryPoint.isNullOrBlank()) { "Extensions must declare an entryPoint" }
        }
        if (type == ContributionType.SKILL) {
            require(entryPoint == null) { "Skills must not declare an entryPoint" }
            require(permissions.isEmpty()) {
                "Skills must not declare permissions (permissions are an Extension concern)"
            }
        }
    }

    /** The highest [ConsentTier] required by any declared permission, or [ConsentTier.NONE]. */
    val maxConsentTier: ConsentTier
        get() = permissions.maxOfOrNull { it.consentTier } ?: ConsentTier.NONE
}
