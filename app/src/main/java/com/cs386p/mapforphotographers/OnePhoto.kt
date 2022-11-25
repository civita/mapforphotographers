package com.cs386p.mapforphotographers

import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.cs386p.mapforphotographers.databinding.ActivityOnePhotoBinding


class OnePhoto: AppCompatActivity() {
    companion object {
        const val titleKey = "title"
        const val selfTextKey = "selfText"
        const val imageURLKey = "imageURL"
        const val thumbnailURLKey = "thumbnailURL"
        const val uriKey = "uri"
    }

    private var title : String = """"""
    private var selfText : String = """"""
    private var imageURL : String = """"""
    private var thumbnailURL : String = """"""
    private var uri : String = """"""

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
            activityOnePhotoBinding.onePhotoImage.setImageURI(Uri.parse(uri))
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