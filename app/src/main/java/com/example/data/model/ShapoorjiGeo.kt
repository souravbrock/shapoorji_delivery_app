package com.example.data.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ShapoorjiGeo {
    // Center point of Shapoorji Sukhobristi / Pallonji Township in New Town Action Area III, Kolkata
    const val CENTER_LATITUDE = 22.5695
    const val CENTER_LONGITUDE = 88.5195
    const val MAX_RADIUS_METERS = 1800.0 // 1.8 km covers all Phases, Towers and Spacio inside Shapoorji

    val TOWERS = listOf(
        "Sukhobristi Phase 1 - Tower A1",
        "Sukhobristi Phase 1 - Tower A2",
        "Sukhobristi Phase 1 - Tower A3",
        "Sukhobristi Phase 1 - Tower A4",
        "Sukhobristi Phase 1 - Tower A8",
        "Sukhobristi Phase 1 - Tower B2",
        "Sukhobristi Phase 1 - Tower B14",
        "Sukhobristi Phase 1 - Tower B22",
        "Sukhobristi Phase 1 - Tower C5",
        "Sukhobristi Phase 1 - Tower C18",
        "Sukhobristi Phase 2 - Tower D4",
        "Sukhobristi Phase 2 - Tower D11",
        "Sukhobristi Phase 2 - Tower E6",
        "Sukhobristi Phase 2 - Tower E19",
        "Sukhobristi Phase 2 - Tower F3",
        "Spacio at Shapoorji - Tower 2",
        "Spacio at Shapoorji - Tower 5",
        "Shapoorji Sukhobristi Club House Area",
        "Shapoorji Commercial Complex / Main Gate"
    )

    /**
     * Calculates geodesic distance in meters using Haversine formula
     */
    fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    /**
     * Returns true if coordinates lie strictly within Shapoorji Sukhobristi delivery zone
     */
    fun isInsideShapoorji(lat: Double, lon: Double): Boolean {
        return distanceInMeters(lat, lon, CENTER_LATITUDE, CENTER_LONGITUDE) <= MAX_RADIUS_METERS
    }
}
