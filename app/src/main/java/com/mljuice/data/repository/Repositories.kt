package com.mljuice.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mljuice.data.local.dao.CategoryDao
import com.mljuice.data.local.dao.ProductDao
import com.mljuice.data.local.dao.SaleDao
import com.mljuice.data.local.entity.CategoryEntity
import com.mljuice.data.local.entity.ProductEntity
import com.mljuice.data.local.entity.ProductVariantEntity
import com.mljuice.data.local.entity.SaleEntity
import com.mljuice.data.local.entity.SaleItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserUid(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    suspend fun login(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}

class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getProducts(): Flow<List<ProductEntity>> = productDao.getAllActiveProducts()
    fun getCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getVariants(productId: String): List<ProductVariantEntity> = withContext(Dispatchers.IO) {
        productDao.getVariantsForProduct(productId)
    }

    suspend fun saveProduct(product: ProductEntity, variants: List<ProductVariantEntity>) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
        if (variants.isNotEmpty()) {
            productDao.insertVariants(variants)
        }
        // Background sync to Firestore
        try {
            firestore.collection("stores").document(product.storeId)
                .collection("products").document(product.id)
                .set(product).await()
        } catch (_: Exception) {}
    }
}

class SaleRepository(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()

    suspend fun recordSale(sale: SaleEntity, items: List<SaleItemEntity>) = withContext(Dispatchers.IO) {
        // 1. Simpan transaksi & detail ke Room lokal secara instan
        saleDao.insertCompleteSale(sale, items)

        // 2. Kurangi stok bahan/produk di Room
        for (item in items) {
            productDao.decreaseStock(item.productId, item.quantity)
        }

        // 3. Sinkronisasikan ke Cloud Firestore
        try {
            val saleData = hashMapOf(
                "id" to sale.id,
                "storeId" to sale.storeId,
                "transactionNumber" to sale.transactionNumber,
                "queueNumber" to sale.queueNumber,
                "customerName" to sale.customerName,
                "timestamp" to sale.timestamp,
                "cashierUid" to sale.cashierUid,
                "cashierName" to sale.cashierName,
                "finalTotal" to sale.finalTotal,
                "paymentType" to sale.paymentType,
                "amountPaid" to sale.amountPaid,
                "changeAmount" to sale.changeAmount,
                "orderStatus" to sale.orderStatus,
                "notes" to sale.notes,
                "items" to items
            )
            firestore.collection("stores").document(sale.storeId)
                .collection("sales").document(sale.id)
                .set(saleData).await()

            saleDao.markAsSynced(sale.id)
        } catch (_: Exception) {
            // Tetap tersimpan di Room lokal dengan flag isSynced = false
        }
    }
}
