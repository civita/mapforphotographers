package com.cs386p.mapforphotographers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.ContactsContract.Contacts.Photo
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cs386p.mapforphotographers.model.PhotoMeta
import com.cs386p.mapforphotographers.ui.profile.ProfileFragment
import com.cs386p.mapforphotographers.glide.Glide
import java.util.*
import kotlin.reflect.full.memberProperties

class PhotoViewModel() : ViewModel() {
    // LiveData for entire note list, all images
    private val photoMetaList = MutableLiveData<List<PhotoMeta>>().apply {
        value = listOf()
    }
    // Firestore state
    private val storage = Storage()
    private var firebaseAuthLiveData = FirestoreAuthLiveData()
    // Database access
    private val dbHelp = ViewModelDBHelper()

    private var photoCount = MutableLiveData<Int>()
    private var photoLikedCount = MutableLiveData<Int>()
    private var isViewingLiked = MutableLiveData<Boolean>().apply {
        value = false
    }

    /////////////////////////////////////////////////////////////
    // Notes, memory cache and database interaction
    fun fetchPhotoMeta() {
        val currentUser = firebaseAuthLiveData.getCurrentUser()
        if (currentUser != null && currentUser.uid.isNotEmpty()) {
            if (isViewingLiked.value == true) {
                dbHelp.fetchPhotoMeta(currentUser.uid, true, photoMetaList)
            } else {
                dbHelp.fetchPhotoMeta(currentUser.uid, false, photoMetaList)
            }
        }
    }

    fun fetchPublicPhotoMeta() {
        dbHelp.fetchPhotoMeta("", false, photoMetaList)
    }

    fun observePhotoMeta(): LiveData<List<PhotoMeta>> {
        return photoMetaList
    }

    fun observePhotoCount(): LiveData<Int> {
        return photoCount
    }

    fun observeIsViewingLiked(): LiveData<Boolean> {
        return isViewingLiked
    }

    fun updateIsViewingLiked(value: Boolean) {
        isViewingLiked.postValue(value)
    }

    fun toggleIsViewingLiked() {
        isViewingLiked.postValue(!isViewingLiked.value!!)
    }

    fun signOut() {
        photoMetaList.postValue(listOf())
        photoCount.postValue(0)
        photoLikedCount.postValue(0)
        isViewingLiked.postValue(false)
    }

    fun observerPhotoLikedCount(): LiveData<Int> {
        return photoLikedCount
    }

    fun fetchPhotoCount(uid: String) {
        dbHelp.dbFetchPhotoCount(uid, photoCount)
    }

    fun fetchPhotoLikedCount(uid: String) {
        dbHelp.dbFetchPhotoLikedCount(uid, photoLikedCount)
    }

    fun removePhoto(photoMeta: PhotoMeta) {
        storage.deleteImage(photoMeta.uuid)
        dbHelp.removePhotoMeta(photoMeta, photoMetaList)
    }

    fun createPhotoMeta(photoMeta: PhotoMeta, uuid : String) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        photoMeta.ownerName = currentUser.displayName ?: "Anonymous user"
        photoMeta.ownerUid = currentUser.uid
        photoMeta.uuid = uuid
        dbHelp.createPhotoMeta(photoMeta, photoMetaList)
    }

    fun likeOnePhoto(uuid: String) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        dbHelp.dbLikeOnePhoto(uuid, currentUser.uid)
    }

    fun unlikeOnePhoto(uuid: String) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        dbHelp.dbUnlikeOnePhoto(uuid, currentUser.uid)
    }

    fun glideFetch(uuid: String, imageView: ImageView) {
        Glide.fetch(storage.uuid2StorageReference(uuid), imageView)
    }

    fun glideFetchFull(uuid: String, imageView: ImageView) {
        Glide.fetchFull(storage.uuid2StorageReference(uuid), imageView)
    }

    fun glideFetch(uuid: String, context: Context, zoom: Float): Bitmap {
        return Glide.fetch(storage.uuid2StorageReference(uuid), context, zoom)
    }

    // Convenient place to put it as it is shared
    companion object {
        fun doOnePhoto(context: Context, data: Uri) {
            val onePhotoIntent = Intent(context, OnePhoto::class.java)
            onePhotoIntent.putExtra("""uri""", data.toString())
            context.startActivity(onePhotoIntent)
        }
        fun doOnePhotoViewing(context: Context, photometa: PhotoMeta) {
            //viewModel.fetchPhotoMeta()
            val onePhotoIntent = Intent(context, OnePhotoViewing::class.java)
            onePhotoIntent.putExtra("""photoMeta""", photometa)
            context.startActivity(onePhotoIntent)
        }
    }

    // Search function
    private var searchTerm = MutableLiveData<String>().apply {
        value = ""
    }

    fun setSearchTerm(s: String) {
        searchTerm.value = s
    }

    fun searchTermRefresh() {
        Log.d("xxx_searchTerm", "refreshed")
        val fetch = searchTerm.value
        searchTerm.value = fetch
    }

    private fun search(fulltext: String?, subtext: String): Boolean {
        if( subtext.isEmpty() ) return true
        val i = fulltext?.indexOf(subtext, ignoreCase = true)
        if( i == -1 ) return false
        return true
    }

    private fun filterList(): List<PhotoMeta> {
        // We know value is not null
        val searchTermValue = searchTerm.value!!
        return if (photoMetaList.value.isNullOrEmpty()) {
            listOf()
        } else {
            photoMetaList.value!!.filter {
                var found = false
                for (property in PhotoMeta::class.memberProperties) {
                    if(property.name.contains("picture") || property.name.contains("ownerName")) {
                        if (search(property.get(it)?.toString(), searchTermValue)) {
                            Log.d("xxx_photoviewmodel", property.get(it)?.toString()?: "...")
                            found = true
                        }
                    }
                }
                found
            }
        }
    }

    private var livePhotoMetaList = MediatorLiveData<List<PhotoMeta>>().apply {
        addSource(searchTerm) {value = filterList()}
        addSource(photoMetaList) {value = photoMetaList.value}
    }

    fun observeLivePhotoMeta(): LiveData<List<PhotoMeta>> {
        return livePhotoMetaList
    }
}
