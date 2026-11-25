package com.mljuice.ui.pos

import com.mljuice.data.local.entity.ProductEntity
import com.mljuice.data.local.entity.ProductVariantEntity
import java.util.UUID

data class CartItem(
    val cartId: String = UUID.randomUUID().toString(),
    val product: ProductEntity,
    val variant: ProductVariantEntity? = null,
    val sizeName: String = "Reguler (16oz)",
    val iceLevel: String = "Normal Ice",
    val sugarLevel: String = "Normal Sugar",
    val toppings: List<String> = emptyList(),
    var quantity: Int = 1,
    val baseUnitPrice: Double
) {
    val unitPriceWithModifiers: Double
        get() {
            var price = baseUnitPrice
            // Setiap topping menambah Rp 3.000
            price += (toppings.size * 3000.0)
            return price
        }

    val itemSubtotal: Double
        get() = unitPriceWithModifiers * quantity

    val modifierSummary: String
        get() {
            val parts = mutableListOf<String>()
            parts.add(sizeName)
            if (iceLevel != "Normal Ice") parts.add(iceLevel)
            if (sugarLevel != "Normal Sugar") parts.add(sugarLevel)
            if (toppings.isNotEmpty()) parts.add(toppings.joinToString(", "))
            return parts.joinToString(" • ")
        }
}
