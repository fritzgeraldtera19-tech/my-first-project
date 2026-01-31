package com.safe_tap.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationHandler(private val context: Context) {

    companion object {
        private const val TAG = "LocationHandler"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            onFailure("Location permission not granted")
            return
        }

        Log.d(TAG, "Getting last known location...")

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val age = System.currentTimeMillis() - location.time
                    Log.d(TAG, "Last known location found:")
                    Log.d(TAG, "  Coordinates: ${location.latitude}, ${location.longitude}")
                    Log.d(TAG, "  Accuracy: ${location.accuracy}m")
                    Log.d(TAG, "  Age: ${age / 1000}s")
                    Log.d(TAG, "  Provider: ${location.provider}")

                    onSuccess(location.latitude, location.longitude)
                } else {
                    Log.w(TAG, "Last location is null, requesting fresh location...")
                    getCurrentLocationFresh(onSuccess, onFailure)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get last location: ${exception.message}")
                onFailure(exception.message ?: "Failed to get location")
            }
    }

    private fun getCurrentLocationFresh(
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted for fresh location")
            onFailure("Location permission not granted")
            return
        }

        Log.d(TAG, "Requesting fresh current location with HIGH_ACCURACY...")

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(TAG, "Fresh location obtained:")
                Log.d(TAG, "  Coordinates: ${location.latitude}, ${location.longitude}")
                Log.d(TAG, "  Accuracy: ${location.accuracy}m")
                Log.d(TAG, "  Provider: ${location.provider}")

                onSuccess(location.latitude, location.longitude)
            } else {
                Log.e(TAG, "Fresh location is null - GPS may be disabled")
                onFailure("Unable to get current location. Please enable GPS and try again.")
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get fresh location: ${exception.message}")
            onFailure(exception.message ?: "Failed to get location")
        }
    }

    fun formatLocation(latitude: Double, longitude: Double): String {
        return "$latitude, $longitude"
    }

    fun getGoogleMapsUrl(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }
}