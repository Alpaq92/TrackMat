package com.naviq.trackmat

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class LocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient) {

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(interval: Long = 2000L, tolerance: Long = 30L): Flow<Location> {
        val locationFlow = callbackFlow {
            if(!context.hasLocationPermission()) {
                throw LocationException()
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if(!isGpsEnabled && !isNetworkEnabled) {
                throw LocationException()
            }

            val locationRequest = LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(interval)
                .setPriority(PRIORITY_HIGH_ACCURACY)

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)

                    val location = result.locations.minBy { it.accuracy }
                    if(location != null && location.accuracy < tolerance) {
                        launch { send(location) }
                    }
                }
            }

            client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }

        return locationFlow
    }

    class LocationException : Exception()
}