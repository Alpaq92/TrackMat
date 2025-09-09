package com.naviq.trackmat

import android.location.Location


object DistanceCalculator {
    fun calculate(loc1: Location, loc2: Location): Float {
        return loc1.distanceTo(loc2)
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }
}