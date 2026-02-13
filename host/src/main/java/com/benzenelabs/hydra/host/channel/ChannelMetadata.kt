package com.benzenelabs.hydra.host.channel

/**
 * Static metadata describing a channel, declared by the channel extension at
 * registration time.
 */
data class ChannelMetadata(
    val channelId: ChannelId,
    val displayName: String,
    val description: String,
    val iconBlobId: String?,
    val supportsMultipleSessions: Boolean,
    val authType: ChannelAuthType,
    val capabilities: Set<ChannelCapability>
) {
    init {
        require(displayName.isNotBlank()) { "ChannelMetadata.displayName must not be blank" }
        require(description.isNotBlank()) { "ChannelMetadata.description must not be blank" }
    }
}

/**
 * How a channel authenticates with its external platform.
 */
enum class ChannelAuthType {
    /** No authentication required (e.g., in-process custom channel). */
    NONE,

    /** Static credential (bot token, API key) entered once by the user. */
    TOKEN,

    /** QR code displayed to the user who scans it on their phone (WhatsApp-style). */
    QR_CODE,

    /** Standard OAuth 2.0 flow via a browser/WebView. */
    OAUTH2,

    /** Phone number + OTP (Telegram user account). */
    PHONE_OTP
}

/**
 * Capabilities a channel extension may support.
 */
enum class ChannelCapability {
    TEXT,
    IMAGES,
    AUDIO,
    VIDEO,
    FILES,
    REACTIONS,
    REPLY_THREAD,
    PRESENCE,
    READ_RECEIPTS,
    HISTORY_SYNC
}
