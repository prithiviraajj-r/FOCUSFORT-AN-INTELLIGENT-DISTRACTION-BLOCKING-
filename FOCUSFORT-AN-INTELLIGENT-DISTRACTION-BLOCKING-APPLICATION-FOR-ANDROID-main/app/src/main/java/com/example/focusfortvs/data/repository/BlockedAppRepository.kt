package com.example.focusfortvs.data.repository

import com.example.focusfortvs.data.local.dao.BlockedAppDao
import com.example.focusfortvs.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppRepository @Inject constructor(
    private val blockedAppDao: BlockedAppDao,
) {
    fun observeForMode(mode: String): Flow<List<BlockedAppEntity>> = blockedAppDao.observeForMode(mode)

    suspend fun getForMode(mode: String): List<BlockedAppEntity> = blockedAppDao.getForMode(mode)

    suspend fun saveSelection(mode: String, packageName: String, appLabel: String) {
        blockedAppDao.upsert(
            BlockedAppEntity(
                packageName = packageName.trim(),
                mode = mode.trim(),
                appLabel = appLabel,
                isEnabled = true,
            ),
        )
    }

    suspend fun removeSelection(mode: String, packageName: String) {
        blockedAppDao.getForMode(mode.trim())
            .find { it.packageName == packageName }
            ?.let { blockedAppDao.delete(it) }
    }

    suspend fun replaceSelections(mode: String, selectedPackages: Set<String>, appLabels: Map<String, String>) {
        val normalizedMode = mode.trim()
        blockedAppDao.clearForMode(normalizedMode)
        selectedPackages.forEach { packageName ->
            blockedAppDao.upsert(
                BlockedAppEntity(
                    packageName = packageName.trim(),
                    mode = normalizedMode,
                    appLabel = appLabels[packageName].orEmpty(),
                    isEnabled = true,
                ),
            )
        }
    }
}
