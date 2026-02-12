package com.benzenelabs.hydra.contributions.api

/**
 * An in-memory unpacked contribution ready for installation.
 *
 * Contains the validated [manifest] and a map of [files] keyed by relative path.
 */
data class ContributionPackage(
        val manifest: ContributionManifest,
        val files: Map<String, ByteArray>
) {
    /** Returns the content of a file by relative path, or null if not present. */
    fun fileContent(relativePath: String): ByteArray? = files[relativePath]

    /** Returns the markdown instruction content for Skills, or null for Extensions. */
    fun skillInstructions(): String? {
        if (manifest.type != ContributionType.SKILL) return null
        val bytes = files["instructions.md"] ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    /** Returns the entry point JS source for Extensions, or null for Skills. */
    fun extensionEntrySource(): String? {
        if (manifest.type != ContributionType.EXTENSION) return null
        val path = manifest.entryPoint ?: return null
        val bytes = files[path] ?: return null
        return bytes.toString(Charsets.UTF_8)
    }
}
