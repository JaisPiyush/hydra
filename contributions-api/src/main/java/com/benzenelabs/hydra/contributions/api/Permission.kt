package com.benzenelabs.hydra.contributions.api

/**
 * A declared capability that an Extension may request.
 *
 * Each permission has a stable [id] string (used in manifests), a [consentTier] indicating its risk
 * level, and a human-readable [description].
 */
enum class Permission(val id: String, val consentTier: ConsentTier, val description: String) {
    NETWORK(
            id = "network",
            consentTier = ConsentTier.LOW,
            description = "Make outbound HTTP/WebSocket requests"
    ),
    STORAGE_READ(
            id = "storage.read",
            consentTier = ConsentTier.LOW,
            description = "Read from sandboxed Extension storage"
    ),
    STORAGE_WRITE(
            id = "storage.write",
            consentTier = ConsentTier.MEDIUM,
            description = "Write to sandboxed Extension storage"
    ),
    OAUTH(
            id = "oauth",
            consentTier = ConsentTier.MEDIUM,
            description = "Initiate OAuth flows to third-party services"
    ),
    ANDROID_INTENT(
            id = "android.intent",
            consentTier = ConsentTier.HIGH,
            description = "Send Android Intents to other apps"
    ),
    ANDROID_ACCESSIBILITY(
            id = "android.accessibility",
            consentTier = ConsentTier.CRITICAL,
            description = "Use Android Accessibility Service APIs"
    ),
    PERIPHERAL_BLUETOOTH(
            id = "peripheral.bluetooth",
            consentTier = ConsentTier.MEDIUM,
            description = "Connect to Bluetooth LE wearables"
    ),
    PERIPHERAL_NETWORK(
            id = "peripheral.network",
            consentTier = ConsentTier.HIGH,
            description = "Connect to workstation terminals via WebSocket/mDNS"
    );

    companion object {
        private val byId = entries.associateBy { it.id }

        /**
         * Resolves a [Permission] by its [id] string.
         *
         * @throws IllegalStateException if no permission matches the given id.
         */
        fun fromId(id: String): Permission = byId[id] ?: error("Unknown permission id: $id")

        /**
         * Resolves a set of [Permission]s from a list of id strings.
         *
         * @throws IllegalStateException if any id is unknown.
         */
        fun fromIds(ids: List<String>): Set<Permission> = ids.map { fromId(it) }.toSet()
    }
}
