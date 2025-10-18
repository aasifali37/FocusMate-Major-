package com.example.focusmate_draft1

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OverlayBlockActivity : AppCompatActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent, non-touchable overlay
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.activity_overlay_block)

        // Optional toast for package being blocked
        val packageName = intent.getStringExtra("packageName")
        if (packageName != null) {
            Toast.makeText(this, "Blocking: $packageName", Toast.LENGTH_SHORT).show()
        }

        // Register to close overlay via broadcast
        val intentFilter = IntentFilter("com.example.focusmate_draft1.ACTION_CLOSE_OVERLAY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(closeReceiver, intentFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Use OnBackPressedDispatcher instead")
    override fun onBackPressed() {
        // Block back navigation
        Toast.makeText(this, "Blocking in progress, can't go back", Toast.LENGTH_SHORT).show()
    }

}
