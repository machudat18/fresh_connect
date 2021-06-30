package com.dat.fresh_connect.ble

import android.bluetooth.le.ScanResult

interface OnDeviceSelectedCallBack {
    fun callBackDeviceDetails(item : ScanResult)
}