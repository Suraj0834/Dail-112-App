package com.dial112.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.dial112.data.local.dao.*
import com.dial112.data.local.entity.*
import com.dial112.data.local.converters.Converters

/**
 * Dial112Database - Main Room Database
 *
 * Provides offline caching for:
 * - User session data
 * - Filed cases / FIRs
 * - SOS history
 * - Crime hotspots
 */
@Database(
    entities = [
        UserEntity::class,
        CaseEntity::class,
        SosLogEntity::class,
        HotspotEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Dial112Database : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun caseDao(): CaseDao
    abstract fun sosLogDao(): SosLogDao
    abstract fun hotspotDao(): HotspotDao

    companion object {
        const val DATABASE_NAME = "dial112_db"
    }
}
