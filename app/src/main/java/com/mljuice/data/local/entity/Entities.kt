package com.mljuice.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val name: String,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["categoryId"])]
)
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val purchasePrice: Double = 0.0,
    val price: Double,
    val stock: Double = 999.0,
    val unit: String = "Cup",
    val imageUrl: String? = null,
    val hasVariant: Boolean = false,
    val recipeNotes: String? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "product_variants",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["productId"])]
)
data class ProductVariantEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val name: String,                    // e.g. "Large 22oz"
    val purchasePrice: Double = 0.0,
    val price: Double
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val transactionNumber: String,
    val queueNumber: Int,
    val customerName: String = "Pelanggan Umum",
    val timestamp: Long = System.currentTimeMillis(),
    val cashierUid: String,
    val cashierName: String,
    val subtotalGross: Double,
    val discountAmount: Double = 0.0,
    val finalTotal: Double,
    val paymentType: String = "TUNAI",   // TUNAI, QRIS, TRANSFER
    val amountPaid: Double,
    val changeAmount: Double = 0.0,
    val orderStatus: String = "PAID",    // QUEUED, MAKING, READY, COMPLETED
    val notes: String? = null,
    val isSynced: Boolean = false
)

@Entity(
    tableName = "sale_items",
    indices = [Index(value = ["saleId"])]
)
data class SaleItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productId: String,
    val productName: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val quantity: Double,
    val price: Double,
    val modifierNotes: String? = null,   // "Less Ice, Gula 50%"
    val itemTotal: Double
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val cashierUid: String,
    val cashierName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val openingCash: Double,
    val closingCash: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val status: String = "OPEN"
)

@Entity(tableName = "store_profile")
data class StoreProfileEntity(
    @PrimaryKey val id: String = "default_store",
    val storeName: String = "ML JUICE",
    val address: String = "Jl. Segar Selalu No. 88",
    val phone: String = "0812-XXXX-XXXX",
    val printerMacAddress: String? = null
)
