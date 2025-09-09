package com.naviq.trackmat

import android.location.Location
import android.util.Log

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
    var fullDistance: Double = 0.0
    var partDistance: Double = 0.0
    var partDistanceBackup: Double = 0.0

    fun addDistance(newLocation: Location) {
        if(currentLocation != null) {
            var newDistance = DistanceCalculator.Calculate(currentLocation!!, newLocation)

            if(newDistance != Float.NaN && newDistance != Float.POSITIVE_INFINITY
                && newDistance != Float.NEGATIVE_INFINITY) {
                fullDistance += newDistance
                partDistance += newDistance
            }
        }

        currentLocation = newLocation
    }

    fun resetDistances() {
        currentLocation = null

        fullDistance = 0.0
        partDistance = 0.0
        partDistanceBackup = 0.0
    }

    fun clearPartDistance() {
        partDistanceBackup = partDistance
        partDistance = 0.0
    }

    fun clearLocation() {
        currentLocation = null
    }

    fun undoPartDistance() {
        partDistance = partDistanceBackup
        partDistanceBackup = 0.0
    }
}