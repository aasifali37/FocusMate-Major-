package com.example.focusmate_draft1

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.focusmate_draft1.FlashcardsFragment
import com.example.focusmate_draft1.ui.ProfileFragment


class NavigationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        setupNavigation(bottomNav)

        // Load the default fragment (FlashcardsFragment)
        if (savedInstanceState == null) {
            //loadFragment(MonitorFragment(),"MONITOR")
            loadFragment(FlashcardsFragment())
        }


        checkPermissions()
    }

    private fun setupNavigation(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            val fragmentTag = when (item.itemId) {
                R.id.nav_flashcards -> "FLASHCARDS"
                R.id.nav_monitor -> "MONITOR"
                R.id.nav_profile -> "PROFILE"
                else -> "FLASHCARDS"
            }

            val currentFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            if (currentFragment == null) {
                val fragment = when (item.itemId) {
                    R.id.nav_flashcards -> FlashcardsFragment()
                    R.id.nav_monitor -> MonitorFragment()
                    R.id.nav_profile -> ProfileFragment()
                    else -> FlashcardsFragment()
                }
                loadFragment(fragment)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        println("NavigationActivity: Loading fragment - ${fragment.javaClass.simpleName}") // ✅ Debug log
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }



    private fun checkPermissions() {
        if (!hasUsageAccess()) {
            Toast.makeText(this, "Grant usage access for monitoring", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun hasUsageAccess(): Boolean {
        // Check if USAGE_STATS permission is granted
        try {
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
