package com.krri.uwb

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BeaconData(
    val bluetoothDevice: BluetoothDevice,
    val id: String,
    val rssi: Int?,
    val distance: Double,
) : Parcelable
