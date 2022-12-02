package com.cs386p.mapforphotographers.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.cs386p.mapforphotographers.MainActivity
import com.cs386p.mapforphotographers.PhotoViewModel
import com.cs386p.mapforphotographers.PhotoViewModel.Companion.doOnePhotoViewing
import com.cs386p.mapforphotographers.R
import com.cs386p.mapforphotographers.databinding.FragmentHomeBinding
import com.cs386p.mapforphotographers.model.PhotoMeta
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.internal.ViewUtils.hideKeyboard

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private var locationPermissionGranted = false
    private val viewModelPhoto: PhotoViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private var photoList: List<PhotoMeta> = listOf()
    private var zoom: Float = 0.0F
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var deviceLocationGot: Boolean = false

    // An Android nightmare
    // https://stackoverflow.com/a/70562398
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    // https://stackoverflow.com/questions/7789514/how-to-get-activitys-windowtoken-without-view
    private fun hideKeyboard() {
        if (activity != null){
            // Hide soft keyboard
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModelPhoto.fetchPublicPhotoMeta()

        geocoder = Geocoder(root.context)
        checkGooglePlayServices()
        requestPermission()
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFrag) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(binding.root.context)

        viewModelPhoto.observePhotoMeta().observe(viewLifecycleOwner) {
            if (this::map.isInitialized) {
                photoList = it
                Log.d("xxx_home", it.size.toString())
                map.clear()
                zoom = map.cameraPosition.zoom
                for (i in it.indices) {
                    MainScope().launch(Dispatchers.Default) {
                        // aWait for bitmap job to finish
                        if(it.size > i) {
                            val bitmap = viewModelPhoto.glideFetch(it[i].uuid, binding.root.context, zoom)
                            // Modify map on main thread
                            withContext(Dispatchers.Main) {
                                map.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(it[i].pictureLat.toDouble(), it[i].pictureLng.toDouble()))
                                        .title(it[i].pictureTitle)
                                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                )?.tag = i
                            }
                        }
                    }
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        viewModelPhoto.fetchPublicPhotoMeta()
        super.onResume()
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
        if(locationPermissionGranted) {
            // Note, we checked location permissions in requestPermission, but the compiler
            // might complain about our not checking it.
            val permission = ContextCompat.checkSelfPermission(binding.root.context,
                Manifest.permission.ACCESS_FINE_LOCATION)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                if (!locationPermissionGranted) {
                    val snack = Snackbar.make(binding.root, "Unable to show location - permission required", Snackbar.LENGTH_LONG)
                    snack.show()
                    return
                }
            }
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }

        map.setOnMarkerClickListener {
            Log.d("xxx_home", it.tag.toString())
            doOnePhotoViewing(binding.root.context, photoList[it.tag as Int])
            true
        }

        map.setOnCameraIdleListener {
            if(map.cameraPosition.zoom != zoom) {
                Log.d("xxx_home", zoom.toString())
                map.clear()
                zoom = map.cameraPosition.zoom
                for (i in photoList.indices) {
                    MainScope().launch(Dispatchers.Default) {
                        // aWait for bitmap job to finish
                        val bitmap = viewModelPhoto.glideFetch(photoList[i].uuid, binding.root.context, zoom)
                        // Modify map on main thread
                        withContext(Dispatchers.Main) {
                            map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(photoList[i].pictureLat.toDouble(), photoList[i].pictureLng.toDouble()))
                                    .title(photoList[i].pictureTitle)
                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            )?.tag = i
                        }
                    }
                }
            }

        }

        binding.goBut.setOnClickListener {
            if(!binding.mapET.text.isNullOrBlank()) {
                MainScope().launch(Dispatchers.Default) {
                    val address = geocoder.getFromLocationName(binding.mapET.text.toString(), 1)
                    withContext(Dispatchers.Main) {
                        if(address.size > 0 && address.first() != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(address.first().latitude, address.first().longitude), 15.0f))
                        }
                    }
                }
                binding.mapET.text.clear()
                binding.mapET.clearFocus()
                hideKeyboard()
            } else {
                val snack = Snackbar.make(it, "Please provide an address!", Snackbar.LENGTH_SHORT)
                snack.show()
            }
        }
        getDeviceLocation()
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     * https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
     */
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (!deviceLocationGot) {
            try {
                if (locationPermissionGranted) {
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { lastKnownLocation ->
                        if (lastKnownLocation != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastKnownLocation.latitude,
                                    lastKnownLocation.longitude), 15.0F))
                            deviceLocationGot = true
                        } else {
                            map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(30.28400, -97.743083),
                                        15.0F
                                    )
                                )
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("Exception: %s", e.message, e)
            }
        }

    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode =
            googleApiAvailability.isGooglePlayServicesAvailable(binding.root.context)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 257)?.show()
            } else {
                Log.i(javaClass.simpleName,
                    "This device must install Google Play Services.")
                //finish()
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
                val snack = Snackbar.make(binding.root, "Unable to show location - permission required", Snackbar.LENGTH_LONG)
                snack.show()
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }
}