package com.cs386p.mapforphotographers

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirestoreAuthLiveData : LiveData<FirebaseUser?>() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val authStateListener = FirebaseAuth.AuthStateListener {
        value = firebaseAuth.currentUser
    }

    fun updateUser() {
        value = firebaseAuth.currentUser
    }
    fun getCurrentUser() : FirebaseUser? {
        return firebaseAuth.currentUser
    }
    override fun onActive() {
        super.onActive()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {
        super.onInactive()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}