package com.cs386p.mapforphotographers.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth


class ProfileViewModel : ViewModel() {

    private var defaultDisplayName = "please login first!"

    private val _text = MutableLiveData<String>().apply {
        value = "This is profile Fragment"
    }
    val text: LiveData<String> = _text


    private var loginButtonText = MutableLiveData("Login") // default = login

    fun observeLoginButtonText() : LiveData<String> {
        return loginButtonText
    }

    // copied from fc7
    private var displayName = MutableLiveData(defaultDisplayName)
    private var email = MutableLiveData("Uninitialized")
    private var uid = MutableLiveData("Uninitialized")

    private fun userLogout() {
        displayName.postValue(defaultDisplayName)
        email.postValue("No email, no active user")
        uid.postValue("No uid, no active user")
        loginButtonText.postValue("Login")
    }

    fun updateUser() {
        // XXX Write me. Update user data in view model
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null) {
            displayName.postValue(user.displayName)
            email.postValue(user.email)
            uid.postValue(user.uid)
            loginButtonText.postValue("Logout")
        }
    }

    fun observeDisplayName() : LiveData<String> {
        return displayName
    }
    fun observeEmail() : LiveData<String> {
        return email
    }
    fun observeUid() : LiveData<String> {
        return uid
    }
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        userLogout()
    }
}