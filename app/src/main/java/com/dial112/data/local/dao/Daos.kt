package com.dial112.data.local.dao

import androidx.room.*
import com.dial112.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * UserDao - Database access for user session
 */
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

/**
 * CaseDao - Database access for cases/FIRs
 */
@Dao
interface CaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCases(cases: List<CaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: CaseEntity)

    @Query("SELECT * FROM cases ORDER BY createdAt DESC")
    fun observeAllCases(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE id = :caseId")
    suspend fun getCaseById(caseId: String): CaseEntity?

    @Query("DELETE FROM cases")
    suspend fun clearCases()
}

/**
 * SosLogDao - Database access for SOS history
 */
@Dao
interface SosLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSosLog(log: SosLogEntity)

    @Query("SELECT * FROM sos_logs ORDER BY timestamp DESC")
    fun observeAllSosLogs(): Flow<List<SosLogEntity>>

    @Query("SELECT * FROM sos_logs WHERE synced = 0")
    suspend fun getUnsyncedLogs(): List<SosLogEntity>

    @Query("UPDATE sos_logs SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}

/**
 * HotspotDao - Database access for crime hotspots
 */
@Dao
interface HotspotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspots(hotspots: List<HotspotEntity>)

    @Query("SELECT * FROM hotspots")
    suspend fun getAllHotspots(): List<HotspotEntity>

    @Query("SELECT * FROM hotspots")
    fun observeHotspots(): Flow<List<HotspotEntity>>

    @Query("DELETE FROM hotspots")
    suspend fun clearHotspots()
}
