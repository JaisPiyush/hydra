package com.benzenelabs.hydra.host.channel

/**
 * A request from a channel extension for user authentication.
 */
sealed class AuthRequest {

    /** The extension is providing a QR code data URI for the user to scan. */
    data class QrCode(
        val dataUri: String,
        val expiresAt: Long?
    ) : AuthRequest() {
        init {
            require(dataUri.isNotBlank()) { "QrCode.dataUri must not be blank" }
        }
    }

    /** The extension is requesting a static token/API key from the user. */
    data class TokenEntry(
        val prompt: String,
        val isPassword: Boolean = true
    ) : AuthRequest() {
        init {
            require(prompt.isNotBlank()) { "TokenEntry.prompt must not be blank" }
        }
    }

    /**
     * The extension is providing an OAuth2 authorization URL.
     */
    data class OAuth2(
        val authorizationUrl: String,
        val redirectScheme: String
    ) : AuthRequest() {
        init {
            require(authorizationUrl.isNotBlank()) { "OAuth2.authorizationUrl must not be blank" }
            require(redirectScheme.isNotBlank()) { "OAuth2.redirectScheme must not be blank" }
        }
    }

    /** The extension is requesting a phone number and will handle OTP verification. */
    data class PhoneOtp(
        val prompt: String = "Enter your phone number",
        val countryCodeHint: String? = null
    ) : AuthRequest()
}
