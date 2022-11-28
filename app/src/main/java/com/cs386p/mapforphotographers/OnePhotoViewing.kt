package com.cs386p.mapforphotographers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.commit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.cs386p.mapforphotographers.databinding.ActivityOnePhotoBinding
import com.cs386p.mapforphotographers.databinding.ActivityOnePhotoViewingBinding
import com.cs386p.mapforphotographers.model.PhotoMeta
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Array.newInstance
import java.net.URI
import java.util.*


class OnePhotoViewing: AppCompatActivity(), OnMapReadyCallback {
    companion object {
        const val photoMetaKey = "photoMeta"
    }

    val viewModel: PhotoViewModel by viewModels()
    var photometa = PhotoMeta()

    private lateinit var map: GoogleMap
    private var locationPermissionGranted = false

    private var _binding: ActivityOnePhotoViewingBinding? = null
    private val binding get() = _binding!!

    fun hideBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        // XXX Write me (one liner)
        supportActionBar?.hide()
    }
    fun showBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Hide both the status bar and the navigation bar
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        supportActionBar?.show()
    }

    private fun buttonLiked(isLiked: Boolean) {
        if(isLiked) {
            binding.onePhotoViewButtonLike.text = "Liked!"
            binding.onePhotoViewButtonLike.setBackgroundColor(Color.parseColor("#d94434"))
        } else {
            binding.onePhotoViewButtonLike.text = "Like this photo"
            binding.onePhotoViewButtonLike.setBackgroundColor(Color.GRAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityOnePhotoViewingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)

        photometa = intent.getParcelableExtra(photoMetaKey)!!

        checkGooglePlayServices()
        requestPermission()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.one_photo_view_mapFrag) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!photometa.uuid.isNullOrEmpty()) {
            binding.onePhotoViewButtonLike.setOnClickListener {
                val user = FirebaseAuth.getInstance().currentUser
                if(user != null) {
                    if(photometa.likedBy.contains(user.uid)) {
                        // unlike photo
                        viewModel.unlikeOnePhoto(photometa.uuid)
                        buttonLiked(false)
                        photometa.likedBy.remove(user.uid)

                    } else {
                        // like photo
                        viewModel.likeOnePhoto(photometa.uuid)
                        buttonLiked(true)
                        photometa.likedBy += user.uid
                    }
                }
            }

            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                if(photometa.likedBy.contains(user.uid)) {
                    // change button to already liked
                    buttonLiked(true)

                } else {
                    // change button to not like
                    buttonLiked(false)
                }
            } else {
                // set button unclickable
                binding.onePhotoViewButtonLike.isClickable = false
                binding.onePhotoViewButtonLike.text = "Login to like this photo!"
            }
            viewModel.glideFetch(photometa.uuid, binding.onePhotoImage)
            viewModel.glideFetchFull(photometa.uuid, binding.onePhotoImageFullScreen)

            binding.onePhotoImage.setOnClickListener {
                hideBars()
                binding.onePhotoLayoutFullScreen.visibility = View.VISIBLE
            }
            binding.onePhotoImageFullScreen.setOnClickListener {
                showBars()
                binding.onePhotoLayoutFullScreen.visibility = View.GONE
            }

            binding.onePhotoViewPhotographer.text = photometa.ownerName
            binding.onePhotoViewTime.text = photometa.pictureDate
            binding.onePhotoViewCamera.text = photometa.pictureCamera
            binding.onePhotoViewLens.text = photometa.pictureLens
            binding.onePhotoViewShutterSpeed.text = photometa.pictureShutterSpeed
            binding.onePhotoViewFocalLength.text = photometa.pictureFocalLength
            binding.onePhotoViewAperture.text = photometa.pictureAperture
            binding.onePhotoViewIso.text = photometa.pictureIso
            binding.onePhotoViewLat.text = photometa.pictureLat
            binding.onePhotoViewLng.text = photometa.pictureLng
            supportActionBar?.title = photometa.pictureTitle
            binding.onePhotoViewDescription.text = photometa.pictureDescription
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.onMapReady(map)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.d("xxx", locationPermissionGranted.toString())
        if( locationPermissionGranted ) {
            // XXX Write me.
            // Note, we checked location permissions in requestPermission, but the compiler
            // might complain about our not checking it.
            val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            Log.d("xxx", permission.toString())

            if (permission != PackageManager.PERMISSION_GRANTED) {
                if (!locationPermissionGranted) {
                    Toast.makeText(this,
                        "Unable to show location - permission required",
                        Toast.LENGTH_LONG).show()
                    return
                }
            }
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }

        // Start the map at the Harry Ransom center
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(photometa.pictureLat.toDouble(), photometa.pictureLng.toDouble()), 15.0f))
        val titleString = "%.3f".format(photometa.pictureLat.toDouble()) + " " + "%.3f".format(photometa.pictureLng.toDouble())
        map.addMarker(
            MarkerOptions()
                .position(LatLng(photometa.pictureLat.toDouble(), photometa.pictureLng.toDouble()))
                .title(titleString)
        )
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode =
            googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 257)?.show()
            } else {
                Log.i(javaClass.simpleName,
                    "This device must install Google Play Services.")
                finish()
            }
        }
    }

    private fun requestPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    locationPermissionGranted = true;
                } else -> {
                Toast.makeText(this,
                    "Unable to show location - permission required",
                    Toast.LENGTH_LONG).show()
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }
}