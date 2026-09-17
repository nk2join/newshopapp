package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.data.model.ShopSetting

@Database(
    entities = [
        Product::class,
        Purchase::class,
        Sale::class,
        Expense::class,
        ShopSetting::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun shopSettingDao(): ShopSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN purchaseUnit TEXT NOT NULL DEFAULT 'Unit'")
                db.execSQL("ALTER TABLE products ADD COLUMN saleUnit TEXT NOT NULL DEFAULT 'Unit'")
                db.execSQL("ALTER TABLE products ADD COLUMN unitsPerPurchaseUnit INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN totalCostPool INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE purchases ADD COLUMN unitsPerPurchaseUnit INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE purchases ADD COLUMN individualUnitsAdded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE purchases ADD COLUMN costPerIndividualUnit INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE purchases SET individualUnitsAdded = quantity WHERE individualUnitsAdded = 0")
                db.execSQL("UPDATE purchases SET costPerIndividualUnit = purchasePrice WHERE costPerIndividualUnit = 0")
                db.execSQL("UPDATE products SET totalCostPool = stockQuantity * purchasePrice WHERE totalCostPool = 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shop_ledger_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
