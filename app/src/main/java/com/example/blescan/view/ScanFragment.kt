package com.example.blescan.view

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blescan.R
import com.example.blescan.ble.OnDeviceSelectedCallBack
import com.example.blescan.ble.ScanResultAdapter
import kotlinx.android.synthetic.main.scan_device_frg.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScanFragment : Fragment() {
    private lateinit var mContext: Context
    private lateinit var mRootView: View
    private var isScanning = false
        set(value) {
            field = value
            scan_ble_button.text = if (value) "Stop Scan" else "Start Scan"
        }
    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(data = scanResults, callBack = onDeviceSelectedCallBack)

    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    val isLocationPermissionGranted
        get() = context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    val filter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid.fromString(MainActivity.ENVIRONMENTAL_SERVICE_UUID.toString())
    ).build()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context;
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mRootView = inflater.inflate(R.layout.scan_device_frg, container, false)
        return mRootView
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        val llm = LinearLayoutManager(mContext)
        llm.orientation = LinearLayoutManager.VERTICAL
        list_ble_device.layoutManager = llm
        list_ble_device.adapter = scanResultAdapter
        scan_ble_button.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.d(
                        "ScanCallback",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                    )
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    private fun startBleScan() {
        if (!isLocationPermissionGranted!!) {
            requestLocationPermission()
        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }


    private fun requestLocationPermission() {
        if (isLocationPermissionGranted!!) {
            return
        }
        requestPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            MainActivity.LOCATION_PERMISSION_REQUEST_CODE
        )
    }


    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, MainActivity.ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MainActivity.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MainActivity.ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    private val onDeviceConnectCallback: OnDeviceSelectedCallBack =
        object : OnDeviceSelectedCallBack {
            override fun callBackDeviceDetails(item: ScanResult) {
                if (isScanning) {
                    stopBleScan()
                }

            }

        }

    private val onDeviceSelectedCallBack: OnDeviceSelectedCallBack =
        object : OnDeviceSelectedCallBack {
            override fun callBackDeviceDetails(item: ScanResult) {
                Log.d(MainActivity.TAG, "nCallBack: " + item.device.address)
                fragmentManager?.beginTransaction()
                    ?.add(
                        R.id.frame, DeviceDetailsFragment.newInstance(
                            onDeviceConnectCallback, item
                        )
                    )
                    ?.commitNow()
            }

        }

    companion object {
        fun newInstance() = ScanFragment()
    }
}