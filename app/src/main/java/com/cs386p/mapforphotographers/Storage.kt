package com.cs386p.mapforphotographers

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.io.File

// Store files in firebase storage
class Storage {
    // Create a storage reference from our app
    private val photoStorage: StorageReference =
        FirebaseStorage.getInstance().reference.child("images")

    // https://firebase.google.com/docs/storage/android/upload-files#upload_from_a_local_file
    fun uploadImage(uri: Uri, uuid: String, uploadSuccess:(Long)->Unit) {
        // XXX Write me
        //val file = Uri.fromFile(localFile)
        val uuidRef = photoStorage.child(uuid)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpg")
            .build()
        val uploadTask = uuidRef.putFile(uri, metadata)

        // Register observers to listen for when the download is done or if it fails
        uploadTask
            .addOnFailureListener {
                // Handle unsuccessful uploads
                Log.d(javaClass.simpleName, "Upload FAILED $uuid")
//                if(localFile.delete()) {
//                    Log.d(javaClass.simpleName, "Upload FAILED $uuid, file deleted")
//                } else {
//                    Log.d(javaClass.simpleName, "Upload FAILED $uuid, file delete FAILED")
//                }
            }
            .addOnSuccessListener {
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                val sizeBytes = it.metadata?.sizeBytes ?: -1
                uploadSuccess(sizeBytes)
                Log.d(javaClass.simpleName, "Upload succeeded $uuid")
//                if(localFile.delete()) {
//                    Log.d(javaClass.simpleName, "Upload succeeded $uuid, file deleted")
//                } else {
//                    Log.d(javaClass.simpleName, "Upload succeeded $uuid, file delete FAILED")
//                }
            }
    }
    // https://firebase.google.com/docs/storage/android/delete-files#delete_a_file
    fun deleteImage(pictureUUID: String) {
        // Delete the file
        // XXX Write me
        photoStorage.child(pictureUUID).delete()
            .addOnSuccessListener {
                Log.d(javaClass.simpleName, "Deleted $pictureUUID")
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Delete FAILED of $pictureUUID")
            }
    }

    fun uuid2StorageReference(uuid: String): StorageReference {
        return photoStorage.child(uuid)
    }
}