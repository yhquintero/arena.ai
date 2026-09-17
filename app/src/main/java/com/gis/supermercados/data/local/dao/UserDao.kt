package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gis.supermercados.data.local.entity.UserEntity
import com.gis.supermercados.domain.model.Role
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query(
        """
        SELECT u.*, IFNULL(s.name, '') AS store_name
        FROM users u LEFT JOIN stores s ON s.id = u.store_id
        ORDER BY u.is_active DESC, u.full_name ASC
        """
    )
    fun observeAllWithStore(): Flow<List<UserWithStoreRow>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM users WHERE is_active = 1")
    fun observeActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert
    suspend fun insertAll(users: List<UserEntity>): List<Long>

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET password_hash = :hash, password_salt = :salt WHERE id = :userId")
    suspend fun updatePassword(userId: Long, hash: String, salt: String): Int

    @Query("UPDATE users SET failed_attempts = :attempts, locked_until = :lockedUntil WHERE id = :userId")
    suspend fun updateFailedAttempts(userId: Long, attempts: Int, lockedUntil: Long): Int

    @Query("UPDATE users SET last_login_at = :timestamp, failed_attempts = 0, locked_until = 0 WHERE id = :userId")
    suspend fun registerSuccessfulLogin(userId: Long, timestamp: Long): Int

    @Query("UPDATE users SET biometric_enabled = :enabled WHERE id = :userId")
    suspend fun setBiometricEnabled(userId: Long, enabled: Boolean): Int

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun updateRole(userId: Long, role: Role): Int

    @Query("UPDATE users SET is_active = :isActive WHERE id = :userId")
    suspend fun setActive(userId: Long, isActive: Boolean): Int

    @Query("UPDATE users SET store_id = :storeId WHERE id = :userId")
    suspend fun updateStore(userId: Long, storeId: Long?): Int
}
