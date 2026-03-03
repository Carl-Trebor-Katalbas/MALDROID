package com.app.maldroid

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.app.maldroid.settingsactivities.Privacy
//import com.app.maldroid.settingsactivities.ScanOptions
import com.app.maldroid.R
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        val scanOptionsBtn = view.findViewById<Button>(R.id.scanOptions)
//        scanOptionsBtn.setOnClickListener {
//            val intent = Intent(requireContext(), ScanOptions::class.java)
//            startActivity(intent)
//
//
//        }
        val privacyBtn = view.findViewById<Button>(R.id.privacy)
        privacyBtn.setOnClickListener {
            val intent = Intent(requireContext(), Privacy::class.java)
            startActivity(intent)
        }
    }
}
