package com.mljuice.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mljuice.MLJuiceApp
import com.mljuice.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private val db by lazy { MLJuiceApp.instance.database }
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTxCount: TextView
    private lateinit var tvCash: TextView
    private lateinit var tvNonCash: TextView
    private lateinit var tvBestSellers: TextView

    private var reportSummaryText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        findViewById<View>(R.id.btnBackReport).setOnClickListener { finish() }
        tvTotalRevenue = findViewById(R.id.tvReportTotalRevenue)
        tvTxCount = findViewById(R.id.tvReportTxCount)
        tvCash = findViewById(R.id.tvReportCash)
        tvNonCash = findViewById(R.id.tvReportNonCash)
        tvBestSellers = findViewById(R.id.tvBestSellersList)

        findViewById<View>(R.id.btnShareReport).setOnClickListener {
            shareViaWhatsApp(reportSummaryText)
        }

        loadTodayReport()
    }

    private fun loadTodayReport() {
        // Ambil rentang waktu awal hari ini (00:00:00) sampai akhir hari ini (23:59:59)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endTime = cal.timeInMillis

        lifecycleScope.launch {
            val summary = withContext(Dispatchers.IO) {
                db.saleDao().getSalesSummary(startTime, endTime)
            }
            val bestSellers = withContext(Dispatchers.IO) {
                db.saleDao().getBestSellers(startTime, endTime)
            }

            tvTotalRevenue.text = currencyFormatter.format(summary.totalRevenue)
            tvTxCount.text = "${summary.totalTransactions} Transaksi Selesai"
            tvCash.text = currencyFormatter.format(summary.totalCash)
            tvNonCash.text = currencyFormatter.format(summary.totalNonCash)

            if (bestSellers.isNotEmpty()) {
                val listText = bestSellers.mapIndexed { idx, item ->
                    "${idx + 1}. ${item.productName} — ${item.totalQty.toInt()} cup (${currencyFormatter.format(item.totalRevenue)})"
                }.joinToString("\n")
                tvBestSellers.text = listText
            } else {
                tvBestSellers.text = "Belum ada transaksi minuman yang tercatat hari ini."
            }

            reportSummaryText = buildString {
                append("🧃 *REKAP PENJUALAN ML JUICE HARI INI*\n")
                append("Total Omset: ${currencyFormatter.format(summary.totalRevenue)}\n")
                append("Jumlah Transaksi: ${summary.totalTransactions}\n")
                append("Tunai: ${currencyFormatter.format(summary.totalCash)}\n")
                append("Non-Tunai: ${currencyFormatter.format(summary.totalNonCash)}\n\n")
                append("🏆 *Menu Terlaris:*\n")
                if (bestSellers.isNotEmpty()) {
                    bestSellers.forEachIndexed { i, it ->
                        append("${i + 1}. ${it.productName} (${it.totalQty.toInt()} cup)\n")
                    }
                } else {
                    append("Belum ada data\n")
                }
            }
        }
    }

    private fun shareViaWhatsApp(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Bagikan Rekap Penjualan"))
    }
}
