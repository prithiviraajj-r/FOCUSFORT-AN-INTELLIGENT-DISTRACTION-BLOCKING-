package com.example.focusfortvs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.focusfortvs.data.local.dao.BlockedAppDao
import com.example.focusfortvs.data.local.dao.ProfileDao
import com.example.focusfortvs.data.local.dao.SessionDao
import com.example.focusfortvs.data.local.entity.BlockedAppEntity
import com.example.focusfortvs.data.local.entity.ProfileEntity
import com.example.focusfortvs.data.local.entity.SessionEntity
import com.example.focusfortvs.data.local.entity.UnlockEventEntity

@Database(
    entities = [
        ProfileEntity::class,
        BlockedAppEntity::class,
        SessionEntity::class,
        UnlockEventEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class FocusFortDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DB_NAME = "focusfort.db"
    }
}
