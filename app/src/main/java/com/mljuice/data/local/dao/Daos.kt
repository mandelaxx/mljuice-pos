package com.mljuice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mljuice.data.local.entity.CategoryEntity
import com.mljuice.data.local.entity.ProductEntity
import com.mljuice.data.local.entity.ProductVariantEntity
import com.mljuice.data.local.entity.SaleEntity
import com.mljuice.data.local.entity.SaleItemEntity
import com.mljuice.data.local.entity.ShiftEntity
import com.mljuice.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.flow.Flow

data class BestSellerItem(
    val productName: String,
    val totalQty: Double,
    val totalRevenue: Double
)

data class SalesSummary(
    val totalRevenue: Double,
    val totalTransactions: Int,
    val totalCash: Double,
    val totalNonCash: Double
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product_variants WHERE productId = :productId")
    suspend fun getVariantsForProduct(productId: String): List<ProductVariantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<ProductVariantEntity>)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId")
    suspend fun decreaseStock(productId: String, qty: Double)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE orderStatus IN ('QUEUED', 'MAKING', 'READY') ORDER BY timestamp ASC")
    fun getActiveKitchenQueue(): Flow<List<SaleEntity>>

    @Query("UPDATE sales SET orderStatus = :status WHERE id = :saleId")
    suspend fun updateOrderStatus(saleId: String, status: String)

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: String): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("UPDATE sales SET isSynced = 1 WHERE id = :saleId")
    suspend fun markAsSynced(saleId: String)

    @Transaction
    suspend fun insertCompleteSale(sale: SaleEntity, items: List<SaleItemEntity>) {
        insertSale(sale)
        insertSaleItems(items)
    }

    // Reports & Analytics
    @Query("""
        SELECT 
            COALESCE(SUM(finalTotal), 0.0) as totalRevenue,
            COUNT(id) as totalTransactions,
            COALESCE(SUM(CASE WHEN paymentType = 'TUNAI' THEN finalTotal ELSE 0.0 END), 0.0) as totalCash,
            COALESCE(SUM(CASE WHEN paymentType != 'TUNAI' THEN finalTotal ELSE 0.0 END), 0.0) as totalNonCash
        FROM sales 
        WHERE timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getSalesSummary(startTime: Long, endTime: Long): SalesSummary

    @Query("""
        SELECT productName, SUM(quantity) as totalQty, SUM(itemTotal) as totalRevenue
        FROM sale_items
        INNER JOIN sales ON sale_items.saleId = sales.id
        WHERE sales.timestamp BETWEEN :startTime AND :endTime
        GROUP BY productName
        ORDER BY totalQty DESC
        LIMIT 10
    """)
    suspend fun getBestSellers(startTime: Long, endTime: Long): List<BestSellerItem>
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveShift(): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Query("UPDATE shifts SET status = 'CLOSED', endTime = :endTime, closingCash = :closingCash, totalCashSales = :cashSales WHERE id = :shiftId")
    suspend fun closeShift(shiftId: String, endTime: Long, closingCash: Double, cashSales: Double)
}

@Dao
interface StoreProfileDao {
    @Query("SELECT * FROM store_profile WHERE id = 'default_store' LIMIT 1")
    suspend fun getProfile(): StoreProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: StoreProfileEntity)

    @Query("UPDATE store_profile SET printerMacAddress = :mac WHERE id = 'default_store'")
    suspend fun updatePrinterMac(mac: String)
}
