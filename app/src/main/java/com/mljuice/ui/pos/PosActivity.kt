package com.mljuice.ui.pos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.mljuice.MLJuiceApp
import com.mljuice.R
import com.mljuice.data.local.entity.ProductEntity
import com.mljuice.data.local.entity.ProductVariantEntity
import com.mljuice.data.local.entity.SaleEntity
import com.mljuice.data.local.entity.SaleItemEntity
import com.mljuice.data.repository.ProductRepository
import com.mljuice.data.repository.SaleRepository
import com.mljuice.printer.BluetoothPrinterHelper
import com.mljuice.ui.kitchen.KitchenActivity
import com.mljuice.ui.reports.ReportActivity
import com.mljuice.ui.shift.ShiftActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class PosActivity : AppCompatActivity() {

    private val db by lazy { MLJuiceApp.instance.database }
    private val productRepo by lazy { ProductRepository(db.productDao(), db.categoryDao()) }
    private val saleRepo by lazy { SaleRepository(db.saleDao(), db.productDao()) }

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private var allProducts = listOf<ProductEntity>()
    private var displayedProducts = listOf<ProductEntity>()
    private val cartItems = mutableListOf<CartItem>()

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var cartAdapter: CartAdapter

    private lateinit var rvMenuGrid: RecyclerView
    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvCartSubtotal: TextView
    private lateinit var tvCartGrandTotal: TextView
    private lateinit var btnProceedCheckout: MaterialButton
    private lateinit var tvQueueBadge: TextView
    private lateinit var etSearchMenu: EditText
    private lateinit var chipGroupCategories: ChipGroup

    private var currentQueueNumber = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        initViews()
        setupRecyclerViews()
        setupListeners()
        observeProducts()
    }

    private fun initViews() {
        rvMenuGrid = findViewById(R.id.rvMenuGrid)
        rvCartItems = findViewById(R.id.rvCartItems)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        tvCartSubtotal = findViewById(R.id.tvCartSubtotal)
        tvCartGrandTotal = findViewById(R.id.tvCartGrandTotal)
        btnProceedCheckout = findViewById(R.id.btnProceedCheckout)
        tvQueueBadge = findViewById(R.id.tvQueueBadge)
        etSearchMenu = findViewById(R.id.etSearchMenu)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)

        tvQueueBadge.text = "Antrian #$currentQueueNumber"

        // Top Navigation buttons
        findViewById<View>(R.id.btnNavKitchen).setOnClickListener {
            startActivity(Intent(this, KitchenActivity::class.java))
        }
        findViewById<View>(R.id.btnNavReports).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
        findViewById<View>(R.id.btnNavShift).setOnClickListener {
            startActivity(Intent(this, ShiftActivity::class.java))
        }
    }

    private fun setupRecyclerViews() {
        menuAdapter = MenuAdapter(emptyList()) { product ->
            showModifierDialog(product)
        }
        rvMenuGrid.layoutManager = GridLayoutManager(this, 3)
        rvMenuGrid.adapter = menuAdapter

        cartAdapter = CartAdapter(cartItems) {
            updateCartSummary()
        }
        rvCartItems.layoutManager = LinearLayoutManager(this)
        rvCartItems.adapter = cartAdapter
    }

    private fun setupListeners() {
        etSearchMenu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterMenu(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.first()
            val categoryKeyword = when (checkedId) {
                R.id.chipJus -> "Jus"
                R.id.chipSmoothie -> "Smoothie"
                R.id.chipTopping -> "Topping"
                else -> ""
            }
            filterMenuByCategory(categoryKeyword)
        }

        btnProceedCheckout.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                showPaymentDialog()
            }
        }
    }

    private fun observeProducts() {
        lifecycleScope.launch {
            productRepo.getProducts().collectLatest { products ->
                allProducts = products
                displayedProducts = products
                menuAdapter.updateProducts(products)
            }
        }
    }

    private fun filterMenu(query: String) {
        displayedProducts = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(query, ignoreCase = true) || it.categoryName.contains(query, ignoreCase = true) }
        }
        menuAdapter.updateProducts(displayedProducts)
    }

    private fun filterMenuByCategory(categoryKeyword: String) {
        displayedProducts = if (categoryKeyword.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.categoryName.contains(categoryKeyword, ignoreCase = true) || it.name.contains(categoryKeyword, ignoreCase = true) }
        }
        menuAdapter.updateProducts(displayedProducts)
    }

    private fun showModifierDialog(product: ProductEntity) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modifier, null)
        dialog.setContentView(dialogView)

        dialogView.findViewById<TextView>(R.id.tvModifierTitle).text = product.name
        dialogView.findViewById<View>(R.id.btnCloseModifier).setOnClickListener { dialog.dismiss() }

        val rgSize = dialogView.findViewById<RadioGroup>(R.id.rgSize)
        val rgIce = dialogView.findViewById<RadioGroup>(R.id.rgIce)
        val rgSugar = dialogView.findViewById<RadioGroup>(R.id.rgSugar)
        val cbBoba = dialogView.findViewById<CheckBox>(R.id.cbBoba)
        val cbJelly = dialogView.findViewById<CheckBox>(R.id.cbJelly)
        val cbChia = dialogView.findViewById<CheckBox>(R.id.cbChia)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmModifier)

        // Jika bukan produk minuman (misal hanya topping/snack), sembunyikan opsi ukuran/es
        if (!product.hasVariant) {
            rgSize.visibility = View.GONE
            rgIce.visibility = View.GONE
            rgSugar.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            var selectedSizeName = "Reguler (16oz)"
            var basePrice = product.price

            if (product.hasVariant) {
                when (rgSize.checkedRadioButtonId) {
                    R.id.rbLarge -> {
                        selectedSizeName = "Large (22oz)"
                        basePrice = product.price + 3000.0
                    }
                    R.id.rbJumbo -> {
                        selectedSizeName = "Jumbo (32oz)"
                        basePrice = product.price + 8000.0
                    }
                    else -> {
                        selectedSizeName = "Reguler (16oz)"
                        basePrice = product.price
                    }
                }
            }

            val selectedIce = when (rgIce.checkedRadioButtonId) {
                R.id.rbIceLess -> "Sedikit Es"
                R.id.rbIceNone -> "Tanpa Es"
                else -> "Normal Ice"
            }

            val selectedSugar = when (rgSugar.checkedRadioButtonId) {
                R.id.rbSugarLess -> "Gula 50%"
                R.id.rbSugarNone -> "Tanpa Gula"
                else -> "Normal Sugar"
            }

            val toppings = mutableListOf<String>()
            if (cbBoba.isChecked) toppings.add("Boba")
            if (cbJelly.isChecked) toppings.add("Jelly")
            if (cbChia.isChecked) toppings.add("Chia Seed")

            val cartItem = CartItem(
                product = product,
                sizeName = selectedSizeName,
                iceLevel = selectedIce,
                sugarLevel = selectedSugar,
                toppings = toppings,
                quantity = 1,
                baseUnitPrice = basePrice
            )

            cartItems.add(cartItem)
            cartAdapter.notifyItemInserted(cartItems.size - 1)
            updateCartSummary()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateCartSummary() {
        val totalQty = cartItems.sumOf { it.quantity }
        val grandTotal = cartItems.sumOf { it.itemSubtotal }

        if (cartItems.isEmpty()) {
            tvEmptyCart.visibility = View.VISIBLE
            btnProceedCheckout.isEnabled = false
            btnProceedCheckout.text = "Bayar Sekarang (0 Item)"
            tvCartSubtotal.text = "Rp 0"
            tvCartGrandTotal.text = "Rp 0"
        } else {
            tvEmptyCart.visibility = View.GONE
            btnProceedCheckout.isEnabled = true
            btnProceedCheckout.text = "Bayar Sekarang ($totalQty Item)"
            tvCartSubtotal.text = currencyFormatter.format(grandTotal)
            tvCartGrandTotal.text = currencyFormatter.format(grandTotal)
        }
    }

    private fun showPaymentDialog() {
        val grandTotal = cartItems.sumOf { it.itemSubtotal }

        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        dialog.setContentView(dialogView)

        val tvTotalBill = dialogView.findViewById<TextView>(R.id.tvTotalBill)
        val rgPaymentMethod = dialogView.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val etCashAmount = dialogView.findViewById<EditText>(R.id.etCashAmount)
        val tvChangeAmount = dialogView.findViewById<TextView>(R.id.tvChangeAmount)
        val etOrderNotes = dialogView.findViewById<EditText>(R.id.etOrderNotes)
        val cbAutoPrint = dialogView.findViewById<CheckBox>(R.id.cbAutoPrint)
        val btnProcessPayment = dialogView.findViewById<MaterialButton>(R.id.btnProcessPayment)

        tvTotalBill.text = currencyFormatter.format(grandTotal)
        etCashAmount.setText(grandTotal.toLong().toString())
        tvChangeAmount.text = "Rp 0"

        // Quick Cash Buttons
        dialogView.findViewById<View>(R.id.btnExactCash).setOnClickListener {
            etCashAmount.setText(grandTotal.toLong().toString())
        }
        dialogView.findViewById<View>(R.id.btn20k).setOnClickListener {
            etCashAmount.setText("20000")
        }
        dialogView.findViewById<View>(R.id.btn50k).setOnClickListener {
            etCashAmount.setText("50000")
        }
        dialogView.findViewById<View>(R.id.btn100k).setOnClickListener {
            etCashAmount.setText("100000")
        }

        etCashAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputCash = s.toString().toDoubleOrNull() ?: 0.0
                val change = (inputCash - grandTotal).coerceAtLeast(0.0)
                tvChangeAmount.text = currencyFormatter.format(change)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnProcessPayment.setOnClickListener {
            val paymentType = when (rgPaymentMethod.checkedRadioButtonId) {
                R.id.rbQris -> "QRIS"
                R.id.rbTransfer -> "TRANSFER"
                else -> "TUNAI"
            }

            val cashPaid = if (paymentType == "TUNAI") {
                etCashAmount.text.toString().toDoubleOrNull() ?: grandTotal
            } else {
                grandTotal
            }

            if (cashPaid < grandTotal) {
                Toast.makeText(this, "Jumlah uang tunai kurang dari total tagihan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val change = cashPaid - grandTotal
            val notes = etOrderNotes.text.toString().trim()
            val autoPrint = cbAutoPrint.isChecked

            completeTransaction(grandTotal, paymentType, cashPaid, change, notes, autoPrint)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun completeTransaction(
        total: Double,
        paymentType: String,
        amountPaid: Double,
        change: Double,
        notes: String?,
        autoPrint: Boolean
    ) {
        val saleId = UUID.randomUUID().toString()
        val queueNo = currentQueueNumber++
        tvQueueBadge.text = "Antrian #$currentQueueNumber"

        val sale = SaleEntity(
            id = saleId,
            storeId = "default_store",
            transactionNumber = "MLJ-${System.currentTimeMillis()}",
            queueNumber = queueNo,
            subtotalGross = total,
            finalTotal = total,
            paymentType = paymentType,
            amountPaid = amountPaid,
            changeAmount = change,
            cashierUid = "owner_kasir",
            cashierName = "Kasir ML Juice",
            orderStatus = "QUEUED",
            notes = notes
        )

        val saleItems = cartItems.map { cart ->
            SaleItemEntity(
                saleId = saleId,
                productId = cart.product.id,
                productName = cart.product.name,
                variantName = cart.sizeName,
                quantity = cart.quantity.toDouble(),
                price = cart.unitPriceWithModifiers,
                modifierNotes = cart.modifierSummary,
                itemTotal = cart.itemSubtotal
            )
        }

        lifecycleScope.launch {
            // 1. Simpan ke Room lokal & cloud
            saleRepo.recordSale(sale, saleItems)

            // 2. Cetak struk dan tiket barista bila dicentang
            if (autoPrint) {
                val profile = db.storeProfileDao().getProfile()
                BluetoothPrinterHelper.printCustomerReceipt(this@PosActivity, profile, sale, saleItems)
                BluetoothPrinterHelper.printKitchenTicket(this@PosActivity, sale, saleItems)
            }

            Toast.makeText(this@PosActivity, "✓ Transaksi Berhasil! Antrian #$queueNo", Toast.LENGTH_LONG).show()

            // 3. Bersihkan keranjang belanja
            cartItems.clear()
            cartAdapter.notifyDataSetChanged()
            updateCartSummary()
        }
    }
}
