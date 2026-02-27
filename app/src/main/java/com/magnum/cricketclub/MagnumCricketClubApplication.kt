package com.magnum.cricketclub

import android.app.Application

class MagnumCricketClubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase only if available
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase not configured yet - app will work in offline mode
            android.util.Log.d("MagnumCricketClub", "Firebase not configured: ${e.message}")
        }
    }
}
