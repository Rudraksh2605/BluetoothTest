package com.hfad.bluetoothtest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var locationText: TextView
    private val locationChannelId = "location_updates_channel"
    private val notificationId = 2

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.0)
            val longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.0)
            locationText.text = "Latitude: $latitude, Longitude: $longitude"
            showLocationUpdateNotification(latitude, longitude)
        }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocationGranted && coarseLocationGranted) {
                startLocationService()
                askForNotificationPermission()
            } else {
                Toast.makeText(
                    this, "Location permissions denied. App functionality may be limited.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this, "Notification permission denied. Notifications won't be shown.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationText = findViewById(R.id.location_text)
        createNotificationChannel()

        // Request location permissions once
        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(LocationService.ACTION_LOCATION_BROADCAST)
        registerReceiver(locationReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(locationReceiver)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                locationChannelId,
                "Location Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showLocationUpdateNotification(latitude: Double, longitude: Double) {
        val notification = NotificationCompat.Builder(this, locationChannelId)
            .setContentTitle("Location Update")
            .setContentText("Latitude: $latitude, Longitude: $longitude")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }
}
