package com.benzenelabs.hydra.host.data

import com.benzenelabs.hydra.contributions.api.ContributionId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StorageScopeTest {

    @Test
    fun `scope labels are correct`() {
        assertEquals("agent", StorageScope.Agent.label)
        assertEquals("ext:acme.tool", StorageScope.Extension(ContributionId("acme.tool")).label)
    }

    @Test
    fun `agent scope is singleton`() {
        assertSame(StorageScope.Agent, StorageScope.Agent)
    }

    @Test
    fun `extension scopes equal strictly by id`() {
        val ext1 = StorageScope.Extension(ContributionId("acme.tool"))
        val ext2 = StorageScope.Extension(ContributionId("acme.tool"))
        val ext3 = StorageScope.Extension(ContributionId("other.tool"))

        assertEquals(ext1, ext2)
        assertNotEquals(ext1, ext3)
    }

    @Test
    fun `exhaustive when check`() {
        val scope: StorageScope = StorageScope.Agent
        // This fails to compile if not exhaustive, serving as the check
        @Suppress("UNUSED_VARIABLE")
        val result =
                when (scope) {
                    is StorageScope.Agent -> true
                    is StorageScope.Extension -> true
                }
    }
}
