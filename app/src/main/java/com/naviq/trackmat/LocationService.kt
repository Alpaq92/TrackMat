package com.naviq.trackmat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


class LocationService: Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private lateinit var preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener
    private val locationLock = ReentrantLock()
    private val lockTimeout = 500L
    private var pattern = DecimalFormat()

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        locationClient = LocationClient(applicationContext, LocationServices.getFusedLocationProviderClient(applicationContext))

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key != resources.getString(R.string.help_preference) && key != resources.getString(R.string.restore_preference) && !key.isNullOrBlank()) {
                restart()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)

        pattern = DecimalFormat(resources.getString(R.string.default_decimal_format))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            TRACKING_START -> start()
            TRACKING_STOP -> stop()
            TRACKING_PAUSE -> pause()
            TRACKING_REFRESH -> refresh()
            TRACKING_RESTART -> restart()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)

        scope.cancel()
    }

    private fun start() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        displayUnits = preferences.getString(resources.getString(R.string.unit_preference), null) ?: resources.getString(R.string.default_unit)
        val index = resources.getStringArray(R.array.units).indexOf(displayUnits)
        val divisors = resources.getStringArray(R.array.divisors)
        divisor = divisors[index].toDouble()

        val defaultFrequency = resources.getInteger(R.integer.default_frequency)
        val defaultTolerance = resources.getInteger(R.integer.default_tolerance)

        val frequency = preferences.getInt(resources.getString(R.string.frequency_preference), defaultFrequency) * 1000L
        val tolerance = (preferences.getInt(resources.getString(R.string.tolerance_preference), defaultTolerance)).toLong()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, INTENT_REQUEST, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle("${resources.getString(R.string.distance_notification)} [${displayUnits}]")
            .setContentText(resources.getString(R.string.distance_blank_text))
            .setSmallIcon(R.drawable.ic_path)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notification.build())

        locationClient
            .getLocationUpdates(frequency, tolerance)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                if(locationLock.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
                    val accuracy = pattern.format(location.accuracy)
                    DistanceTracker.getInstance().addDistance(location)
                    val distances = refresh()

                    val updatedNotification = notification.setContentText("${distances.total} (${distances.current}) [${accuracy}]")
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())

                    locationLock.unlock()
                }
            }
            .launchIn(scope)

        state = ServiceState.RUNNING
    }

    private fun stop() {
        stopSelf()
        DistanceTracker.getInstance().resetDistances()
        refresh()

        state = ServiceState.HALT
    }

    private fun pause() {
        stopSelf()
        DistanceTracker.getInstance().clearLocation()
        refresh()

        state = ServiceState.PAUSED
    }

    private fun restart() {
        if (state == ServiceState.RUNNING) {
            pause()

            val intent = Intent(applicationContext, LocationService::class.java).apply {
                action = TRACKING_START
                startService(this)
            }

            applicationContext.startService(intent)
        }
    }

    private fun refresh(): Distances {
        val distances = Distances()

        distances.total = pattern.format(if (divisor != 0.0) DistanceTracker.getInstance().totalDistance/divisor else 0L)
        distances.current = pattern.format(if (divisor != 0.0) DistanceTracker.getInstance().currentDistance/divisor else 0L)

        val intent = Intent()
        intent.action = resources.getString(R.string.action_space)

        intent.putExtra(R.id::totalDistance.name, distances.total)
        intent.putExtra(R.id::currentDistance.name, distances.current)
        intent.putExtra(R.id::displayUnits.name, displayUnits)

        sendBroadcast(intent)

        return distances
    }

    companion object {
        const val TRACKING_START = "TRACKING_START"
        const val TRACKING_STOP = "TRACKING_ACTION_STOP"
        const val TRACKING_PAUSE = "TRACKING_PAUSE"
        const val TRACKING_REFRESH = "TRACKING_REFRESH"
        const val TRACKING_RESTART = "TRACKING_RESTART"
        const val CHANNEL_ID = "LOCATION_CHANNEL"
        const val CHANNEL_NAME = "LOCATION_CHANNEL"
        const val NOTIFICATION_ID = 20002
        const val INTENT_REQUEST = 30003

        val isRunning: Boolean
            get() = state == ServiceState.RUNNING
        var state: ServiceState = ServiceState.HALT

        var displayUnits: String = String()
        var divisor: Double = 0.0
    }

    class Distances {
        lateinit var total: String
        lateinit var current: String
    }

    enum class ServiceState {
        HALT, RUNNING, PAUSED
    }
}