//package com.app.maldroid.settingsactivities
//
//import android.os.Bundle
//import android.widget.ImageView
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import com.app.maldroid.R
//
//class ScanOptions : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_scan_options)
//
//
//        val backArrow = findViewById<ImageView>(R.id.backArrow)
//        backArrow.setOnClickListener {
//            onBackPressedDispatcher.onBackPressed()
//        }
//    }
//}