package com.benzenelabs.hydra.host.channel

import com.benzenelabs.hydra.contributions.api.ContributionId

/**
 * Alias for the [ContributionId] of the extension that implements this channel.
 *
 * A channel is identified by its backing extension. There is a 1:1 relationship
 * between a channel and its extension.
 */
typealias ChannelId = ContributionId
