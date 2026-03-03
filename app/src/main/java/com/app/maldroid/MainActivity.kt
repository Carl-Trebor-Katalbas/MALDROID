package com.app.maldroid
import android.annotation.SuppressLint
import android.content.Intent
import com.app.maldroid.R
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.app.maldroid.data.ScanDataManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavbar: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        requestAllFilesAccess()

        ScanDataManager.loadHistoryFromCache(this)
        bottomNavbar = findViewById(R.id.bottom_navbar)

        bottomNavbar.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId){
                R.id.home -> {
                    replacefragment(HomeFragment())
                    true
                }
                R.id.recentScans -> {
                    replacefragment(RecentScansFragment())
                    true
                }
                R.id.settings -> {
                    replacefragment(SettingsFragment())
                    true
                }
                else -> false
             }
        }
        replacefragment(HomeFragment())
    }
    private fun replacefragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.navbar_container,fragment).commit()
    }
    @SuppressLint("ObsoleteSdkInt")
    private fun requestAllFilesAccess() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("To delete malicious APKs from your Downloads folder, Maldroid needs 'All Files Access'.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}