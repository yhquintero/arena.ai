package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gis.supermercados.domain.model.Role

/**
 * Usuario local. La contrasena NUNCA se guarda en claro:
 * se almacena el hash PBKDF2-HMAC-SHA256 con sal aleatoria por usuario.
 */
@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["username"], unique = true), Index(value = ["store_id"])]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    @ColumnInfo(name = "full_name") val fullName: String = "",
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    val role: Role = Role.CAJERO,
    @ColumnInfo(name = "store_id") val storeId: Long? = null,
    @ColumnInfo(name = "biometric_enabled") val biometricEnabled: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "failed_attempts") val failedAttempts: Int = 0,
    @ColumnInfo(name = "locked_until") val lockedUntil: Long = 0L,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "last_login_at") val lastLoginAt: Long = 0L,
)
