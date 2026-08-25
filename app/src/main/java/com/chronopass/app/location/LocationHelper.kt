package com.chronopass.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Fix(val latitude: Double, val longitude: Double, val accuracy: Float)

// ponytail: single current-location shot, no continuous updates.
@SuppressLint("MissingPermission")
suspend fun getCurrentFix(context: Context): Fix? = suspendCancellableCoroutine { cont ->
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    cont.invokeOnCancellation { cts.cancel() }
    try {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { Fix(it.latitude, it.longitude, it.accuracy) })
            }
            .addOnFailureListener { cont.resume(null) }
    } catch (e: SecurityException) {
        cont.resume(null) // permission not granted
    }
}

/** Distance in meters between two coordinates. */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val out = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, out)
    return out[0]
}
