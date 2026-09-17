package com.gis.supermercados.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.gis.supermercados.data.local.entity.UserEntity

/** Usuario con el nombre de su sucursal asignada. */
data class UserWithStoreRow(
    @Embedded val user: UserEntity,
    @ColumnInfo(name = "store_name") val storeName: String = "",
)
