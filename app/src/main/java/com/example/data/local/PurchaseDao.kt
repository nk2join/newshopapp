package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Purchase
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY date DESC, id DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases ORDER BY date DESC, id DESC")
    suspend fun getAllPurchasesList(): List<Purchase>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: Long): Purchase?

    @Query("SELECT * FROM purchases WHERE productId = :productId ORDER BY date DESC, id DESC")
    fun getPurchasesForProduct(productId: Long): Flow<List<Purchase>>

    @Query("SELECT COUNT(*) FROM purchases WHERE productId = :productId")
    suspend fun countPurchasesForProduct(productId: Long): Int

    @Query("SELECT * FROM purchases WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getPurchasesBetween(startDate: Long, endDate: Long): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    suspend fun getPurchasesBetweenDirect(startDate: Long, endDate: Long): List<Purchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: Purchase): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(purchases: List<Purchase>)

    @Update
    suspend fun update(purchase: Purchase)

    @Delete
    suspend fun delete(purchase: Purchase)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM purchases")
    suspend fun clearAll()
}
