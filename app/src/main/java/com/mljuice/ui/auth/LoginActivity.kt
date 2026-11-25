package com.mljuice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mljuice.R
import com.mljuice.ui.pos.PosActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rgAuthMode: RadioGroup
    private lateinit var rbModeLogin: RadioButton
    private lateinit var rbModeRegister: RadioButton
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSubmitAuth: MaterialButton
    private lateinit var btnGuestMode: MaterialButton

    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        rgAuthMode = findViewById(R.id.rgAuthMode)
        rbModeLogin = findViewById(R.id.rbModeLogin)
        rbModeRegister = findViewById(R.id.rbModeRegister)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth)
        btnGuestMode = findViewById(R.id.btnGuestMode)
    }

    private fun setupListeners() {
        rgAuthMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbModeRegister) {
                isRegisterMode = true
                etName.visibility = View.VISIBLE
                btnSubmitAuth.text = "Daftar Akun Baru & Masuk"
            } else {
                isRegisterMode = false
                etName.visibility = View.GONE
                btnSubmitAuth.text = "Masuk ke Kasir"
            }
        }

        btnSubmitAuth.setOnClickListener {
            handleAuthSubmit()
        }

        btnGuestMode.setOnClickListener {
            Toast.makeText(this, "Masuk sebagai Kasir Tamu (Mode Offline)", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PosActivity::class.java))
            finish()
        }
    }

    private fun handleAuthSubmit() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val name = etName.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email wajib diisi"
            return
        }
        if (password.length < 6) {
            etPassword.error = "Kata sandi minimal 6 karakter"
            return
        }

        btnSubmitAuth.isEnabled = false
        btnSubmitAuth.text = "Memproses..."

        lifecycleScope.launch {
            try {
                if (isRegisterMode) {
                    // 1. Buat akun baru di Firebase Auth
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = result.user?.uid ?: "unknown_uid"

                    // 2. Simpan data profil ke Firestore
                    val userProfile = hashMapOf(
                        "uid" to uid,
                        "name" to if (name.isEmpty()) "Owner ML Juice" else name,
                        "email" to email,
                        "role" to "OWNER",
                        "storeId" to "default_store",
                        "createdAt" to System.currentTimeMillis()
                    )
                    try {
                        firestore.collection("users").document(uid).set(userProfile).await()
                    } catch (_: Exception) {}

                    Toast.makeText(this@LoginActivity, "Akun berhasil didaftarkan! Selamat datang di ML Juice.", Toast.LENGTH_LONG).show()
                } else {
                    // Login ke akun yang sudah ada
                    auth.signInWithEmailAndPassword(email, password).await()
                    Toast.makeText(this@LoginActivity, "Login berhasil!", Toast.LENGTH_SHORT).show()
                }

                startActivity(Intent(this@LoginActivity, PosActivity::class.java))
                finish()
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("The email address is already in use") == true ->
                        "Email ini sudah terdaftar! Silakan pilih mode 'Masuk (Login)'."
                    e.message?.contains("The email address is badly formatted") == true ->
                        "Format email tidak valid. Contoh: nama@gmail.com"
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true || e.message?.contains("wrong password") == true ->
                        "Email atau kata sandi salah. Jika belum punya akun, pilih tab 'Daftar Baru'."
                    else -> "Gagal: ${e.localizedMessage}"
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                btnSubmitAuth.isEnabled = true
                btnSubmitAuth.text = if (isRegisterMode) "Daftar Akun Baru & Masuk" else "Masuk ke Kasir"
            }
        }
    }
}
