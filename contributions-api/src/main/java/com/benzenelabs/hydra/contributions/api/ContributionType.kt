package com.benzenelabs.hydra.contributions.api

/**
 * Discriminator for the two kinds of installable contributions.
 *
 * - [SKILL] — a markdown-based instruction set that teaches the AI what to do.
 * - [EXTENSION] — a pure-JavaScript module that registers `__hydra.*` tools.
 */
enum class ContributionType {
    SKILL,
    EXTENSION
}
