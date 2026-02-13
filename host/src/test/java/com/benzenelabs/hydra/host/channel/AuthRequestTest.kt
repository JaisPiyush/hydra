package com.benzenelabs.hydra.host.channel

import org.junit.Test

class AuthRequestTest {

    @Test
    fun `QrCode validation`() {
        AuthRequest.QrCode("data:image/png...", 1000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QrCode blank dataUri throws`() {
        AuthRequest.QrCode("  ", 1000)
    }

    @Test
    fun `TokenEntry validation`() {
        AuthRequest.TokenEntry("Prompt")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TokenEntry blank prompt throws`() {
        AuthRequest.TokenEntry("  ")
    }

    @Test
    fun `OAuth2 validation`() {
        AuthRequest.OAuth2("https://auth", "app://callback")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `OAuth2 blank url throws`() {
        AuthRequest.OAuth2("  ", "scheme")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `OAuth2 blank scheme throws`() {
        AuthRequest.OAuth2("url", "  ")
    }

    @Test
    fun `PhoneOtp validation`() {
        AuthRequest.PhoneOtp("Prompt", "US")
    }
}
