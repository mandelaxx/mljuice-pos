package com.mljuice.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.mljuice.data.local.entity.SaleEntity
import com.mljuice.data.local.entity.SaleItemEntity
import com.mljuice.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BluetoothPrinterHelper {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

    fun getPairedPrinters(): List<BluetoothConnection> {
        val bluetoothPrinters = BluetoothPrintersConnections()
        return bluetoothPrinters.list?.toList() ?: emptyList()
    }

    suspend fun printCustomerReceipt(
        context: Context,
        profile: StoreProfileEntity?,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        paperWidthMm: Float = 48f, // 58mm printer has 48mm printable width
        charactersPerLine: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = BluetoothPrintersConnections.selectFirstPaired()
            if (connection == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Tidak ada printer Bluetooth yang terhubung!", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            val storeName = profile?.storeName ?: "ML JUICE"
            val storeAddress = profile?.address ?: "Pusat Minuman Segar & Sehat"
            val storePhone = profile?.phone ?: ""

            val printer = EscPosPrinter(connection, 203, paperWidthMm, charactersPerLine)

            val receiptText = buildString {
                append("[C]<b><font size='big'>$storeName</font></b>\n")
                append("[C]$storeAddress\n")
                if (storePhone.isNotEmpty()) append("[C]Telp: $storePhone\n")
                append("[C]--------------------------------\n")
                append("[L]<b>ANTRIAN: #${sale.queueNumber}</b>\n")
                append("[L]No: ${sale.transactionNumber}\n")
                append("[L]Waktu: ${dateFormatter.format(Date(sale.timestamp))}\n")
                append("[L]Kasir: ${sale.cashierName}\n")
                append("[C]--------------------------------\n")

                for (item in items) {
                    val variantPart = if (!item.variantName.isNullOrEmpty()) " (${item.variantName})" else ""
                    append("[L]<b>${item.productName}$variantPart</b>\n")
                    if (!item.modifierNotes.isNullOrEmpty()) {
                        append("[L]  * ${item.modifierNotes}\n")
                    }
                    val formattedPrice = currencyFormatter.format(item.price)
                    val formattedTotal = currencyFormatter.format(item.itemTotal)
                    append("[L]  ${item.quantity.toInt()}x @$formattedPrice [R]$formattedTotal\n")
                }

                append("[C]--------------------------------\n")
                append("[L]Subtotal: [R]${currencyFormatter.format(sale.subtotalGross)}\n")
                if (sale.discountAmount > 0) {
                    append("[L]Diskon: [R]-${currencyFormatter.format(sale.discountAmount)}\n")
                }
                append("[L]<b>TOTAL: [R]<font size='big'>${currencyFormatter.format(sale.finalTotal)}</font></b>\n")
                append("[L]Bayar (${sale.paymentType}): [R]${currencyFormatter.format(sale.amountPaid)}\n")
                append("[L]Kembalian: [R]<b>${currencyFormatter.format(sale.changeAmount)}</b>\n")
                append("[C]--------------------------------\n")
                append("[C]Terima kasih atas pesanan Anda!\n")
                append("[C]Nikmati kesegaran buah asli setiap hari.\n\n\n")
            }

            printer.printFormattedTextAndCut(receiptText)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal mencetak struk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    suspend fun printKitchenTicket(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        charactersPerLine: Int = 32
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = BluetoothPrintersConnections.selectFirstPaired() ?: return@withContext false
            val printer = EscPosPrinter(connection, 203, 48f, charactersPerLine)

            val ticketText = buildString {
                append("[C]<b><font size='big'>TIKET BARISTA / DAPUR</font></b>\n")
                append("[C]================================\n")
                append("[C]<b><font size='big'>#${sale.queueNumber}</font></b>\n")
                append("[L]Order: ${sale.transactionNumber}\n")
                append("[L]Waktu: ${dateFormatter.format(Date(sale.timestamp))}\n")
                if (!sale.notes.isNullOrEmpty()) {
                    append("[L]<b>Catatan: ${sale.notes}</b>\n")
                }
                append("[C]--------------------------------\n")

                for (item in items) {
                    append("[L]<b>${item.quantity.toInt()}x ${item.productName}</b>\n")
                    if (!item.variantName.isNullOrEmpty()) {
                        append("[L]  Ukuran: ${item.variantName}\n")
                    }
                    if (!item.modifierNotes.isNullOrEmpty()) {
                        append("[L]  Rasa: ${item.modifierNotes}\n")
                    }
                    append("[L]  ---\n")
                }

                append("[C]================================\n\n\n")
            }

            printer.printFormattedTextAndCut(ticketText)
            true
        } catch (_: Exception) {
            false
        }
    }
}
