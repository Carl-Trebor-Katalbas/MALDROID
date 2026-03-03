package com.app.maldroid.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.app.maldroid.MainActivity
import com.app.maldroid.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "MaldroidPrefs"
        private const val KEY_ONBOARDING_FINISHED = "is_onboarding_finished"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isOnboardingFinished()) {
            navigateToHome()
            return
        }

        setContentView(R.layout.activity_splash)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {

            markOnboardingFinished()
            navigateToHome()
        }
    }

    private fun isOnboardingFinished(): Boolean {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_ONBOARDING_FINISHED, false)
    }

    private fun markOnboardingFinished() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(KEY_ONBOARDING_FINISHED, true)
            apply()
        }
    }
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}