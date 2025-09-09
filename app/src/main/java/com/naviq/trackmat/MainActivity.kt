package com.naviq.trackmat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.action_space))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        }
        else {
            registerReceiver(broadcastReceiver, filter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshActivity(LocationService.isRunning, true)
        updateValues()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        updateValues()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
            {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_CODE)
                }
            }
        }
    }

    fun onPlayPause(view: View) {
        if ((ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        ) {
            try {
                refreshActivity(!LocationService.isRunning)

                if(!LocationService.isRunning) {
                    val intent = Intent(applicationContext, LocationService::class.java).apply {
                        action = LocationService.ACTION_START
                        startService(this)
                    }

                    applicationContext.startService(intent)
                } else {
                    val intent = Intent(applicationContext, LocationService::class.java).apply {
                        action = LocationService.ACTION_PAUSE
                        startService(this)
                    }

                    applicationContext.startService(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.message!!.take(100), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, resources.getString(R.string.permission_missing), Toast.LENGTH_LONG).show()
        }
    }

    fun onStop(view: View) {
        val intent = Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            startService(this)
        }

        applicationContext.startService(intent)
        DistanceTracker.getInstance().resetDistances()

        val green = resources.getColor(R.color.green)
        val playPauseButton = findViewById<ImageButton>(R.id.playPauseButton)
        playPauseButton.setImageResource(R.drawable.ic_play)
        playPauseButton.setBackgroundColor(green)
    }

    fun onClear(view: View) {
        DistanceTracker.getInstance().clearPartDistance()
        updateValues()
    }

    fun onUndo(view: View) {
        DistanceTracker.getInstance().undoPartDistance()
        updateValues()
    }

    private fun refreshActivity(isTracking: Boolean, onCreate: Boolean = false) {
        if(onCreate) {
            var config = resources.configuration

            if (config.orientation === Configuration.ORIENTATION_PORTRAIT) {
                setContentView(R.layout.activity_main_vertical)
            } else if (config.orientation === Configuration.ORIENTATION_LANDSCAPE) {
                setContentView(R.layout.activity_main_horizontal)
            }
        }

        val green = resources.getColor(R.color.green)
        val yellow = resources.getColor(R.color.yellow)

        val playPauseButton = findViewById<ImageButton>(R.id.playPauseButton)

        if(isTracking) {
            playPauseButton.setImageResource(R.drawable.ic_pause)
            playPauseButton.setBackgroundColor(yellow)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play)
            playPauseButton.setBackgroundColor(green)
        }
    }

    private fun updateValues() {
        val intent = Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_REFRESH
            startService(this)
        }

        applicationContext.startService(intent)
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            for (key in intent.extras!!.keySet()) {
                val text = intent.getStringExtra(key)

                val id = resources.getIdentifier(key, "id", context.packageName)
                val textView = findViewById<TextView>(id)

                if(textView != null) {
                    textView.text = text
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 10001
    }
}