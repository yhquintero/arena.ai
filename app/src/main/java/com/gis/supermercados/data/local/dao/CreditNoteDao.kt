package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.gis.supermercados.data.local.entity.CreditNoteEntity
import com.gis.supermercados.data.local.entity.CreditNoteItemEntity
import com.gis.supermercados.data.local.entity.CreditNoteWithItemsRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditNoteDao {

    @Transaction
    @Query(
        """
        SELECT * FROM credit_notes
        WHERE (:storeId IS NULL OR store_id = :storeId)
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeAll(storeId: Long?, limit: Int = 200): Flow<List<CreditNoteWithItemsRow>>

    @Transaction
    @Query("SELECT * FROM credit_notes WHERE id = :creditNoteId")
    suspend fun getWithItems(creditNoteId: Long): CreditNoteWithItemsRow?

    @Query("SELECT * FROM credit_notes WHERE sale_id = :saleId ORDER BY created_at DESC")
    fun observeBySale(saleId: Long): Flow<List<CreditNoteEntity>>

    @Query("SELECT IFNULL(MAX(id), 0) FROM credit_notes")
    suspend fun maxId(): Long

    @Insert
    suspend fun insert(note: CreditNoteEntity): Long

    @Insert
    suspend fun insertItems(items: List<CreditNoteItemEntity>)

    @Query("DELETE FROM credit_notes WHERE id = :creditNoteId")
    suspend fun deleteById(creditNoteId: Long): Int
}
