package com.cs386p.mapforphotographers

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.cs386p.mapforphotographers.databinding.ActivityOnePhotoBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI


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
                    storage.uploadImage(Uri.parse(uri), activityOnePhotoBinding.onePhotoTitle.text.toString()) {

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
                    activityOnePhotoBinding.onePhotoTime.text = exifInterface.getAttribute(ExifInterface.TAG_DATETIME).toString()
                    activityOnePhotoBinding.onePhotoCamera.text = exifInterface.getAttribute(ExifInterface.TAG_MODEL).toString()
                    activityOnePhotoBinding.onePhotoLens.text = exifInterface.getAttribute(ExifInterface.TAG_LENS_MAKE).toString()
                    activityOnePhotoBinding.onePhotoShutterSpeed.text = exifInterface.getAttributeDouble(ExifInterface.TAG_SHUTTER_SPEED_VALUE, 0.00).toString()
                    activityOnePhotoBinding.onePhotoFocalLength.text = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH).toString()
                    activityOnePhotoBinding.onePhotoAperture.text = exifInterface.getAttribute(ExifInterface.TAG_APERTURE_VALUE).toString()
                    activityOnePhotoBinding.onePhotoIso.text = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED).toString()
                    activityOnePhotoBinding.onePhotoLat.text = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE).toString()
                    activityOnePhotoBinding.onePhotoLng.text = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).toString()


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