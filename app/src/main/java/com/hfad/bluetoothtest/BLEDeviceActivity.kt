package com.hfad.bluetoothtest

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BLEDeviceActivity : AppCompatActivity() {

    private lateinit var connectBtn: Button
    private lateinit var bleDeviceInfo: TextView
    private lateinit var deviceList: RecyclerView
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private val devices = mutableListOf<BluetoothDevice>()

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device)

        bleDeviceInfo = findViewById(R.id.ble_device_info)
        connectBtn = findViewById(R.id.ConnectBtn)
        deviceList = findViewById(R.id.device_list)
        deviceList.layoutManager = LinearLayoutManager(this)

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        val adapter = BLEDeviceAdapter(devices) { device ->
            connectToDevice(device)
        }
        deviceList.adapter = adapter

        connectBtn.setOnClickListener {
            if (checkPermissions()) {
                startScan(adapter)
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this , Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            // For older Android versions
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // For Android 11 and below
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startScan(adapter: BLEDeviceAdapter) {
        if (!checkPermissions()) {
            Toast.makeText(
                this,
                "Bluetooth permissions are required to scan",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        devices.clear()
        bluetoothLeScanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    if (!devices.contains(device)) {
                        devices.add(device)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLEDeviceActivity", "Scan failed with error: $errorCode")
            }
        })
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!checkPermissions()) {
            Toast.makeText(
                this,
                "Bluetooth permissions are required to connect",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        bluetoothLeScanner.stopScan(object : ScanCallback() {})
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        displayDeviceInfo(device)
    }

    private fun displayDeviceInfo(device: BluetoothDevice) {
        val deviceName = device.name ?: "Unknown Device"
        val deviceAddress = device.address
        bleDeviceInfo.text = "Name: $deviceName\nAddress: $deviceAddress\n\nConnecting..."
        Log.d("BLEDeviceActivity", "Device Name: $deviceName, Address: $deviceAddress")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothAdapter.STATE_CONNECTED) {
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val services = gatt?.services
            runOnUiThread {
                displayServicesAndCharacteristics(services)
            }
        }

        private fun displayServicesAndCharacteristics(services: List<BluetoothGattService>?) {
            val stringBuilder = StringBuilder()

            services?.forEach { service ->
                stringBuilder.append("Service: ${service.uuid}\n")
                service.characteristics.forEach { characteristic ->
                    stringBuilder.append("\tCharacteristic: ${characteristic.uuid}\n")
                }
            }

            bleDeviceInfo.append("\nServices and Characteristics:\n$stringBuilder")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permissions denied. App functionality may be limited.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}