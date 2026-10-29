package com.example.focusfortvs.data.repository

import com.example.focusfortvs.data.local.dao.ProfileDao
import com.example.focusfortvs.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
) {
    fun observeProfile(): Flow<ProfileEntity?> = profileDao.observeProfile()

    suspend fun getProfile(): ProfileEntity? = profileDao.getProfile()

    suspend fun saveProfile(profile: ProfileEntity) = profileDao.upsert(profile)

    suspend fun setCurrentMode(mode: String) {
        val existing = profileDao.getProfile()
        val profile = existing ?: ProfileEntity(
            displayName = "FocusFort User",
            currentMode = mode,
            createdAtMillis = System.currentTimeMillis(),
        )
        profileDao.upsert(profile.copy(currentMode = mode))
    }
}
