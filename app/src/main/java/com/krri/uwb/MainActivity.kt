package com.krri.uwb

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import com.estimote.uwb.api.EstimoteUWBFactory
import com.estimote.uwb.api.EstimoteUWBManager
import com.estimote.uwb.api.ranging.EstimoteUWBRangingResult
import com.estimote.uwb.api.scanning.EstimoteUWBScanResult
import com.krri.uwb.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: BeaconListAdapter
    private lateinit var uwbManager: EstimoteUWBManager
    private var isRangingStarted = false // Add this variable

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

        // Set click listener for RecyclerView items
        listAdapter.setOnItemClickListener(object : BeaconListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (!isRangingStarted) { // Check if ranging is not already started
                    val selectedItem = listAdapter.currentList[position]

                    // Stop device scanning
                    uwbManager.stopDeviceScanning()

                    // Connect to selected device
                    uwbManager.connect(selectedItem.bluetoothDevice, this@MainActivity)

                    // Set ranging started flag to true
                    isRangingStarted = true

                    startRanging()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        // Disconnect device and stop ranging when the activity is paused
        uwbManager.disconnectDevice()
        isRangingStarted = false
    }

    private fun startRanging() {
        // Collect ranging results after connecting
        lifecycleScope.launch {
            uwbManager.rangingResult.collect { result ->
                Log.e("TEST","result: ${result}")
                when (result) {
                    is EstimoteUWBRangingResult.Position -> {
                        Log.e("TEST","왜 돼?")
                        binding.deviceId.text = result.device.address.toString()
                        binding.deviceDistance.text = result.position.distance.toString()
                        binding.deviceAzimuth.text = result.position.azimuth.toString()
                        binding.deviceElevation.text = result.position.elevation.toString()
                    }
                    // Handle other ranging results if needed
                    else -> {
                        Log.e("TEST","왜 안돼?")
                        binding.deviceId.text = "N/A"
                        binding.deviceDistance.text = "N/A"
                        binding.deviceAzimuth.text = "N/A"
                        binding.deviceElevation.text = "N/A"
                    }
                }
            }
        }
    }

    private fun startDeviceScanning() {
        // Start UWB device scanning
        uwbManager.startDeviceScanning(this)

        // Collect and process scanned UWB devices
        lifecycleScope.launch {
            uwbManager.uwbDevices.collect { scanResult: EstimoteUWBScanResult ->
                when (scanResult) {
                    is EstimoteUWBScanResult.Devices -> {
                        val tempList = arrayListOf<BeaconData>()
                        for (device in scanResult.devices) {
                            // Calculate distance based on RSSI value
                            val distance =
                                round((10.0).pow((-76.0 - device.rssi!!) / 3.0) * 10) / 10

                            // Create BeaconData instance and add to tempList

                            device.device?.let {
                                tempList.add(
                                    BeaconData(
                                        bluetoothDevice = it,
                                        id = device.deviceId,
                                        rssi = device.rssi,
                                        distance = distance
                                    )
                                )
                            }
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