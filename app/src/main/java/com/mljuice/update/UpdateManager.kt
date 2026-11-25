package com.mljuice.update

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.mljuice.BuildConfig
import com.mljuice.R
import java.io.File

object UpdateManager {

    fun checkForUpdate(context: Context) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("app_config").document("latest_version")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val latestCode = doc.getLong("versionCode") ?: 0
                val versionName = doc.getString("versionName") ?: "Terbaru"
                val downloadUrl = doc.getString("downloadUrl")
                val releaseNotes = doc.getString("releaseNotes") ?: "Peningkatan performa & perbaikan bug."
                val forceUpdate = doc.getBoolean("forceUpdate") ?: false

                if (latestCode > BuildConfig.VERSION_CODE && !downloadUrl.isNullOrEmpty()) {
                    showUpdateDialog(context, versionName, downloadUrl, releaseNotes, forceUpdate)
                }
            }
            .addOnFailureListener {
                // Silently ignore if offline
            }
    }

    private fun showUpdateDialog(
        context: Context,
        versionName: String,
        downloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean
    ) {
        val builder = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_update_title) + " (v$versionName)")
            .setMessage("$releaseNotes\n\nApakah Anda ingin memperbarui sekarang?")
            .setCancelable(!forceUpdate)
            .setPositiveButton(context.getString(R.string.btn_update_now)) { _, _ ->
                downloadAndInstallApk(context, downloadUrl, versionName)
            }

        if (!forceUpdate) {
            builder.setNegativeButton(context.getString(R.string.btn_update_later), null)
        }

        builder.show()
    }

    private fun downloadAndInstallApk(context: Context, url: String, version: String) {
        Toast.makeText(context, "Mengunduh pembaruan ML Juice...", Toast.LENGTH_SHORT).show()

        val fileName = "mljuice-v$version.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("ML Juice POS Update v$version")
            .setDescription("Mengunduh berkas instalasi terbaru")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try {
                        ctxt.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    installApk(ctxt, destination)
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
