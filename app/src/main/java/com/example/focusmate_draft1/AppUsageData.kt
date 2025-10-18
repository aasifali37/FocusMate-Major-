package com.example.focusmate_draft1

import android.graphics.drawable.Drawable

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val lastTimeUsed: String,
    val usageTimeMillis: Long,
    val icon: Drawable?,
    var isMonitored: Boolean = false,
    var usageLimitMillis: Long = 0L // 👈 Add this line for limit tracking
)
