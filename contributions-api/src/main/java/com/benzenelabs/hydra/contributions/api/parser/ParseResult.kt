package com.benzenelabs.hydra.contributions.api.parser

import com.benzenelabs.hydra.contributions.api.ContributionPackage

/** Outcome of a [ContributionPackageParser.parse] operation. */
sealed class ParseResult {

    /** The zip archive was parsed and validated successfully. */
    data class Success(val pkg: ContributionPackage) : ParseResult()

    /** The zip archive could not be parsed or the manifest was invalid. */
    data class Failure(val reason: String, val cause: Throwable? = null) : ParseResult()
}
