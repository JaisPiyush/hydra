package com.benzenelabs.hydra.host.channel

import org.junit.Test

class ChannelMetadataTest {

    @Test(expected = IllegalArgumentException::class)
    fun `blank displayName throws`() {
        createMetadata(displayName = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank description throws`() {
        createMetadata(description = "  ")
    }

    @Test
    fun `valid construction`() {
        createMetadata(iconBlobId = null, capabilities = emptySet())
    }

    private fun createMetadata(
            channelId: String = "com.example",
            displayName: String = "Example",
            description: String = "Description",
            iconBlobId: String? = "blob1",
            capabilities: Set<ChannelCapability> = setOf(ChannelCapability.TEXT)
    ) =
            ChannelMetadata(
                    channelId = ChannelId(channelId),
                    displayName = displayName,
                    description = description,
                    iconBlobId = iconBlobId,
                    supportsMultipleSessions = true,
                    authType = ChannelAuthType.TOKEN,
                    capabilities = capabilities
            )
}
