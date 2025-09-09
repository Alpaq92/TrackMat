package com.naviq.trackmat

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    @SuppressLint("InlinedApi")
    override fun onStart() {
        super.onStart()

        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.action_space))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_EXPORTED)
        }
        else {
            registerReceiver(locationReceiver, filter)
        }
    }

    @SuppressLint("InlinedApi")
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
        unregisterReceiver(locationReceiver)
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
                    changeState(LocationService.TRACKING_START, false)
                } else {
                    changeState(LocationService.TRACKING_PAUSE, false)
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.message!!.take(100), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, resources.getString(R.string.permission_missing), Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    fun onStop(view: View) {
        changeState(LocationService.TRACKING_STOP, false)

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

    fun onEdit(view: View) {
        val wasRunning = LocationService.isRunning

        if (wasRunning) {
            changeState(LocationService.TRACKING_PAUSE, false)
        }

        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.activity_edit, null)
        val context = ContextThemeWrapper(this, R.style.Base_Theme_TrackMat)

        val builder = AlertDialog.Builder(context)
        builder
            .setView(view)
            .setTitle(resources.getString(R.string.edit_new_values))
            .setPositiveButton(resources.getString(R.string.app_ok)) { dialog, which ->
                val total = getValue(view, R.id.totalEdit)
                val current = getValue(view, R.id.currentEdit)

                if (total != null) {
                    DistanceTracker.getInstance().totalDistance = total * 1000
                }

                if (current != null) {
                    DistanceTracker.getInstance().currentDistance = current * 1000
                }

                DistanceTracker.getInstance().clearLocation()

                if (wasRunning) {
                    changeState(LocationService.TRACKING_START, true)
                }

                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.app_cancel)) { dialog, which ->
                dialog.cancel()

                if (wasRunning) {
                    changeState(LocationService.TRACKING_START, true)
                }
            }

        val dialog = builder.create()
        dialog.show()
    }

    fun onSettings(view: View) {
        val intent = Intent(this, CustomPreferencesActivity::class.java)
        this.startActivity(intent)
    }

    @Suppress("DEPRECATED_IDENTITY_EQUALS")
    private fun refreshActivity(isTracking: Boolean, onCreate: Boolean = false) {
        if(onCreate) {
            val config = resources.configuration

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
            action = LocationService.TRACKING_REFRESH
            startService(this)
        }

        applicationContext.startService(intent)
    }

    private fun changeState(serviceAction: String, refreshValues: Boolean) {
        val intent = Intent(applicationContext, LocationService::class.java).apply {
            action = serviceAction
            startService(this)
        }

        applicationContext.startService(intent)

        if (refreshValues) {
            updateValues()
        }
    }

    private fun getValue(view: View, id: Int): Double? {
        try {
            val editText = view.findViewById<EditText>(id).text.toString()
            return editText.toDouble().round(2)
        }
        catch (error: Exception) {
            // ignore
        }

        return null
    }

    private var locationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            for (key in intent.extras!!.keySet()) {
                val text = intent.getStringExtra(key)

                if (text != null) {
                    val id = resources.getIdentifier(key, resources.getString(R.string.id_key), context.packageName)
                    val textView = findViewById<TextView>(id)

                    if (textView != null) {
                        textView.text = text
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 10001
    }
}