package com.naviq.trackmat

import android.location.Location


class DistanceTracker private constructor() {
    companion object {

        @Volatile
        private var instance: DistanceTracker? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: DistanceTracker().also { instance = it }
            }
    }

    var currentLocation: Location? = null
    var totalDistance: Double = 0.0
    var currentDistance: Double = 0.0

    fun addDistance(newLocation: Location) {
        if(currentLocation != null) {
            val newDistance = DistanceCalculator.calculate(currentLocation!!, newLocation)

            if(newDistance != Float.POSITIVE_INFINITY && newDistance != Float.NEGATIVE_INFINITY) {
                totalDistance += newDistance
                currentDistance += newDistance
            }
        }

        currentLocation = newLocation
    }

    fun resetDistances() {
        currentLocation = null

        totalDistance = 0.0
        currentDistance = 0.0
    }

    fun clearPartDistance() {
        currentDistance = 0.0
    }

    fun clearLocation() {
        currentLocation = null
    }
}