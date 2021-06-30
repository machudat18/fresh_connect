package com.dat.fresh_connect.view

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.dat.fresh_connect.R
import com.dat.fresh_connect.ble.OnDeviceSelectedCallBack
import com.dat.fresh_connect.ble.ServiceModel
import kotlinx.android.synthetic.main.details_device_frg.*


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DeviceDetailsFragment(
    private val mCallBack: OnDeviceSelectedCallBack,
    private val item: ScanResult
) : Fragment() {
    private lateinit var mContext: Context
    private lateinit var mRootView: View
    private var connected = false
        set(value) {
            field = value
            runOnUiThread {
                button_connect.text =
                    if (value) "Ngắt kết nối với thiết bị" else "Kết nối với thiết bị"
            }
        }
    private lateinit var listService: MutableList<ServiceModel>
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context;
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mRootView = inflater.inflate(R.layout.details_device_frg, container, false)
        return mRootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        listenToBondStateChanges(mContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        mContext.applicationContext.unregisterReceiver(
            broadcastReceiver
        )
    }

    private fun initView() {

        device_details_name.text = "Tên thiết bị : " + (item.device.name ?: "unamed")
        device_details_address.text = "Địa chỉ của thiết bị :" + item.device.address.toString()
        close.setOnClickListener {
            fragmentManager?.beginTransaction()
                ?.remove(
                    this
                )
                ?.commitNow()
        }
        button_connect.setOnClickListener {
            mCallBack.callBackDeviceDetails(item)
            with(item.device) {
                Log.w("ScanResultAdapter", "Connecting to $address")
                runOnUiThread {
                    button_connect.text = "Đang kết nối tới $address"
                }
                if (connected) {
                } else {
                    connectGatt(context, false, gattCallback)

                }

            }

        }
    }

    private fun listenToBondStateChanges(context: Context) {
        context.applicationContext.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            with(intent) {
                action?.let { Log.d("test", it) }
                if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val device = getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val previousBondState = getIntExtra(
                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                        -1
                    )
                    val bondState = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val bondTransition = "${previousBondState.toBondStateDescription()} to " +
                            bondState.toBondStateDescription()
                    Log.w(
                        "Bond state change",
                        "${device?.address} bond state changed | $bondTransition"
                    )
                }
            }
        }

        private fun Int.toBondStateDescription() = when (this) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            BluetoothDevice.BOND_NONE -> "NOT BONDED"
            else -> "ERROR: $this"
        }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            Log.d("gatt", status.toString())
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                    Log.d("BluetoothGattCallback", "Trying to pair with: ${gatt.device.address}")
                    gatt.device.createBond()
                    connected = true
                    // TODO: Store a reference to BluetoothGatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    connected = false
                    gatt.close()
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                connected = false
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w(
                    "BluetoothGattCallback",
                    "Discovered ${services.size} services for ${device.address}"
                )
                getGattServiceDetails() // See implementation just above this section
                // Consider connection setup as complete here
            }
        }
    }

    private fun BluetoothGatt.getGattServiceDetails() {
        val listService = StringBuilder()
        if (services.isEmpty()) {
//           listService.add(
//               ServiceModel(
//                   "Không có service",
//                   null
//               )
//           )
            Log.i(
                "printGattTable",
                "No service and characteristic available, call discoverServices() first?"
            )
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n",
            ) { it.uuid.toString() }
//            listService.add(
//                ServiceModel(
//                    service = "Service : \n" + service.uuid,
//                    listOfCharacteristics =  + characteristicsTable
//                    )
//
//            )
            listService.append("Service\n ${service.uuid}\nCharacteristics:\n$characteristicsTable\n\n")

            Log.i(
                "printGattTable",
                "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
        runOnUiThread { all_service.text = listService }

    }

    companion object {
        fun newInstance(
            mCallBack: OnDeviceSelectedCallBack,
            item: ScanResult
        ) = DeviceDetailsFragment(mCallBack, item)
    }
}