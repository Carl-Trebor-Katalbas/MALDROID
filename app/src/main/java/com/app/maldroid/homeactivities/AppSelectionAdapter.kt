//package com.app.maldroid.homeactivities
//
//import android.content.pm.ApplicationInfo
//import android.content.pm.PackageManager
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Filter
//import android.widget.Filterable
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.app.maldroid.R
//
//class AppSelectionAdapter(
//    private var apps: List<ApplicationInfo>,
//    private val packageManager: PackageManager,
//    private val onAppSelected: (ApplicationInfo) -> Unit
//) : RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>(), Filterable {
//
//    private var filteredApps: List<ApplicationInfo> = apps
//
//    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
//        val appName: TextView = itemView.findViewById(R.id.appName)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_app_selection, parent, false)
//        return AppViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
//        val appInfo = filteredApps[position]
//
//        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager))
//        holder.appName.text = appInfo.loadLabel(packageManager)
//
//        holder.itemView.setOnClickListener {
//            onAppSelected(appInfo)
//        }
//    }
//
//    override fun getItemCount(): Int = filteredApps.size
//
//    override fun getFilter(): Filter {
//        return object : Filter() {
//            override fun performFiltering(constraint: CharSequence?): FilterResults {
//                val filteredList = mutableListOf<ApplicationInfo>()
//
//                if (constraint.isNullOrBlank()) {
//                    filteredList.addAll(apps)
//                } else {
//                    val filterPattern = constraint.toString().lowercase().trim()
//                    apps.forEach { app ->
//                        val appLabel = app.loadLabel(packageManager).toString().lowercase()
//                        if (appLabel.contains(filterPattern)) {
//                            filteredList.add(app)
//                        }
//                    }
//                }
//
//                val results = FilterResults()
//                results.values = filteredList
//                return results
//            }
//
//            @Suppress("UNCHECKED_CAST")
//            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
//                filteredApps = results?.values as? List<ApplicationInfo> ?: emptyList()
//                notifyDataSetChanged()
//            }
//        }
//    }
//
//    fun updateList(newApps: List<ApplicationInfo>) {
//        apps = newApps
//        filteredApps = newApps
//        notifyDataSetChanged()
//    }
//}