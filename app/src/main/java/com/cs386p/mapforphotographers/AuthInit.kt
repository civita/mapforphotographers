package com.cs386p.mapforphotographers

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.cs386p.mapforphotographers.ui.profile.ProfileViewModel
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest


// From fc6
// https://firebase.google.com/docs/auth/android/firebaseui
class AuthInit(viewModel: ProfileViewModel, signInLauncher: ActivityResultLauncher<Intent>) {
    companion object {
        private const val TAG = "AuthInit"
        fun setDisplayName(displayName : String, viewModel: ProfileViewModel) {
            Log.d(TAG, "XXX profile change request")
            // XXX Write me. User is attempting to update display name. Get the profile updates (see android doc)
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "XXX User profile updated.")
                            viewModel.updateUser()
                        }
                    }
            }
        }
    }

    init {
        val user = FirebaseAuth.getInstance().currentUser
        if(user == null) {
            Log.d(TAG, "XXX user null")
            // Choose authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build())

            // Create and launch sign-in intent
            // XXX Write me. Set authentication providers and start sign-in for user
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.Theme_MapForPhotographers)
                .build()
            signInLauncher.launch(signInIntent)
        } else {
            Log.d(TAG, "XXX user ${user.displayName} email ${user.email}")
            viewModel.updateUser()
        }
    }
}
