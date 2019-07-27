package com.example.moonshot

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.list_item.view.*

class BLEAdapter(private val context: Context, private val listener: OnDeviceClickListener) :
    RecyclerView.Adapter<BLEAdapter.ViewHolder>() {

    var bleList: List<BluetoothDevice> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bleList[position])
    }


    override fun getItemCount(): Int = bleList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val device = bleList[adapterPosition]
            listener.onDeviceClicked(device)
        }

        fun bind(ble: BluetoothDevice) {
            itemView.dName.text = ble.name
//            itemView.dAddress.text = ble.address
        }

    }

    interface OnDeviceClickListener {
        fun onDeviceClicked(device: BluetoothDevice)
    }
}