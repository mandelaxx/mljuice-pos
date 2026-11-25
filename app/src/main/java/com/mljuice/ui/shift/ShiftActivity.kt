package com.mljuice.ui.shift

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mljuice.MLJuiceApp
import com.mljuice.R
import com.mljuice.data.local.entity.ShiftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ShiftActivity : AppCompatActivity() {

    private val db by lazy { MLJuiceApp.instance.database }
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val timeFormat = SimpleDateFormat("HH:mm WIB", Locale("id", "ID"))

    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStartTime: TextView
    private lateinit var tvOpeningCash: TextView
    private lateinit var tvActionPrompt: TextView
    private lateinit var etCashInput: EditText
    private lateinit var btnShiftAction: MaterialButton

    private var activeShift: ShiftEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shift)

        findViewById<View>(R.id.btnBackShift).setOnClickListener { finish() }

        tvStatusTitle = findViewById(R.id.tvShiftStatusTitle)
        tvStartTime = findViewById(R.id.tvShiftStartTime)
        tvOpeningCash = findViewById(R.id.tvShiftOpeningCash)
        tvActionPrompt = findViewById(R.id.tvShiftActionPrompt)
        etCashInput = findViewById(R.id.etShiftCashInput)
        btnShiftAction = findViewById(R.id.btnShiftAction)

        checkCurrentShift()
    }

    private fun checkCurrentShift() {
        lifecycleScope.launch {
            activeShift = withContext(Dispatchers.IO) {
                db.shiftDao().getActiveShift()
            }

            if (activeShift != null) {
                // Ada shift yang sedang berjalan -> Opsi Tutup Shift
                val shift = activeShift!!
                tvStatusTitle.text = "Status: SHIFT KASIR AKTIF"
                tvStatusTitle.setTextColor(0xFF2E7D32.toInt())
                tvStartTime.text = "Dimulai: ${timeFormat.format(Date(shift.startTime))}"
                tvOpeningCash.text = "Modal Kas Awal: ${currencyFormatter.format(shift.openingCash)}"

                tvActionPrompt.text = "Hitung Uang Kas Akhir di Laci (Tutup Shift):"
                etCashInput.hint = "Total uang tunai di laci kasir"
                btnShiftAction.text = "Tutup Shift & Rekonsiliasi"
                btnShiftAction.setOnClickListener { closeActiveShift() }
            } else {
                // Tidak ada shift aktif -> Opsi Buka Shift Baru
                tvStatusTitle.text = "Status: SHIFT BELUM DIBUKA"
                tvStatusTitle.setTextColor(0xFFE65100.toInt())
                tvStartTime.text = "Silakan buka shift kasir sebelum memulai transaksi."
                tvOpeningCash.text = "Modal Kas Awal: Rp 0"

                tvActionPrompt.text = "Masukkan Modal Kas Awal (Kembalian di Laci):"
                etCashInput.hint = "Contoh: 100000"
                btnShiftAction.text = "Buka Shift Kasir Baru"
                btnShiftAction.setOnClickListener { openNewShift() }
            }
        }
    }

    private fun openNewShift() {
        val openingAmount = etCashInput.text.toString().toDoubleOrNull() ?: 0.0
        val newShift = ShiftEntity(
            id = UUID.randomUUID().toString(),
            storeId = "default_store",
            cashierUid = "owner_kasir",
            cashierName = "Kasir ML Juice",
            startTime = System.currentTimeMillis(),
            openingCash = openingAmount,
            status = "OPEN"
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.shiftDao().insertShift(newShift)
            }
            Toast.makeText(this@ShiftActivity, "Shift kasir berhasil dibuka dengan modal ${currencyFormatter.format(openingAmount)}", Toast.LENGTH_SHORT).show()
            etCashInput.text.clear()
            checkCurrentShift()
        }
    }

    private fun closeActiveShift() {
        val shift = activeShift ?: return
        val actualClosingCash = etCashInput.text.toString().toDoubleOrNull() ?: 0.0

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.shiftDao().closeShift(
                    shiftId = shift.id,
                    endTime = System.currentTimeMillis(),
                    closingCash = actualClosingCash,
                    cashSales = 0.0
                )
            }
            Toast.makeText(this@ShiftActivity, "Shift kasir berhasil ditutup!", Toast.LENGTH_SHORT).show()
            etCashInput.text.clear()
            checkCurrentShift()
        }
    }
}
