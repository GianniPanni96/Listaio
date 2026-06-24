package com.listaio

import android.app.Application
import com.google.firebase.auth.FirebaseAuth

class ListaioApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Kick off anonymous sign-in early so auth is ready before the first Firestore write.
        // Firebase auto-initializes via google-services.json; we only need to ensure a user exists.
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}
