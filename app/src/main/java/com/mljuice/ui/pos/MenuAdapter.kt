package com.mljuice.ui.pos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mljuice.R
import com.mljuice.data.local.entity.ProductEntity
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter(
    private var products: List<ProductEntity>,
    private val onProductSelected: (ProductEntity) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu_card, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val product = products[position]
        holder.tvName.text = product.name
        holder.tvCategory.text = product.categoryName
        holder.tvPrice.text = currencyFormatter.format(product.price)

        holder.itemView.setOnClickListener { onProductSelected(product) }
        holder.btnAddToCart.setOnClickListener { onProductSelected(product) }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<ProductEntity>) {
        products = newProducts
        notifyDataSetChanged()
    }

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val btnAddToCart: MaterialButton = itemView.findViewById(R.id.btnAddToCart)
    }
}
