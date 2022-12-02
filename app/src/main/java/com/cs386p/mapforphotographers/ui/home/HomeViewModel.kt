package com.cs386p.mapforphotographers.ui.home

import android.app.PendingIntent.getActivity
import android.location.Geocoder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs386p.mapforphotographers.MainActivity
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.Provider

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    // Start the map at the Harry Ransom center
    private val _position = MutableLiveData<LatLng>().apply {
        value = LatLng(30.28400, -97.743083)
    }
    val position: LiveData<LatLng> = _position

    fun getGeocode(address: String, geocoder: Geocoder) {
        // XXX Write me.  This is where the network request is initiated.
        viewModelScope.launch(
            context = viewModelScope.coroutineContext + Dispatchers.IO
        ) {
            val geocodeResult = geocoder.getFromLocationName(address, 1)
            println(geocodeResult)
            if (geocodeResult.isNotEmpty()) {
                if (geocodeResult[0].hasLatitude()) {
                    _position.postValue(LatLng(geocodeResult[0].latitude, geocodeResult[0].longitude))
                }
            }
        }
    }
}