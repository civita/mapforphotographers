package com.cs386p.mapforphotographers

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import java.math.BigDecimal
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cs386p.mapforphotographers.databinding.ActivityOnePhotoBinding
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.RoundingMode
import java.net.URI
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt


class OnePhoto: AppCompatActivity(), OnMapReadyCallback {
    companion object {
        const val uriKey = "uri"
    }
    private val viewModel: PhotoViewModel by viewModels()

    private var uri : String = """"""
    private val storage = Storage()

    private var photometa = PhotoMeta()

    private lateinit var map: GoogleMap
    private var locationPermissionGranted = false

    private var _binding: ActivityOnePhotoBinding? = null
    private val binding get() = _binding!!

    fun convertToExposureTime(value: Double): String {
        var ret = ""
        if (value>0) {
            ret += "1/"
            ret += BigDecimal(2.0.pow(value)).setScale(0, RoundingMode.HALF_EVEN).toString()
        } else {
            ret += BigDecimal(2.0.pow(-1.0*value)).setScale(0, RoundingMode.HALF_EVEN).toString()

        }
        return ret + "s"
    }

    fun convertToFStop(value: Double): String {
        return "f/" + BigDecimal(sqrt(2.0.pow(value))).setScale(1, RoundingMode.HALF_EVEN).toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityOnePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        supportActionBar?.title = "Upload a photo"

        uri = intent.getStringExtra(uriKey).toString()
        Log.d("xxx_onephoto", uri)

        // Google maps things...
        checkGooglePlayServices()
        requestPermission()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.one_photo_mapFrag) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!uri.isNullOrEmpty()) {

            binding.onePhotoButtonSubmit.setOnClickListener {
                if (photometa.pictureLat == "null" || photometa.pictureLng == "null") { // some dirty work to avoid null geotag
                    val snack = Snackbar.make(it,"Photo needs a location!", Snackbar.LENGTH_LONG)
                    snack.show()
                } else {
                    if(!binding.onePhotoTitle.text.isNullOrBlank()) {
                        val uuid = UUID.randomUUID().toString()
                        photometa.pictureTitle = binding.onePhotoTitle.text.toString()
                        photometa.pictureDescription = binding.onePhotoDescription.text.toString()
                        photometa.private = binding.onePhotoPrivate.isChecked
                        storage.uploadImage(Uri.parse(uri), uuid) { ret ->
                            viewModel.createPhotoMeta(photometa, uuid)
                            // todo snack not shown
                            val snack = Snackbar.make(it,"Upload completed!", Snackbar.LENGTH_LONG)
                            snack.show()
                            finish()
                        }
                    } else {
                        val snack = Snackbar.make(it,"Photo needs a title!", Snackbar.LENGTH_LONG)
                        snack.show()
                    }
                }
            }
            binding.onePhotoImage.setImageURI(Uri.parse(uri))

            val sIn: InputStream?
            try {
                sIn = contentResolver.openInputStream(Uri.parse(uri))
                val exifInterface = sIn?.let { ExifInterface(it) }
                if (exifInterface != null) {
                    photometa.pictureDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL).toString()
                    if (photometa.pictureDate == "null")
                        photometa.pictureDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME).toString()
                    binding.onePhotoTime.text = photometa.pictureDate

                    photometa.pictureCamera = exifInterface.getAttribute(ExifInterface.TAG_MODEL).toString()
                    binding.onePhotoCamera.text = photometa.pictureCamera

                    photometa.pictureLens = exifInterface.getAttribute(ExifInterface.TAG_LENS_MODEL).toString()
                    binding.onePhotoLens.text = photometa.pictureLens

                    photometa.pictureShutterSpeed = convertToExposureTime(exifInterface.getAttributeDouble(ExifInterface.TAG_SHUTTER_SPEED_VALUE, 0.00))
                    binding.onePhotoShutterSpeed.text = photometa.pictureShutterSpeed

                    photometa.pictureFocalLength = exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.00).toString() + "mm"
                    binding.onePhotoFocalLength.text = photometa.pictureFocalLength

                    photometa.pictureAperture = convertToFStop(exifInterface.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.00))
                    binding.onePhotoAperture.text = photometa.pictureAperture

                    photometa.pictureIso = exifInterface.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY).toString()
                    binding.onePhotoIso.text = photometa.pictureIso

                    //photometa.pictureLat = exifInterface.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, 0.00).toString()
                    photometa.pictureLat = exifInterface.latLong?.first().toString()
                    binding.onePhotoLat.text = photometa.pictureLat

                    //photometa.pictureLng = exifInterface.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, 0.00).toString()
                    photometa.pictureLng = exifInterface.latLong?.last().toString()
                    binding.onePhotoLng.text = photometa.pictureLng
                }
                try {
                    sIn?.close()
                } catch (ignored: IOException) {
                }

            } catch (e: IOException) {
                // Handle any errors
            }
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

        map.setOnMapClickListener {
            map.clear()

            val titleString = "%.3f".format(it.latitude) + " " + "%.3f".format(it.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(titleString)
            )

            photometa.pictureLat = it.latitude.toString()
            binding.onePhotoLat.text = photometa.pictureLat

            photometa.pictureLng = it.longitude.toString()
            binding.onePhotoLng.text = photometa.pictureLng
        }

        if (photometa.pictureLat != "null" && photometa.pictureLng != "null") { // some dirty work to avoid null geotag
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(photometa.pictureLat.toDouble(), photometa.pictureLng.toDouble()), 15.0f))
            val titleString = "%.3f".format(photometa.pictureLat.toDouble()) + " " + "%.3f".format(photometa.pictureLng.toDouble())
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(photometa.pictureLat.toDouble(), photometa.pictureLng.toDouble()))
                    .title(titleString)
            )
        }
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