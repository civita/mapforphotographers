package com.cs386p.mapforphotographers

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.cs386p.mapforphotographers.model.PhotoMeta
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ViewModelDBHelper() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val rootCollection = "allPhotos"

    fun fetchPhotoMeta(uid: String,
                       isViewingLiked: Boolean,
                       sortInfo: SortInfo,
                       notesList: MutableLiveData<List<PhotoMeta>>
    ) {
        dbFetchPhotoMeta(uid, isViewingLiked, sortInfo, notesList)
    }
    // If we want to listen for real time updates use this
    // .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
    // But be careful about how listener updates live data
    // and noteListener?.remove() in onCleared
    private fun limitAndGet(query: Query,
                            notesList: MutableLiveData<List<PhotoMeta>>
    ) {
        query
            .limit(100)
            .get()
            .addOnSuccessListener { result ->
                Log.d(javaClass.simpleName, "allPhotos fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                notesList.postValue(result.documents.mapNotNull {
                    it.toObject(PhotoMeta::class.java)
                })
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "allPhotos fetch FAILED ", it)
            }
    }
    /////////////////////////////////////////////////////////////
    // Interact with Firestore db
    // https://firebase.google.com/docs/firestore/query-data/order-limit-data
    private fun dbFetchPhotoMeta(uid: String,
                                 isViewingLiked: Boolean,
                                 sortInfo: SortInfo,
                                 notesList: MutableLiveData<List<PhotoMeta>>
    ) {
        // XXX Write me and use limitAndGet
        //todo sortInfo
//        val orderByName = when (sortInfo.sortColumn) {
//            SortColumn.TITLE -> "pictureTitle"
//            SortColumn.SIZE -> "byteSize"
//        }
//        if (sortInfo.ascending) {
//            limitAndGet(db.collection(rootCollection).orderBy(orderByName, Query.Direction.ASCENDING), notesList)
//        } else {
//            limitAndGet(db.collection(rootCollection).orderBy(orderByName, Query.Direction.DESCENDING), notesList)
//        }
        if(isViewingLiked) {
            limitAndGet(db.collection(rootCollection).whereArrayContains("likedBy", uid), notesList)
        } else {
            limitAndGet(db.collection(rootCollection), notesList)
        }
    }

    fun dbFetchPhotoCount(uid: String, photoCount: MutableLiveData<Int>) {
        db.collection(rootCollection)
            .whereEqualTo("ownerUid", uid)
            .get()
            .addOnSuccessListener { result ->
                photoCount.postValue(result!!.documents.size)
            }
            .addOnFailureListener {
                photoCount.postValue(0)
            }
    }

    fun dbFetchPhotoLikedCount(uid: String, photoLikedCount: MutableLiveData<Int>) {
        db.collection(rootCollection)
            .whereArrayContains("likedBy", uid)
            .get()
            .addOnSuccessListener { result ->
                photoLikedCount.postValue(result!!.documents.size)
            }
            .addOnFailureListener {
                photoLikedCount.postValue(0)
            }
    }

    fun dbLikeOnePhoto(uuid: String, uid: String) {
        db.collection(rootCollection)
            .limit(1)
            .whereEqualTo("uuid", uuid)
            .get()
            .addOnSuccessListener { result ->
                var docRef = db.collection(rootCollection).document(result.first().id)
                docRef.update("likedBy", FieldValue.arrayUnion(uid))
            }
            .addOnFailureListener {
            }
    }

    fun dbUnlikeOnePhoto(uuid: String, uid: String) {
        db.collection(rootCollection)
            .limit(1)
            .whereEqualTo("uuid", uuid)
            .get()
            .addOnSuccessListener { result ->
                var docRef = db.collection(rootCollection).document(result.first().id)
                docRef.update("likedBy", FieldValue.arrayRemove(uid))
            }
            .addOnFailureListener {
            }
    }

    // https://firebase.google.com/docs/firestore/manage-data/add-data#add_a_document
    fun createPhotoMeta(
        sortInfo: SortInfo,
        photoMeta: PhotoMeta,
        notesList: MutableLiveData<List<PhotoMeta>>
    ) {
        // You can get a document id if you need it.
        photoMeta.firestoreID = db.collection(rootCollection).document().id
        // XXX Write me: add photoMeta
        db.collection(rootCollection)
            .add(photoMeta)
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "photoMeta create \"${photoMeta.pictureTitle}\" id: ${photoMeta.firestoreID}"
                )
                dbFetchPhotoMeta(photoMeta.ownerUid, false, sortInfo, notesList)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "photoMeta create FAILED \"${photoMeta.pictureTitle}\"")
                Log.w(javaClass.simpleName, "Error ", e)
            }
    }

    // https://firebase.google.com/docs/firestore/manage-data/delete-data#delete_documents
    fun removePhotoMeta(
        sortInfo: SortInfo,
        photoMeta: PhotoMeta,
        photoMetaList: MutableLiveData<List<PhotoMeta>>
    ) {
        // XXX Write me.  Make sure you delete the correct entry
        db.collection(rootCollection)
            .document(photoMeta.firestoreID)
            .delete()
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "photoMeta delete \"${photoMeta.pictureTitle}\" id: ${photoMeta.firestoreID}"
                )
                dbFetchPhotoMeta(photoMeta.ownerUid, false, sortInfo, photoMetaList)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "photoMeta deleting FAILED \"${photoMeta.pictureTitle}\"")
                Log.w(javaClass.simpleName, "Error adding document", e)
            }
    }
}