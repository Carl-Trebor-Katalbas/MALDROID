//package com.app.maldroid.homeactivities
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.app.maldroid.R
//import com.app.maldroid.data.model.ScanResultItem
//import java.text.SimpleDateFormat
//import java.util.*
//
//class ScannedApp(private val items: List<ScanResultItem>) :
//    RecyclerView.Adapter<ScannedApp.ViewHolder>() {
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val appName: TextView = itemView.findViewById(R.id.appNameText)
//        val scanDate: TextView = itemView.findViewById(R.id.scanDateText)
//        val scanResult: TextView = itemView.findViewById(R.id.scanResultText)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_scan_result, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = items[position]
//
//        holder.appName.text = item.appName
//        holder.scanResult.text = item.scanResult
//
//        val dateFormat = SimpleDateFormat("MM/dd/yy, hh:mm a", Locale.getDefault())
//        val dateString = dateFormat.format(Date(item.scanTimestamp))
//        holder.scanDate.text = dateString
//
//        val textColor = if (item.isSafe) {
//            ContextCompat.getColor(holder.itemView.context, R.color.green)
//        } else {
//            ContextCompat.getColor(holder.itemView.context, R.color.red)
//        }
//        holder.scanResult.setTextColor(textColor)
//    }
//
//    override fun getItemCount(): Int {
//        return items.size
//    }
//}