package com.driveflow.driver.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic calculations (Haversine and spherical bearing) in pure Kotlin.
 */
object GeoUtils {
    const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates great-circle distance between two points in kilometers.
     */
    fun haversineKm(p1: LatLon, p2: LatLon): Double {
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLon = Math.toRadians(p2.lon - p1.lon)
        val lat1 = Math.toRadians(p1.lat)
        val lat2 = Math.toRadians(p2.lat)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculates the initial bearing from point 1 to point 2 in degrees (0..360).
     */
    fun bearingDegrees(p1: LatLon, p2: LatLon): Double {
        val lat1 = Math.toRadians(p1.lat)
        val lat2 = Math.toRadians(p2.lat)
        val dLon = Math.toRadians(p2.lon - p1.lon)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }

    /**
     * Checks if moving from [origin] to [destination] is toward [home]:
     * - Bearing from origin to destination is within 25% (45 degrees) of bearing to home.
     * - Destination distance does not exceed home distance by more than 25%.
     */
    fun isTowardHome(origin: LatLon, destination: LatLon, home: LatLon): Boolean {
        val bHome = bearingDegrees(origin, home)
        val bDest = bearingDegrees(origin, destination)
        var diff = Math.abs(bDest - bHome)
        if (diff > 180.0) {
            diff = 360.0 - diff
        }
        // Within 25% of 180° = 45°
        if (diff > 45.0) return false

        val distHome = haversineKm(origin, home)
        val distDest = haversineKm(origin, destination)
        // Destination does not overshoot home by more than 25%
        return distDest <= distHome * 1.25
    }
}
