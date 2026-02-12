package com.benzenelabs.hydra.contributions.api.parser

/**
 * Contract for parsing a `.zip` byte stream into an in-memory
 * [com.benzenelabs.hydra.contributions.api.ContributionPackage].
 *
 * Validates the manifest before returning. Implemented by the `contributions-installer` module.
 */
interface ContributionPackageParser {

    /**
     * Parses a `.zip` byte stream into an in-memory
     * [com.benzenelabs.hydra.contributions.api.ContributionPackage]. Validates the manifest before
     * returning.
     *
     * @param source raw bytes of the zip archive.
     * @return [ParseResult] indicating success or the specific failure reason.
     */
    suspend fun parse(source: ByteArray): ParseResult
}
