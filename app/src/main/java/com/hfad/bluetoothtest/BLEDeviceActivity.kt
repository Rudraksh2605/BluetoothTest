package com.hfad.bluetoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device)

        bleDeviceInfo = findViewById(R.id.ble_device_info)
        connectBtn = findViewById(R.id.ConnectBtn)
        deviceList = findViewById(R.id.device_list)
        deviceList.layoutManager = LinearLayoutManager(this)

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Set up RecyclerView adapter
        val adapter = BLEDeviceAdapter(devices) { device ->
            connectToDevice(device)
        }
        deviceList.adapter = adapter

        connectBtn.setOnClickListener {
            startScan(adapter)
        }
    }

    private fun startScan(adapter: BLEDeviceAdapter) {
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
        bluetoothLeScanner.stopScan(object : ScanCallback() {})
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        displayDeviceInfo(device)  // Display device info when connecting
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
}
