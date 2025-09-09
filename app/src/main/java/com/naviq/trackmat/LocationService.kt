package com.naviq.trackmat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
    private val locationLock = ReentrantLock()
    private var pattern = DecimalFormat("0.00")

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationClient(applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_PAUSE -> pause()
            ACTION_REFRESH -> refresh()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun start() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, INTENT_REQUEST, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(resources.getString(R.string.distance_notification))
            .setContentText(resources.getString(R.string.distance_blank_text))
            .setSmallIcon(R.drawable.ic_path)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notification.build())

        locationClient
            .getLocationUpdates(2000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                if(locationLock.tryLock(500L, TimeUnit.MILLISECONDS)) {
                    val accuracy = pattern.format(location.accuracy)
                    DistanceTracker.getInstance().addDistance(location)
                    val distances = refresh()

                    val updatedNotification = notification
                        .setContentText("${distances.full} - ${distances.part} (${distances.partBackup}) [${accuracy}]"
                        )
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())

                    locationLock.unlock()
                }
            }
            .launchIn(scope)

        isRunning = true
    }

    private fun stop() {
        stopSelf()
        DistanceTracker.getInstance().resetDistances()
        refresh()

        isRunning = false
    }

    private fun pause() {
        stopSelf()
        DistanceTracker.getInstance().clearLocation()
        refresh()

        isRunning = false
    }

    private fun refresh(): Distances {
        val distances = Distances()
        distances.full = pattern.format(DistanceTracker.getInstance().fullDistance/1000)
        distances.part = pattern.format(DistanceTracker.getInstance().partDistance/1000)
        distances.partBackup = pattern.format(DistanceTracker.getInstance().partDistanceBackup/1000)

        val intent = Intent()
        intent.action = resources.getString(R.string.action_space)
        intent.putExtra("fullDistance", distances.full)
        intent.putExtra("partDistance", distances.part)
        intent.putExtra("partDistanceBackup", distances.partBackup)
        sendBroadcast(intent)

        return distances
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_REFRESH = "ACTION_REFRESH"
        const val CHANNEL_ID = "LOCATION_CHANNEL"
        const val CHANNEL_NAME = "LOCATION_CHANNEL"
        const val NOTIFICATION_ID = 20002
        const val INTENT_REQUEST = 30003
        var isRunning: Boolean = false
            get() = field
            private set(value) {
                field = value
            }
    }

    class Distances {
        lateinit var full: String
        lateinit var part: String
        lateinit var partBackup: String
    }
}