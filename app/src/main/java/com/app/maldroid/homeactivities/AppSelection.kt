//package com.app.maldroid.homeactivities
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.content.pm.ApplicationInfo
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.app.maldroid.MainActivity
//import com.app.maldroid.R
//
//class AppSelection : AppCompatActivity() {
//
//    private lateinit var selectedAppText: TextView
//    private lateinit var selectedAppLayout: LinearLayout
//    private lateinit var selectedAppIcon: ImageView
//    private lateinit var selectedAppName: TextView
//    private lateinit var scanResultText: TextView
//    private lateinit var selectAppButton: Button
//    private lateinit var scanAppButton: Button
//    private lateinit var backButton: ImageButton
//
//    private var selectedApp: ApplicationInfo? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_app_selection)
//
//        initializeViews()
//        setupClickListeners()
//        disableScanButton()
//    }
//
//    private fun initializeViews() {
//        selectedAppText = findViewById(R.id.selectedAppText)
//        selectedAppLayout = findViewById(R.id.selectedAppLayout)
//        selectedAppIcon = findViewById(R.id.selectedAppIcon)
//        selectedAppName = findViewById(R.id.selectedAppName)
//        scanResultText = findViewById(R.id.scanResultText)
//        selectAppButton = findViewById(R.id.selectAppButton)
//        scanAppButton = findViewById(R.id.scanAppButton)
//        backButton = findViewById(R.id.backButton)
//    }
//
//    private fun setupClickListeners() {
//        selectAppButton.setOnClickListener {
//            showAppSelectionDialog()
//        }
//
//        backButton.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            startActivity(intent)
//            finish()
//        }
//
//        scanAppButton.setOnClickListener {
//            selectedApp?.let { app ->
//                scanSingleApp(app)
//            } ?: run {
//                Toast.makeText(this, "Please select an app first", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun showAppSelectionDialog() {
//        val packageManager = packageManager
//        val apps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
//        } else {
//            @Suppress("DEPRECATION")
//            packageManager.getInstalledApplications(0)
//        }
//
//        val userApps = apps.filter {
//            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
//        }.sortedBy { it.loadLabel(packageManager).toString() }
//
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_selection, null)
//        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.appsRecyclerView)
//        val searchView = dialogView.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
//
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Select an App to Scan")
//            .setView(dialogView)
//            .setNegativeButton("Cancel") { d, _ ->
//                d.dismiss()
//            }
//            .create()
//
//        // Set dialog size
//        dialog.window?.setLayout(
//            (resources.displayMetrics.widthPixels * 0.9).toInt(),
//            (resources.displayMetrics.heightPixels * 0.8).toInt()
//        )
//        val adapter = AppSelectionAdapter(userApps, packageManager) { selectedAppInfo ->
//            selectedApp = selectedAppInfo
//
//            val appName = selectedAppInfo.loadLabel(packageManager).toString()
//            updateSelectedAppUI(selectedAppInfo, appName)
//            enableScanButton()
//            scanResultText.text = "Ready to scan: $appName"
//            dialog.dismiss()
//        }
//        recyclerView.adapter = adapter
//        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                adapter.filter.filter(newText)
//                return true
//            }
//        })
//
//        dialog.show()
//    }
//
//    private fun updateSelectedAppUI(appInfo: ApplicationInfo, appName: String) {
//        selectedAppText.visibility = View.GONE
//        selectedAppLayout.visibility = View.VISIBLE
//
//        selectedAppIcon.setImageDrawable(appInfo.loadIcon(packageManager))
//        selectedAppName.text = appName
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun scanSingleApp(appInfo: ApplicationInfo) {
//        try {
//            val appName = appInfo.loadLabel(packageManager).toString()
//
//            val intent = Intent(this, AppScanningProgress::class.java).apply {
//                putExtra("SELECTED_APP_INFO", appInfo) // Pass the selected app
//                putExtra("APP_NAME", appName)
//                putExtra("PACKAGE_NAME", appInfo.packageName)
//            }
//            startActivity(intent)
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Error starting scan: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun enableScanButton() {
//        scanAppButton.isEnabled = true
//        scanAppButton.setBackgroundResource(R.drawable.custom_button_green)
//        scanAppButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
//    }
//
//    private fun disableScanButton() {
//        scanAppButton.isEnabled = false
//        scanAppButton.setBackgroundResource(R.drawable.custom_button2)
//        scanAppButton.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
//    }
//}