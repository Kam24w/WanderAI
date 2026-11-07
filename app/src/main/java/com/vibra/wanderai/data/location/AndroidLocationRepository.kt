package com.vibra.wanderai.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/** Framework `LocationManager` (GPS + network) — no Play Services dependency. */
class AndroidLocationRepository(
    private val context: Context,
) : LocationRepository {

    private val locationManager: LocationManager? =
        context.getSystemService(LocationManager::class.java)

    override fun hasPermission(): Boolean =
        PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // Guarded by hasPermission().
    override fun observeLocation(): Flow<GeoPoint> {
        val manager = locationManager ?: return emptyFlow()
        if (!hasPermission()) return emptyFlow()

        return callbackFlow {
            val listener = LocationListener { location -> trySend(location.toGeoPoint()) }
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

            providers
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull(Location::getTime)
                ?.let { trySend(it.toGeoPoint()) }

            providers.forEach { provider ->
                manager.requestLocationUpdates(
                    provider, MIN_INTERVAL_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper()
                )
            }
            awaitClose { manager.removeUpdates(listener) }
        }
    }

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private const val MIN_INTERVAL_MS = 3_000L
        private const val MIN_DISTANCE_M = 3f
    }
}
