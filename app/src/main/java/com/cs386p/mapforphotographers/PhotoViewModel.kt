package com.cs386p.mapforphotographers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cs386p.mapforphotographers.model.PhotoMeta
import com.cs386p.mapforphotographers.ui.profile.ProfileFragment
import com.cs386p.mapforphotographers.glide.Glide
import java.util.*

enum class SortColumn {
    TITLE,
    SIZE
}
data class SortInfo(val sortColumn: SortColumn, val ascending: Boolean)
class PhotoViewModel() : ViewModel() {
    // Remember the uuid, and hence file name of file camera will create
    private var pictureUUID: String =""
    // LiveData for entire note list, all images
    private var photoMetaList = MutableLiveData<List<PhotoMeta>>()
    private var sortInfo = MutableLiveData(
        SortInfo(SortColumn.TITLE, true))
    // Firestore state
    private val storage = Storage()
    private var firebaseAuthLiveData = FirestoreAuthLiveData()
    // Database access
    private val dbHelp = ViewModelDBHelper()
    // assert does not work
    private lateinit var crashMe: String

    private var photoCount = MutableLiveData<Int>()
    private var photoLikedCount = MutableLiveData<Int>()

    // NB: Here is a problem with this whole strategy.  It "works" when you use
    // local variables to save these "function pointers."  But the viewModel can be
    // cleared, so we want to save these function pointers that are actually closures
    // with a reference to the activity/fragment that created them.  So we get a
    // parcelable error if we try to store them into a SavedHandleState
    private fun noPhoto(name: String) {
        Log.d(javaClass.simpleName, "Function must be initialized to something that can start the camera intent")
        crashMe.plus(name)
    }
    private var takePhotoIntent: (String) -> Unit = ::noPhoto

    private fun defaultPhoto(@Suppress("UNUSED_PARAMETER") sizeBytes : Long) {
        Log.d(javaClass.simpleName, "Function must be initialized to photo callback" )
        crashMe.plus(" ")
    }
    private var photoSuccess: (sizeBytes : Long) -> Unit = ::defaultPhoto


    /////////////////////////////////////////////////////////////
    // Notes, memory cache and database interaction
    fun fetchPhotoMeta() {
        dbHelp.fetchPhotoMeta(sortInfo.value!!, photoMetaList)
    }
    fun observePhotoMeta(): LiveData<List<PhotoMeta>> {
        return photoMetaList
    }
    fun observeSortInfo(): LiveData<SortInfo> {
        return sortInfo
    }

    fun observerPhotoCount(): LiveData<Int> {
        return photoCount
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

    fun sortInfoClick(sortColumn: SortColumn) {
        // XXX User has changed sort info
        if(sortInfo.value?.sortColumn == sortColumn) {
            // asc -> dec ; dec -> asc
            if(sortInfo.value?.ascending!!) {
                sortInfo.value = SortInfo(sortColumn, false)
            } else {
                sortInfo.value = SortInfo(sortColumn, true)
            }
        } else {
            sortInfo.value = SortInfo(sortColumn, true) // default asc
        }
    }

    fun removePhotoAt(position: Int) {
        // XXX Deletion requires two different operations.  What are they?
        val photoMeta = getPhotoMeta(position)
        storage.deleteImage(photoMeta.uuid)
        //EEE // XXX What do to before we delete note?
        Log.d(javaClass.simpleName, "remote photo at pos: $position id: ${photoMeta.firestoreID}")
        dbHelp.removePhotoMeta(observeSortInfo().value!!, photoMeta, photoMetaList)
    }

    // Get a note from the memory cache
    fun getPhotoMeta(position: Int) : PhotoMeta {
        val note = photoMetaList.value?.get(position)
        return note!!
    }

    fun createPhotoMeta(pictureTitle: String, uuid : String,
                        byteSize : Long) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        val photoMeta = PhotoMeta(
            ownerName = currentUser.displayName ?: "Anonymous user",
            ownerUid = currentUser.uid,
            uuid = uuid,
            byteSize = byteSize,
            pictureTitle = pictureTitle,
        )
        dbHelp.createPhotoMeta(sortInfo.value!!, photoMeta, photoMetaList)
    }

    fun createPhotoMeta(photoMeta: PhotoMeta, uuid : String) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        photoMeta.ownerName = currentUser.displayName ?: "Anonymous user"
        photoMeta.ownerUid = currentUser.uid
        photoMeta.uuid = uuid
        dbHelp.createPhotoMeta(sortInfo.value!!, photoMeta, photoMetaList)
    }

    fun likeOnePhoto(uuid: String) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        dbHelp.dbLikeOnePhoto(uuid, currentUser.uid)
    }

    fun unlikeOnePhoto(uuid: String) {
        val currentUser = firebaseAuthLiveData.getCurrentUser()!!
        dbHelp.dbUnlikeOnePhoto(uuid, currentUser.uid)
    }

    /////////////////////////////////////////////////////////////
    // This is intended to be set once by MainActivity.
    // The bummer is that taking a photo requires startActivityForResult
    // which has to be called from an activity.
    fun setPhotoIntent(_takePhotoIntent: (String) -> Unit) {
        takePhotoIntent = _takePhotoIntent
    }

    /////////////////////////////////////////////////////////////
    // Get callback for when camera intent returns.
    // Send intent to take picture
    fun takePhoto(uuid: String, _photoSuccess: (Long) -> Unit) {
        photoSuccess = _photoSuccess
        takePhotoIntent(uuid)
        // Have to remember this in the view model because
        // MainActivity can't remember it without savedInstanceState
        // crap.
        pictureUUID = uuid
    }

    /////////////////////////////////////////////////////////////
    // Callbacks from MainActivity.getResultForActivity from camera intent
    // We can't just schedule the file upload and return.
    // The problem is that our previous picture uploads can still be pending.
    // So a note can have a pictureName that does not refer to an existing file.
    // That violates referential integrity, which we really like in our db (and programming
    // model).
    // So we do not add the pictureName to the note until the picture finishes uploading.
    // That means a user won't see their picture updates immediately, they have to
    // wait for some interaction with the server.
    // You could imagine dealing with this somehow using local files while waiting for
    // a server interaction, but that seems error prone.
    // Freezing the app during an upload also seems bad.
    fun pictureSuccess() {
        val photoFile = ProfileFragment.localPhotoFile(pictureUUID)
        // XXX Write me while preserving referential integrity
        // After calling photoSuccess, reset its value to ::defaultPhoto
//        storage.uploadImage(photoFile, pictureUUID) {
//            photoSuccess(photoFile.length())
//            photoSuccess = ::defaultPhoto
//            pictureUUID = ""
//        }
    }
    fun pictureFailure() {
        // Note, the camera intent will only create the file if the user hits accept
        // so I've never seen this called
        pictureUUID = ""
    }

    /////////////////////////////////////////////////////////////
    fun updateUser() {
        firebaseAuthLiveData.updateUser()
    }

    fun glideFetch(uuid: String, imageView: ImageView) {
        Glide.fetch(storage.uuid2StorageReference(uuid),
            imageView)
    }

    // Convenient place to put it as it is shared
    companion object {
        fun doOnePhoto(context: Context, data: Uri, viewModel: PhotoViewModel) {
            val onePhotoIntent = Intent(context, OnePhoto::class.java)
            onePhotoIntent.putExtra("""uri""", data.toString())
//            onePostIntent.putExtra("""title""", redditPost.title.toString())
//            onePostIntent.putExtra("""selfText""", redditPost.selfText.toString())
//            onePostIntent.putExtra("""imageURL""", redditPost.imageURL)
//            onePostIntent.putExtra("""thumbnailURL""", redditPost.thumbnailURL)
            context.startActivity(onePhotoIntent)
        }
        fun doOnePhotoViewing(context: Context, photometa: PhotoMeta, viewModel: PhotoViewModel) {
            //viewModel.fetchPhotoMeta()
            val onePhotoIntent = Intent(context, OnePhotoViewing::class.java)
            onePhotoIntent.putExtra("""photoMeta""", photometa)
            context.startActivity(onePhotoIntent)
        }
    }
}
