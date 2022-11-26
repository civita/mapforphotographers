package com.cs386p.mapforphotographers

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.cs386p.mapforphotographers.databinding.ActivityOnePhotoBinding
import com.cs386p.mapforphotographers.model.PhotoMeta
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.*


class OnePhoto: AppCompatActivity() {
    companion object {
        const val titleKey = "title"
        const val selfTextKey = "selfText"
        const val imageURLKey = "imageURL"
        const val thumbnailURLKey = "thumbnailURL"
        const val uriKey = "uri"
    }
    private val viewModel: PhotoViewModel by viewModels()

    private var title : String = """"""
    private var selfText : String = """"""
    private var imageURL : String = """"""
    private var thumbnailURL : String = """"""
    private var uri : String = """"""
    private val storage = Storage()
    private var exif = arrayOf(String())
    private var photometa = PhotoMeta()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityOnePhotoBinding = ActivityOnePhotoBinding.inflate(layoutInflater)
        setContentView(activityOnePhotoBinding.root)
        //setSupportActionBar(activityOnePostBinding.toolbar)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)

        uri = intent.getStringExtra(uriKey).toString()
        Log.d("xxx_onephoto", uri)

        if (!uri.isNullOrEmpty()) {

            activityOnePhotoBinding.onePhotoButtonSubmit.setOnClickListener {
                if(!activityOnePhotoBinding.onePhotoTitle.text.isNullOrBlank()) {
                    //viewModel.pictureSuccess()
//                    val localPhotoFile = File(Uri.parse(uri).toString())

                    val uuid = UUID.randomUUID().toString()
                    storage.uploadImage(Uri.parse(uri), uuid) {
//                        viewModel.createPhotoMeta(activityOnePhotoBinding.onePhotoTitle.text.toString(),
//                            activityOnePhotoBinding.onePhotoDescription.text?.toString(),
//                            exif,
//                            uuid
//                            ))
                    }
                } else {
                    val snack = Snackbar.make(it,"Please provide a title!", Snackbar.LENGTH_LONG)
                    snack.show()
                }
            }

            activityOnePhotoBinding.onePhotoImage.setImageURI(Uri.parse(uri))

            val sIn: InputStream?
            try {
                sIn = contentResolver.openInputStream(Uri.parse(uri))
                val exifInterface = sIn?.let { ExifInterface(it) }
                if (exifInterface != null) {



                    Log.d("xxx_onephoto", exifInterface.toString())
                    photometa.pictureDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME).toString()
                    activityOnePhotoBinding.onePhotoTime.text = photometa.pictureDate

                    photometa.pictureCamera = exifInterface.getAttribute(ExifInterface.TAG_MODEL).toString()
                    activityOnePhotoBinding.onePhotoCamera.text = photometa.pictureCamera

                    photometa.pictureLens = exifInterface.getAttribute(ExifInterface.TAG_LENS_MAKE).toString()
                    activityOnePhotoBinding.onePhotoLens.text = photometa.pictureLens

                    photometa.pictureShutterSpeed = exifInterface.getAttributeDouble(ExifInterface.TAG_SHUTTER_SPEED_VALUE, 0.00).toString()
                    activityOnePhotoBinding.onePhotoShutterSpeed.text = photometa.pictureShutterSpeed

                    photometa.pictureFocalLength = exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.00).toString()
                    activityOnePhotoBinding.onePhotoFocalLength.text = photometa.pictureFocalLength

                    photometa.pictureAperture = exifInterface.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.00).toString()
                    activityOnePhotoBinding.onePhotoAperture.text = photometa.pictureAperture

                    photometa.pictureIso = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED).toString() // todo: null?
                    activityOnePhotoBinding.onePhotoIso.text = photometa.pictureIso

                    photometa.pictureLat = exifInterface.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, 0.00).toString()
                    activityOnePhotoBinding.onePhotoLat.text = photometa.pictureLat // todo :covert

                    photometa.pictureLng = exifInterface.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, 0.00).toString()
                    activityOnePhotoBinding.onePhotoLng.text = photometa.pictureLng


                }
                // Extract the EXIF tag here

                try {
                    sIn?.close()
                } catch (ignored: IOException) {
                }

            } catch (e: IOException) {
                // Handle any errors
            }
        }
//        title = intent.getStringExtra(titleKey).toString()
//        selfText = intent.getStringExtra(selfTextKey).toString()
//        imageURL = intent.getStringExtra(imageURLKey).toString()
//        thumbnailURL = intent.getStringExtra(thumbnailURLKey).toString()
//        if(title.length > 30) {
//            title = title.subSequence(0, 30).toString() + "..."
//        }
//        Log.d("xxx_onepost", selfText)
//
//        supportActionBar?.title = title
//        activityOnePostBinding.toolbar.title = title
//        activityOnePostBinding.selfText.text = selfText
//        activityOnePostBinding.selfText.movementMethod = ScrollingMovementMethod()
        //Glide.glideFetch(imageURL, thumbnailURL, activityOnePostBinding.selfImage)

//        addMenuProvider(object : MenuProvider {
//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                // Inflate the menu; this adds items to the action bar if it is present.
//                menuInflater.inflate(R.menu.menu_main, menu)
//            }
//            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
//                // Handle action bar item clicks here.
//                return when (menuItem.itemId) {
//                    android.R.id.home -> {finish(); true} // Handle in fragment
//                    else -> false
//                }
//            }
//        })
    }
}