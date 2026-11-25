package com.mljuice

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mljuice.data.local.db.MLJuiceDatabase

class MLJuiceApp : Application() {

    lateinit var database: MLJuiceDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Inisialisasi Firebase
        FirebaseApp.initializeApp(this)

        // 2. Aktifkan Offline Persistence untuk Cloud Firestore
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // 3. Inisialisasi Room Database lokal
        database = MLJuiceDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: MLJuiceApp
            private set
    }
}
