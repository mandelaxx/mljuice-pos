package com.mljuice.ui.kitchen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mljuice.MLJuiceApp
import com.mljuice.R
import com.mljuice.data.local.entity.SaleEntity
import com.mljuice.data.local.entity.SaleItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KitchenActivity : AppCompatActivity() {

    private val db by lazy { MLJuiceApp.instance.database }
    private lateinit var rvKitchenOrders: RecyclerView
    private lateinit var tvEmptyKitchen: TextView
    private lateinit var tvKitchenActiveCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kitchen)

        findViewById<View>(R.id.btnBackKitchen).setOnClickListener { finish() }
        rvKitchenOrders = findViewById(R.id.rvKitchenOrders)
        tvEmptyKitchen = findViewById(R.id.tvEmptyKitchen)
        tvKitchenActiveCount = findViewById(R.id.tvKitchenActiveCount)

        rvKitchenOrders.layoutManager = GridLayoutManager(this, 2)

        observeKitchenQueue()
    }

    private fun observeKitchenQueue() {
        lifecycleScope.launch {
            db.saleDao().getActiveKitchenQueue().collectLatest { orders ->
                tvKitchenActiveCount.text = "${orders.size} Antrian Aktif"
                tvEmptyKitchen.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE

                val adapter = KitchenOrderAdapter(orders, { saleId, nextStatus ->
                    lifecycleScope.launch {
                        db.saleDao().updateOrderStatus(saleId, nextStatus)
                    }
                }, { saleId ->
                    db.saleDao().getItemsForSale(saleId)
                })
                rvKitchenOrders.adapter = adapter
            }
        }
    }
}

class KitchenOrderAdapter(
    private val orders: List<SaleEntity>,
    private val onAdvanceStatus: (saleId: String, nextStatus: String) -> Unit,
    private val loadItems: suspend (saleId: String) -> List<SaleItemEntity>
) : RecyclerView.Adapter<KitchenOrderAdapter.KitchenViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KitchenViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kitchen_order, parent, false)
        return KitchenViewHolder(view)
    }

    override fun onBindViewHolder(holder: KitchenViewHolder, position: Int) {
        val order = orders[position]
        holder.tvQueueNo.text = "#${order.queueNumber}"
        holder.tvTxNo.text = order.transactionNumber
        holder.tvTime.text = timeFormat.format(Date(order.timestamp))

        // Badge Status
        when (order.orderStatus) {
            "QUEUED" -> {
                holder.tvStatusBadge.text = "MENUNGGU"
                holder.tvStatusBadge.setBackgroundColor(0xFFFFF3E0.toInt())
                holder.tvStatusBadge.setTextColor(0xFFE65100.toInt())
                holder.btnAdvance.text = "Mulai Racik Jus"
                holder.btnAdvance.setOnClickListener { onAdvanceStatus(order.id, "MAKING") }
            }
            "MAKING" -> {
                holder.tvStatusBadge.text = "SEDANG DIBUAT"
                holder.tvStatusBadge.setBackgroundColor(0xFFE3F2FD.toInt())
                holder.tvStatusBadge.setTextColor(0xFF1565C0.toInt())
                holder.btnAdvance.text = "Tandai Siap Saji"
                holder.btnAdvance.setOnClickListener { onAdvanceStatus(order.id, "READY") }
            }
            "READY" -> {
                holder.tvStatusBadge.text = "SIAP DIAMBIL"
                holder.tvStatusBadge.setBackgroundColor(0xFFE8F5E9.toInt())
                holder.tvStatusBadge.setTextColor(0xFF2E7D32.toInt())
                holder.btnAdvance.text = "Selesai Diserahkan"
                holder.btnAdvance.setOnClickListener { onAdvanceStatus(order.id, "COMPLETED") }
            }
        }

        if (order.notes.isNullOrEmpty()) {
            holder.tvNotes.visibility = View.GONE
        } else {
            holder.tvNotes.visibility = View.VISIBLE
            holder.tvNotes.text = "Catatan: ${order.notes}"
        }

        // Muat item pesanan secara asinkron
        (holder.itemView.context as? AppCompatActivity)?.lifecycleScope?.launch {
            val items = withContext(Dispatchers.IO) { loadItems(order.id) }
            val itemsText = items.joinToString("\n") { item ->
                val variant = if (!item.variantName.isNullOrEmpty()) " (${item.variantName})" else ""
                val mod = if (!item.modifierNotes.isNullOrEmpty()) "\n   → ${item.modifierNotes}" else ""
                "• ${item.quantity.toInt()}x ${item.productName}$variant$mod"
            }
            holder.tvItemsList.text = itemsText
        }
    }

    override fun getItemCount(): Int = orders.size

    class KitchenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQueueNo: TextView = itemView.findViewById(R.id.tvKitchenQueueNo)
        val tvTxNo: TextView = itemView.findViewById(R.id.tvKitchenTxNo)
        val tvTime: TextView = itemView.findViewById(R.id.tvKitchenTime)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvKitchenStatusBadge)
        val tvItemsList: TextView = itemView.findViewById(R.id.tvKitchenItemsList)
        val tvNotes: TextView = itemView.findViewById(R.id.tvKitchenNotes)
        val btnAdvance: MaterialButton = itemView.findViewById(R.id.btnAdvanceStatus)
    }
}
