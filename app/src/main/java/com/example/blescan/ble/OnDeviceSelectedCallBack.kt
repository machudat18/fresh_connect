package com.example.blescan.ble

import android.bluetooth.le.ScanResult

interface OnDeviceSelectedCallBack {
    fun callBackDeviceDetails(item : ScanResult)
}