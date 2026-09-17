package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC, id DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY date DESC, id DESC")
    suspend fun getAllSalesList(): List<Sale>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE productId = :productId ORDER BY date DESC, id DESC")
    fun getSalesForProduct(productId: Long): Flow<List<Sale>>

    @Query("SELECT COUNT(*) FROM sales WHERE productId = :productId")
    suspend fun countSalesForProduct(productId: Long): Int

    @Query("SELECT * FROM sales WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getSalesBetween(startDate: Long, endDate: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    suspend fun getSalesBetweenDirect(startDate: Long, endDate: Long): List<Sale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<Sale>)

    @Update
    suspend fun update(sale: Sale)

    @Delete
    suspend fun delete(sale: Sale)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sales")
    suspend fun clearAll()
}
