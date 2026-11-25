package com.mljuice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.mljuice.R
import com.mljuice.ui.pos.PosActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Silakan isi email dan kata sandi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            lifecycleScope.launch {
                try {
                    auth.signInWithEmailAndPassword(email, password).await()
                    Toast.makeText(this@LoginActivity, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, PosActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Login gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                }
            }
        }
    }
}
