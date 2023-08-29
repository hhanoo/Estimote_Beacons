package com.krri.uwb

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import com.estimote.uwb.api.EstimoteUWBFactory
import com.estimote.uwb.api.EstimoteUWBManager
import com.estimote.uwb.api.scanning.EstimoteUWBScanResult
import com.krri.uwb.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: BeaconListAdapter
    private lateinit var uwbManager: EstimoteUWBManager

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<BeaconData>() {
            override fun areItemsTheSame(oldItem: BeaconData, newItem: BeaconData): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: BeaconData, newItem: BeaconData): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Request necessary permissions
        requestPermissions(
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.UWB_RANGING
            ),
            1
        )

        // Initialize UWB Manager
        uwbManager = EstimoteUWBFactory.create()

        // Start device scanning
        startDeviceScanning()

        // Initialize the BeaconListAdapter
        listAdapter = BeaconListAdapter(diffUtil)
        binding.list.adapter = listAdapter
    }

    private fun startDeviceScanning() {
        // Start UWB device scanning
        uwbManager.startDeviceScanning(this)

        // Collect and process scanned UWB devices
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            uwbManager.uwbDevices.collect { scanResult: EstimoteUWBScanResult ->
                when (scanResult) {
                    is EstimoteUWBScanResult.Devices -> {
                        val tempList = arrayListOf<BeaconData>()
                        for (device in scanResult.devices) {
                            // Calculate distance based on RSSI value
                            val distance = round((10.0).pow((-76.0 - device.rssi!!) / 3.0) * 10) / 10

                            // Create BeaconData instance and add to tempList
                            tempList.add(
                                BeaconData(
                                    id = device.deviceId,
                                    rssi = device.rssi,
                                    distance = distance
                                )
                            )
                        }

                        // Submit the scanned data to the adapter
                        listAdapter.submitList(tempList)
                    }

                    is EstimoteUWBScanResult.Error -> {
                        Log.e("UWBScan", "Scan Error: ${scanResult.errorCode}")
                        // Handle scan error
                    }

                    is EstimoteUWBScanResult.ScanNotStarted -> {
                        Log.d("UWBScan", "Scan Not Started")
                        // Handle case when scanning is not started
                    }
                }
            }
        }
    }
}