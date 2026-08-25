package com.chronopass.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Fix(val latitude: Double, val longitude: Double, val accuracy: Float)

// Fast path first (última localização conhecida = instantânea), depois um fix novo
// com prioridade balanceada (bem mais rápido que HIGH_ACCURACY em campo aberto/indoor).
@SuppressLint("MissingPermission")
suspend fun getCurrentFix(context: Context): Fix? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    lastLocation(client)?.let { return it.toFix() }
    return freshLocation(client)?.toFix()
}

@SuppressLint("MissingPermission")
private suspend fun lastLocation(client: FusedLocationProviderClient): Location? =
    runCatching { client.lastLocation.awaitOrNull() }.getOrNull()

@SuppressLint("MissingPermission")
private suspend fun freshLocation(client: FusedLocationProviderClient): Location? =
    suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }
        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (e: SecurityException) {
            cont.resume(null)
        }
    }

private fun Location.toFix() = Fix(latitude, longitude, accuracy)

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
}

/** Distance in meters between two coordinates. */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val out = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, out)
    return out[0]
}
