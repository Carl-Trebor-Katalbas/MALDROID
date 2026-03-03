package com.app.maldroid.homeactivities

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.maldroid.data.model.ApkScanResult
import com.app.maldroid.R
import java.text.SimpleDateFormat
import java.util.*

class ApkScanResultsAdapter(private val items: List<ApkScanResult>) :
    RecyclerView.Adapter<ApkScanResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val apkFileName: TextView = itemView.findViewById(R.id.apkFileNameText)
        val scanResult: TextView = itemView.findViewById(R.id.scanResultText)
        val scanDate: TextView = itemView.findViewById(R.id.scanDateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_apk_scan_result, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.apkFileName.text = "Scanned \"${item.fileName}\""
        holder.scanResult.text = item.scanResult


        val dateFormat = SimpleDateFormat("MM/dd/yy, hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(item.scanTimestamp))
        holder.scanDate.text = dateString


        val textColor = if (item.isSafe) {
            ContextCompat.getColor(holder.itemView.context, R.color.green)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.red)
        }
        holder.scanResult.setTextColor(textColor)
    }

    override fun getItemCount(): Int = items.size
}