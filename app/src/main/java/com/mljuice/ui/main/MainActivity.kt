package com.mljuice.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.mljuice.ui.auth.LoginActivity
import com.mljuice.ui.pos.PosActivity
import com.mljuice.update.UpdateManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cek in-app auto-update dari Firebase Firestore
        UpdateManager.checkForUpdate(this)

        // 2. Cek sesi login Firebase Authentication
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // Sudah login -> Langsung ke Layar Kasir POS Minuman
            startActivity(Intent(this, PosActivity::class.java))
        } else {
            // Belum login -> Arahkan ke Layar Login Email/Password
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
