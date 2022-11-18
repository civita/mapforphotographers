package com.cs386p.mapforphotographers.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class PhotoMeta(
    // Auth information
    var ownerName: String = "",
    var ownerUid: String = "",
    var uuid : String = "",
    var byteSize : Long = 0L,
    var pictureTitle: String = "",
    // Written on the server
    @ServerTimestamp val timeStamp: Timestamp? = null,
    // firestoreID is generated by firestore, used as primary key
    @DocumentId var firestoreID: String = ""
)
