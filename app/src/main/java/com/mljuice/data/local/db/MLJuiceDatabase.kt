package com.mljuice.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mljuice.data.local.dao.CategoryDao
import com.mljuice.data.local.dao.ProductDao
import com.mljuice.data.local.dao.SaleDao
import com.mljuice.data.local.dao.ShiftDao
import com.mljuice.data.local.dao.StoreProfileDao
import com.mljuice.data.local.entity.CategoryEntity
import com.mljuice.data.local.entity.ProductEntity
import com.mljuice.data.local.entity.ProductVariantEntity
import com.mljuice.data.local.entity.SaleEntity
import com.mljuice.data.local.entity.SaleItemEntity
import com.mljuice.data.local.entity.ShiftEntity
import com.mljuice.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ShiftEntity::class,
        StoreProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MLJuiceDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun shiftDao(): ShiftDao
    abstract fun storeProfileDao(): StoreProfileDao

    companion object {
        @Volatile
        private var INSTANCE: MLJuiceDatabase? = null

        fun getInstance(context: Context): MLJuiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MLJuiceDatabase::class.java,
                    "mljuice_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseSeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultMLJuiceMenu(database)
                }
            }
        }

        private suspend fun seedDefaultMLJuiceMenu(db: MLJuiceDatabase) {
            val storeId = "default_store"

            // 1. Kategori Menu
            val catJus = CategoryEntity(id = "cat_jus", storeId = storeId, name = "Jus Buah Segar", sortOrder = 1)
            val catSmoothie = CategoryEntity(id = "cat_smoothie", storeId = storeId, name = "Smoothies", sortOrder = 2)
            val catMilkshake = CategoryEntity(id = "cat_milkshake", storeId = storeId, name = "Milkshake & Tea", sortOrder = 3)
            val catTopping = CategoryEntity(id = "cat_topping", storeId = storeId, name = "Topping Tambahan", sortOrder = 4)

            db.categoryDao().insertCategories(listOf(catJus, catSmoothie, catMilkshake, catTopping))

            // 2. Menu Jus & Varian Ukuran Gelas
            val products = listOf(
                ProductEntity(
                    id = "p_alpukat",
                    storeId = storeId,
                    name = "Jus Alpukat Kocok",
                    categoryId = catJus.id,
                    categoryName = catJus.name,
                    purchasePrice = 7000.0,
                    price = 15000.0,
                    hasVariant = true,
                    recipeNotes = "Alpukat matang 150g, susu kental manis cokelat di dinding cup, es kristal"
                ),
                ProductEntity(
                    id = "p_mangga",
                    storeId = storeId,
                    name = "Jus Mangga Manalagi",
                    categoryId = catJus.id,
                    categoryName = catJus.name,
                    purchasePrice = 6000.0,
                    price = 14000.0,
                    hasVariant = true,
                    recipeNotes = "Mangga 150g, gula cair 30ml, es kristal"
                ),
                ProductEntity(
                    id = "p_naga",
                    storeId = storeId,
                    name = "Jus Buah Naga Merah",
                    categoryId = catJus.id,
                    categoryName = catJus.name,
                    purchasePrice = 5000.0,
                    price = 13000.0,
                    hasVariant = true,
                    recipeNotes = "Buah naga 150g, susu kental manis putih, es kristal"
                ),
                ProductEntity(
                    id = "p_sirsak",
                    storeId = storeId,
                    name = "Jus Sirsak Manis",
                    categoryId = catJus.id,
                    categoryName = catJus.name,
                    purchasePrice = 5000.0,
                    price = 13000.0,
                    hasVariant = true,
                    recipeNotes = "Sirsak buang biji 120g, gula 40ml"
                ),
                ProductEntity(
                    id = "p_jeruk",
                    storeId = storeId,
                    name = "Jus Jeruk Peras Asli",
                    categoryId = catJus.id,
                    categoryName = catJus.name,
                    purchasePrice = 4000.0,
                    price = 12000.0,
                    hasVariant = true,
                    recipeNotes = "Jeruk peras murni 4 buah, gula cair 25ml, es"
                ),
                ProductEntity(
                    id = "p_strawberry_smoothie",
                    storeId = storeId,
                    name = "Strawberry Banana Smoothie",
                    categoryId = catSmoothie.id,
                    categoryName = catSmoothie.name,
                    purchasePrice = 9000.0,
                    price = 20000.0,
                    hasVariant = true,
                    recipeNotes = "Strawberry beku 80g, pisang 1 buah, yogurt 50ml, susu segar"
                ),
                ProductEntity(
                    id = "p_mango_smoothie",
                    storeId = storeId,
                    name = "Mango Tango Smoothie",
                    categoryId = catSmoothie.id,
                    categoryName = catSmoothie.name,
                    purchasePrice = 8500.0,
                    price = 19000.0,
                    hasVariant = true,
                    recipeNotes = "Mangga beku 120g, yogurt 50ml, madu murni"
                ),
                ProductEntity(
                    id = "p_boba",
                    storeId = storeId,
                    name = "Extra Brown Sugar Boba",
                    categoryId = catTopping.id,
                    categoryName = catTopping.name,
                    purchasePrice = 1000.0,
                    price = 3000.0,
                    hasVariant = false,
                    unit = "Porsi"
                ),
                ProductEntity(
                    id = "p_jelly",
                    storeId = storeId,
                    name = "Extra Rainbow Jelly",
                    categoryId = catTopping.id,
                    categoryName = catTopping.name,
                    purchasePrice = 1000.0,
                    price = 3000.0,
                    hasVariant = false,
                    unit = "Porsi"
                )
            )
            db.productDao().insertProducts(products)

            // 3. Varian Ukuran Gelas untuk Tiap Minuman
            val variants = mutableListOf<ProductVariantEntity>()
            for (p in products.filter { it.hasVariant }) {
                variants.add(ProductVariantEntity(productId = p.id, name = "Reguler (16oz)", price = p.price))
                variants.add(ProductVariantEntity(productId = p.id, name = "Large (22oz)", price = p.price + 3000.0))
                variants.add(ProductVariantEntity(productId = p.id, name = "Jumbo (32oz)", price = p.price + 8000.0))
            }
            db.productDao().insertVariants(variants)

            // 4. Profil Toko Default
            db.storeProfileDao().saveProfile(
                StoreProfileEntity(
                    id = storeId,
                    storeName = "ML JUICE",
                    address = "Pusat Minuman Segar & Sehat",
                    phone = "0812-3456-7890"
                )
            )
        }
    }
}
