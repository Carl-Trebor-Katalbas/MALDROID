package com.app.maldroid.homeactivities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.maldroid.R
import com.app.maldroid.data.model.ScanResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanResultsAdapter(
    private val items: List<ScanResultItem>,
    private val onItemClick: (ScanResultItem) -> Unit
) : RecyclerView.Adapter<ScanResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.appNameText)
        val scanResult: TextView = itemView.findViewById(R.id.scanResultText)
        val scanDate: TextView = itemView.findViewById(R.id.scanDateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scan_all_apk, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.appName.text = item.appName
        holder.scanResult.text = item.scanResult
        val dateFormat = SimpleDateFormat("MM/dd/yy, hh:mm a", Locale.getDefault())
        holder.scanDate.text = dateFormat.format(Date(item.scanTimestamp))
        val context = holder.itemView.context
        if (item.isSafe) {
            holder.scanResult.setTextColor(ContextCompat.getColor(context, R.color.green))
        } else {
            holder.scanResult.setTextColor(ContextCompat.getColor(context, R.color.red))
        }
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
    override fun getItemCount(): Int = items.size
}