package com.mljuice.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnGuestMode: MaterialButton

    private lateinit var googleSignInClient: GoogleSignInClient
    private var isRegisterMode = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (!idToken.isNullOrEmpty()) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    firebaseAuthWithGoogle(credential, account.displayName, account.email)
                } else {
                    // Fallback jika idToken null pada mode offline
                    Toast.makeText(this, "Masuk sebagai: ${account?.displayName ?: account?.email}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, PosActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Login Google gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupGoogleSignIn()
        initViews()
        setupListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initViews() {
        rgAuthMode = findViewById(R.id.rgAuthMode)
        rbModeLogin = findViewById(R.id.rbModeLogin)
        rbModeRegister = findViewById(R.id.rbModeRegister)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
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

        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
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
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = result.user?.uid ?: "unknown_uid"

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

    private fun firebaseAuthWithGoogle(credential: AuthCredential, displayName: String?, email: String?) {
        lifecycleScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val uid = result.user?.uid ?: "google_uid"

                val userProfile = hashMapOf(
                    "uid" to uid,
                    "name" to (displayName ?: "Owner ML Juice"),
                    "email" to (email ?: ""),
                    "role" to "OWNER",
                    "storeId" to "default_store",
                    "lastLogin" to System.currentTimeMillis()
                )
                try {
                    firestore.collection("users").document(uid).set(userProfile).await()
                } catch (_: Exception) {}

                Toast.makeText(this@LoginActivity, "Berhasil masuk dengan akun Google!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, PosActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Otentikasi Google Firebase gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
