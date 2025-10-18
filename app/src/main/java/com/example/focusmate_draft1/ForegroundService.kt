package com.example.focusmate_draft1

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.*
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.focusmate_draft1.R
import java.util.concurrent.TimeUnit

class ForegroundService : Service() {

    companion object {
        private const val TAG = "ForegroundService"
        private const val MONITORING_INTERVAL_MS = 3000L
        private const val OVERLAY_DISPLAY_DURATION_MS = 60000L
        private const val NOTIFICATION_CHANNEL_ID = "FocusMateChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var blockView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usageStatsManager: UsageStatsManager
    private var isMonitoring = true
    private var isOverlayVisible = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sharedPreferences = getSharedPreferences("AppLimits", Context.MODE_PRIVATE)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForegroundService", "onStartCommand called with ID: $startId")
        startForeground(NOTIFICATION_ID, createNotification())
        //Log.d("ForegroundService", "Started in foreground with notification ID: $NOTIFICATION_ID")
        Log.d("FOREGROUNDSERVICE", "onStartCommand Called")
        Thread {
            while (true) {
                Log.d("FOREGROUNDSERVICE", "Logging Message")
                Thread.sleep(500)
            }
        }.start()
        startMonitoringLoop()

        return START_STICKY
    }

    private fun startMonitoringLoop() {
        handler.postDelayed({
            if (isMonitoring) {
                checkForegroundAppAndUsage()
                startMonitoringLoop()
            }
        }, MONITORING_INTERVAL_MS)
    }

    private fun checkForegroundAppAndUsage() {
        val foregroundPackage = getForegroundAppPackageName() ?: return
        val usageLimit = loadLimit(foregroundPackage)
        if (usageLimit <= 0) return

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)
        val usage = getAppUsage(foregroundPackage, startTime, endTime)

        Log.d(TAG, "App: $foregroundPackage, Used: $usage ms, Limit: $usageLimit ms")

        if (usage >= usageLimit) {
            showOverlay(foregroundPackage)
        } else {
            removeOverlay()
        }
    }


    private fun getForegroundAppPackageName(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 20000 // 2 seconds window
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var lastForegroundApp: String? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundApp = event.packageName
                Log.d(TAG, "Detected foreground app: $lastForegroundApp")
            }
        }
        if (lastForegroundApp == null) {
            Log.d(TAG, "No foreground app detected in the last 10 seconds")
        }

        return lastForegroundApp
    }

    private fun getPendingIntent(context: Context, packageName: String): PendingIntent {
        val intent = Intent(context, OverlayBlockActivity::class.java).apply {
            putExtra("packageName", packageName) // Pass the blocked app's package name
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createBlockNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val blockChannel = NotificationChannel(
                "BlockNotificationChannel",
                "App Blocking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000)
            }

            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(blockChannel)
        }
    }

    fun createUsageLimitExceededNotification(context: Context, pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, "FocusMateChannel")
            .setSmallIcon(R.drawable.ic_monitor) // Set an icon for the notification
            .setContentTitle("Usage Limit Exceeded")
            .setContentText("You have exceeded the usage limit for this app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true) // Attach the PendingIntent to the notification
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 1000)) // Optional: Add vibration for notification
            .setOngoing(true)
            .build()
    }

    private fun onUsageLimitExceeded(packageName: String) {
        Log.d(TAG, "Usage limit exceeded for $packageName, creating notification")

        // Step 1: Build PendingIntent for OverlayBlockActivity
        val pendingIntent = getPendingIntent(this, packageName)

        // Create a separate high-priority notification channel
        createBlockNotificationChannel(this)

        // Step 2: Build high-priority notification
        val notification = createUsageLimitExceededNotification(this, pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification) // Use a different ID from the foreground notification

        // Also start the activity directly
        val intent = Intent(this, OverlayBlockActivity::class.java).apply {
            putExtra("packageName", packageName) // Pass the blocked app's package name
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)

        // Step 3: Start the service in the foreground with the notification
        startForeground(1, notification) // Notification ID is 1
    }

    private fun getAppUsage(packageName: String, startTime: Long, endTime: Long): Long {
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var totalTime = 0L
        var lastTimestamp = 0L
        var isForeground = false

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastTimestamp = event.timeStamp
                    isForeground = true
                    Log.d(TAG, "$packageName moved to foreground at $lastTimestamp")
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (isForeground) {
                        val sessionTime = event.timeStamp - lastTimestamp
                        totalTime += event.timeStamp - lastTimestamp
                        Log.d(TAG, "$packageName moved to background, session time: $sessionTime ms")
                        isForeground = false
                    }
                }
            }
        }
        Log.d(TAG, "Total usage time for $packageName: $totalTime ms")
        return totalTime
    }




    private fun loadLimit(packageName: String): Long {
        return sharedPreferences.getLong(packageName, 0L)
    }

    private fun showOverlay(packageName: String) {
        Log.d(TAG, "Attempting to show overlay for $packageName")
        if (!canDrawOverlays(this)) {
            Log.e(TAG, "Cannot draw overlays - permission missing")
            return
        }

        /*if (isOverlayVisible){
            Log.d(TAG, "Overlay already visible, skipping")
            return
        }*/

        // Trigger the usage limit exceeded behavior
        Log.d(TAG, "Triggering usage limit exceeded behavior")
        onUsageLimitExceeded(packageName)
        isOverlayVisible = true

        // Automatically close the overlay after 30 seconds (or 1 min)
        val overlayDuration = 120 * 1000L // or use 60 * 1000L for 1 minute
        handler.postDelayed({
            Log.d(TAG, "Sending broadcast to close overlay")
            sendBroadcast(Intent("com.example.focusmate_draft1.ACTION_CLOSE_OVERLAY"))
            isOverlayVisible = false
        }, overlayDuration)
    }



    private fun removeOverlay() {
        try {
            blockView?.let {
                windowManager.removeView(it)
                blockView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Overlay removal failed", e)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FocusMate is Monitoring")
            .setContentText("Blocking overused apps.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FocusMate Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            Log.d(TAG, "Notification Manager: $manager")
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
