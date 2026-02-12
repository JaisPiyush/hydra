package com.benzenelabs.hydra.contributions.api

/** Shared constants used across Hydra modules. */
object HydraConstants {

    /** File name of the manifest inside a contribution zip. */
    const val MANIFEST_FILE_NAME = "manifest.json"

    /** File name of the skill instruction document inside a Skill zip. */
    const val SKILL_INSTRUCTIONS_FILE_NAME = "SKILL.md"

    /** Prefix for all platform primitive APIs available to Extensions. */
    const val PLATFORM_API_PREFIX = "__platform."

    /** Prefix for all high-level tools registered by Extensions. */
    const val HYDRA_TOOL_PREFIX = "__hydra."

    /** Current host (gateway) version. Bump on breaking API changes. */
    val HOST_VERSION: SemanticVersion = SemanticVersion(1, 0, 0)

    /** Maximum allowed size (bytes) for a contribution zip archive: 50 MB. */
    const val MAX_PACKAGE_SIZE_BYTES: Long = 50L * 1024 * 1024

    /** Maximum allowed size (bytes) for the manifest JSON file: 64 KB. */
    const val MAX_MANIFEST_SIZE_BYTES: Int = 64 * 1024
}
