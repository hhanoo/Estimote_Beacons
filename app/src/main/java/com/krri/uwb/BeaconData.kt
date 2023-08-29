package com.krri.uwb

data class BeaconData(
    val id: String,
    val rssi: Int?,
    val distance: Double,
)
