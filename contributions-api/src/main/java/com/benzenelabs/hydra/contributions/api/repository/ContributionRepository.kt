package com.benzenelabs.hydra.contributions.api.repository

import com.benzenelabs.hydra.contributions.api.ContributionId
import com.benzenelabs.hydra.contributions.api.InstalledContribution
import com.benzenelabs.hydra.contributions.api.Permission
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for persisted [InstalledContribution] records.
 *
 * Implemented by the `host` module. Consumed by `guest`, `contributions-installer`, and `app`.
 */
interface ContributionRepository {

    /**
     * Emits the full current list and re-emits on any change (install / uninstall / enable /
     * disable).
     */
    fun observeAll(): Flow<List<InstalledContribution>>

    /** Returns all currently installed contributions. */
    suspend fun getAll(): List<InstalledContribution>

    /** Returns a specific installed contribution, or null if not found. */
    suspend fun findById(id: ContributionId): InstalledContribution?

    /** Persists a newly installed contribution record. */
    suspend fun save(contribution: InstalledContribution)

    /** Removes the record for an uninstalled contribution. */
    suspend fun delete(id: ContributionId)

    /** Updates the enabled state. */
    suspend fun setEnabled(id: ContributionId, enabled: Boolean)

    /** Updates the granted permission set (called after user consent changes). */
    suspend fun updateGrantedPermissions(id: ContributionId, permissions: Set<Permission>)
}
