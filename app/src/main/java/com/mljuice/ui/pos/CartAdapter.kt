package com.mljuice.ui.pos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mljuice.R
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onItemChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_line, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = items[position]
        holder.tvName.text = cartItem.product.name
        holder.tvModifiers.text = cartItem.modifierSummary
        holder.tvPrice.text = currencyFormatter.format(cartItem.itemSubtotal)
        holder.tvQty.text = cartItem.quantity.toString()

        holder.btnPlus.setOnClickListener {
            cartItem.quantity++
            notifyItemChanged(position)
            onItemChanged()
        }

        holder.btnMinus.setOnClickListener {
            if (cartItem.quantity > 1) {
                cartItem.quantity--
                notifyItemChanged(position)
            } else {
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    items.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                }
            }
            onItemChanged()
        }
    }

    override fun getItemCount(): Int = items.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvCartItemName)
        val tvModifiers: TextView = itemView.findViewById(R.id.tvCartItemModifiers)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
        val tvQty: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
    }
}
