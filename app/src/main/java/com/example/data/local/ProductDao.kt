package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getAllActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllActiveProductsList(): List<Product>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsList(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductByIdDirect(id: Long): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getActiveProductByName(name: String): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchActiveProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("UPDATE products SET stockQuantity = :stock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = :stock, totalCostPool = :totalCostPool, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStockAndCostPool(id: Long, stock: Int, totalCostPool: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = :stock, totalCostPool = :totalCostPool, purchasePrice = :purchasePrice, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStockCostPoolAndPrice(id: Long, stock: Int, totalCostPool: Long, purchasePrice: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = :stock, purchasePrice = :purchasePrice, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStockAndPrice(id: Long, stock: Int, purchasePrice: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM products")
    suspend fun clearAll()
}
