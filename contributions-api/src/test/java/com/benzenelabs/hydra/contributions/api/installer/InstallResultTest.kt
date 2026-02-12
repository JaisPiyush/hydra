package com.benzenelabs.hydra.contributions.api.installer

import com.benzenelabs.hydra.contributions.api.ContributionId
import com.benzenelabs.hydra.contributions.api.ContributionManifest
import com.benzenelabs.hydra.contributions.api.ContributionType
import com.benzenelabs.hydra.contributions.api.InstalledContribution
import com.benzenelabs.hydra.contributions.api.Permission
import com.benzenelabs.hydra.contributions.api.SemanticVersion
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstallResultTest {

    private val dummyManifest =
            ContributionManifest(
                    id = ContributionId("acme.tool"),
                    type = ContributionType.EXTENSION,
                    version = SemanticVersion(1, 0, 0),
                    displayName = "Acme Tool",
                    description = "A great tool",
                    author = "Acme Corp",
                    minHostVersion = SemanticVersion(1, 0, 0),
                    permissions = setOf(Permission.NETWORK),
                    entryPoint = "main.js",
                    toolDeclarations = emptyList(),
                    tags = emptyList()
            )

    private val dummyInstalled =
            InstalledContribution(
                    manifest = dummyManifest,
                    installedAt = 1000L,
                    updatedAt = 1000L,
                    isEnabled = true,
                    grantedPermissions = setOf(Permission.NETWORK),
                    installPath = "/data/extensions/acme.tool"
            )

    @Test
    fun `Success holds installed contribution`() {
        val success = InstallResult.Success(dummyInstalled)
        assertEquals(dummyInstalled, success.installed)
    }

    @Test
    fun `ManifestInvalid holds reason`() {
        val failure = InstallResult.Failure.ManifestInvalid("Bad json")
        assertEquals("Bad json", failure.reason)
    }

    @Test
    fun `AlreadyInstalled holds existing contribution`() {
        val failure = InstallResult.Failure.AlreadyInstalled(dummyInstalled)
        assertEquals(dummyInstalled, failure.existing)
    }

    @Test
    fun `VersionDowngrade holds versions`() {
        val current = SemanticVersion(2, 0, 0)
        val attempted = SemanticVersion(1, 0, 0)
        val failure = InstallResult.Failure.VersionDowngrade(current, attempted)
        assertEquals(current, failure.currentVersion)
        assertEquals(attempted, failure.attemptedVersion)
    }

    @Test
    fun `PermissionDenied holds denied permissions`() {
        val denied = setOf(Permission.NETWORK, Permission.STORAGE_WRITE)
        val failure = InstallResult.Failure.PermissionDenied(denied)
        assertEquals(denied, failure.deniedPermissions)
    }

    @Test
    fun `StorageError holds cause`() {
        val ioException = IOException("Disk full")
        val failure = InstallResult.Failure.StorageError(ioException)
        assertEquals(ioException, failure.cause)
    }

    @Test
    fun `HostVersionIncompatible holds versions`() {
        val required = SemanticVersion(2, 0, 0)
        val host = SemanticVersion(1, 5, 0)
        val failure = InstallResult.Failure.HostVersionIncompatible(required, host)
        assertEquals(required, failure.requiredMin)
        assertEquals(host, failure.hostVersion)
    }

    @Test
    fun `Unknown holds cause`() {
        val runtimeException = RuntimeException("Something bad")
        val failure = InstallResult.Failure.Unknown(runtimeException)
        assertEquals(runtimeException, failure.cause)
    }
}
