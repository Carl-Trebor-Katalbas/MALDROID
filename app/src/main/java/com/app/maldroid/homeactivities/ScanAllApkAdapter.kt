package com.app.maldroid.homeactivities

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.maldroid.R
import com.app.maldroid.data.model.ScanResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanAllApkAdapter(private val items: List<ScanResultItem>) :
    RecyclerView.Adapter<ScanAllApkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appNameText)
        val scanResult: TextView = view.findViewById(R.id.scanResultText)
        val scanDate: TextView = view.findViewById(R.id.scanDateText)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_all_apk, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.appName.text = item.appName
        val durationInSeconds = item.scanDuration / 1000.0
        val formattedDuration = String.format(Locale.getDefault(), "%.2fs", durationInSeconds)

        if (item.isSafe) {
            holder.scanResult.text = "Safe • ${item.scanResult}"
            holder.scanResult.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.scanResult.text = "Threat Detected • ${item.scanResult}"
            holder.scanResult.setTextColor(Color.parseColor("#F44336"))
        }

        val date = Date(item.scanTimestamp)
        val format = SimpleDateFormat("MM/dd/yy, h:mm a", Locale.getDefault())
        holder.scanDate.text = "${format.format(date)} • $formattedDuration"
    }

    override fun getItemCount() = items.size
}