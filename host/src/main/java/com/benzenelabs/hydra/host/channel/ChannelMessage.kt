package com.benzenelabs.hydra.host.channel

import com.benzenelabs.hydra.host.data.session.SessionId
import java.util.UUID

/**
 * A single message exchanged on a channel, in either direction.
 */
data class ChannelMessage(
    val id: String,
    val sessionId: SessionId,
    val channelId: ChannelId,
    val direction: MessageDirection,
    val content: MessageContent,
    val replyToId: String?,
    val timestamp: Long,
    val metadata: String?
) {
    init {
        require(id.isNotBlank()) { "ChannelMessage.id must not be blank" }
        require(timestamp > 0) { "ChannelMessage.timestamp must be positive" }
    }

    companion object {
        fun generateId(): String = UUID.randomUUID().toString()
    }
}

enum class MessageDirection { INBOUND, OUTBOUND }

/**
 * Typed message content variants.
 */
sealed class MessageContent {

    /** Plain or formatted text (markdown/HTML subset). */
    data class Text(val body: String) : MessageContent() {
        init {
            require(body.isNotEmpty()) { "Text body must not be empty" }
        }
    }

    /** An image. */
    data class Image(
        val blobUriString: String,
        val caption: String? = null,
        val mimeType: String = "image/jpeg"
    ) : MessageContent()

    /** An audio clip or voice note. */
    data class Audio(
        val blobUriString: String,
        val durationMs: Long? = null,
        val mimeType: String = "audio/ogg"
    ) : MessageContent()

    /** A video clip. */
    data class Video(
        val blobUriString: String,
        val caption: String? = null,
        val durationMs: Long? = null,
        val mimeType: String = "video/mp4"
    ) : MessageContent()

    /** A generic file attachment. */
    data class File(
        val blobUriString: String,
        val fileName: String,
        val mimeType: String
    ) : MessageContent() {
        init {
            require(fileName.isNotBlank()) { "File.fileName must not be blank" }
        }
    }

    /** A reaction to a previous message (e.g., emoji). */
    data class Reaction(
        val emoji: String,
        val targetMessageId: String
    ) : MessageContent() {
        init {
            require(emoji.isNotBlank()) { "Reaction.emoji must not be blank" }
            require(targetMessageId.isNotBlank()) { "Reaction.targetMessageId must not be blank" }
        }
    }

    /** Indicates a previous message was deleted. */
    data class Deletion(val targetMessageId: String) : MessageContent() {
        init {
            require(targetMessageId.isNotBlank()) { "Deletion.targetMessageId must not be blank" }
        }
    }

    /**
     * Structured event from the platform that is not a user-visible message.
     */
    data class Event(val type: String, val payload: String) : MessageContent() {
        init {
            require(type.isNotBlank()) { "Event.type must not be blank" }
        }
    }
}
