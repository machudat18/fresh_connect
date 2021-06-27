package com.example.blescan.ble

import android.bluetooth.le.ScanResult
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.blescan.R

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScanResultAdapter(
    private val data: MutableList<ScanResult> = mutableListOf(),
    private val callBack: OnDeviceSelectedCallBack
) : RecyclerView.Adapter<ScanResultAdapter.ItemDataHolder>() {
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemDataHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ble_device_info_item, parent, false)
        return ItemDataHolder(view)
    }


    override fun onBindViewHolder(holder: ItemDataHolder, position: Int) {
        val item = data[position]
        holder.tvName.text = "Tên thiết bị : " + (item.device.name ?: "unamed")
        holder.tvName.tag = item
        holder.tvUUID.text = "Adress : " + item.device.address.toString()

    }

    override fun getItemCount() = data.size

    inner class ItemDataHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.device_name_item)
        val tvUUID: TextView = view.findViewById(R.id.device_uuid_item)

        init {
            view.setOnClickListener {
                callBack.callBackDeviceDetails(tvName.tag as ScanResult)
            }
        }
    }
}