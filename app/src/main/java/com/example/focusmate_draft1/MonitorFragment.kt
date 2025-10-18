package com.example.focusmate_draft1

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Comparator
import kotlin.math.log

class MonitorFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var packageManagerInstance: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var appUsageAdapter: AppUsageAdapter
    private lateinit var typeSpinner: Spinner
    private val monitorScope = CoroutineScope(Dispatchers.Main + Job())

    private val DISPLAY_ORDER_USAGE_TIME = 0
    private val DISPLAY_ORDER_LAST_TIME_USED = 1
    private val DISPLAY_ORDER_APP_NAME = 2

    // Define the permission launcher at class level
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, start service
            startMonitoringServiceviaPermission()
        } else {
            Log.d("Notification","Notification permission denied!!")
            // Permission denied, handle accordingly
            // Maybe show a dialog explaining why notification is important
        }
    }

    // Comparators for different sorting methods
    class AppNameComparator(private val appLabelList: Map<String, String>) : Comparator<UsageStats> {
        override fun compare(a: UsageStats, b: UsageStats): Int {
            val aLabel = appLabelList[a.packageName] ?: a.packageName
            val bLabel = appLabelList[b.packageName] ?: b.packageName
            return aLabel.compareTo(bLabel)
        }
    }

    object LastTimeUsedComparator : Comparator<UsageStats> {
        override fun compare(a: UsageStats, b: UsageStats): Int {
            return b.lastTimeUsed.compareTo(a.lastTimeUsed)
        }
    }

    object UsageTimeComparator : Comparator<UsageStats> {
        override fun compare(a: UsageStats, b: UsageStats): Int {
            return b.totalTimeInForeground.compareTo(a.totalTimeInForeground)
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = milliseconds / (1000 * 60)
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            "${hours}h ${remainingMinutes}m"
        } else {
            "${minutes}m"
        }
    }

    private fun startMonitoringLoop() {
        monitorScope.launch {
            while (isActive) {
                checkMonitoredApps()
                delay(30 * 1000) // Every 30 seconds
            }
        }
    }

    private fun triggerTemporaryBlock(packageName: String) {
        // Start an activity or dialog with a transparent overlay
        val intent = Intent(requireContext(), OverlayBlockActivity::class.java)
        intent.putExtra("packageName", packageName)
        startActivity(intent)
    }

    private fun checkMonitoredApps() {
        Log.d("Monitor", "Checking apps for usage limits...")

        val monitoredApps = appUsageAdapter.getMonitoredApps()

        monitoredApps.forEach { app ->
            if (app.usageTimeMillis >= app.usageLimitMillis) {
                triggerTemporaryBlock(app.packageName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_monitor, container, false)

        packageManagerInstance = requireActivity().packageManager
        usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Initialize the views
        typeSpinner = view.findViewById(R.id.typeSpinner)
        typeSpinner.onItemSelectedListener = this

        // Setup RecyclerView instead of ListView
        recyclerView = view.findViewById(R.id.pkg_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    private fun startMonitoringServiceviaPermission() {
        // Check and request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= 33) { // Android 13 (TIRAMISU)
            // Use string value instead of the constant for backward compatibility
            val postNotificationPermission = "android.permission.POST_NOTIFICATIONS"

            // Check if we already have notification permission
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    postNotificationPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Launch the permission request
                requestPermissionLauncher.launch(postNotificationPermission)
                return
            }
        }
        //Request Overlay, Notification, and UsageAccess Permissions at the beginning of the Fragment screen
        requestPermissions()
        // Start the service directly if permission is already granted or not needed
        startMonitoringService()
    }

    private fun startMonitoringService() {

        val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startMonitoringButton = view.findViewById<Button>(R.id.startMonitoringButton)
        startMonitoringButton.setOnClickListener {
            if (hasRequiredPermissions()) {
                startMonitoringService()
            } else {
                // Always funnel through this method, which will now:
                // 1) Request POST_NOTIFICATIONS (API 33+) if needed
                // 2) Or immediately call startMonitoringService()
                startMonitoringServiceviaPermission()
            }
        }

        val testBlockButton = view.findViewById<Button>(R.id.testBlockButton)
        testBlockButton.setOnClickListener {
            val intent = Intent(requireContext(), OverlayBlockActivity::class.java)
            intent.putExtra("packageName", "com.example.test")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireContext().startActivity(intent)
        }

        // Add refresh button functionality
        view.findViewById<View>(R.id.refreshButton)?.setOnClickListener {
            if (::appUsageAdapter.isInitialized) {
                refreshStats()
            } else if (hasUsageStatsPermission()) {
                initializeAdapter()
                startMonitoringLoop() // Also start loop if just got permission
            } else {
                requestPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        if (hasUsageStatsPermission()) {
            if (::appUsageAdapter.isInitialized) {
                refreshStats()
            } else {
                initializeAdapter()
            }
        }
    }

    private fun initializeAdapter() {
        val appUsageDataList = mutableListOf<AppUsageData>()
        appUsageAdapter = AppUsageAdapter(requireContext(), appUsageDataList)
        recyclerView.adapter = appUsageAdapter

        // Set the LimitSetter interface to save the limit when it's set
        appUsageAdapter.setLimitSetter(object : LimitSetter {
            override fun onLimitSet(packageName: String, limitMillis: Long) {
                // This will call saveLimit() method in MonitorFragment
                saveLimit(packageName, limitMillis)
            }
        })

        // Fetch and populate app usage data
        loadAppUsageData()
    }


    interface LimitSetter {
        fun onLimitSet(packageName: String, limitMillis: Long)
    }

    fun saveLimit(packageName: String, limit: Long) {
        Log.d("LimitDebug", "Saving limit for $packageName: $limit ms")
        val sharedPreferences = requireContext().getSharedPreferences("AppLimits", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong(packageName, limit)
        editor.apply()
    }

    private fun loadLimit(packageName: String): Long {
        val sharedPreferences = requireContext().getSharedPreferences("AppLimits", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(packageName, 0) // Default to no limit
    }



    private fun loadAppUsageData() {
        val appUsageDataList = mutableListOf<AppUsageData>()
        val appLabelMap = mutableMapOf<String, String>()
        val packageStats = mutableListOf<UsageStats>()

        // Get stats for the last 5 days
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)

        val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            cal.timeInMillis,
            System.currentTimeMillis()
        )

        if (stats != null) {
            val map = mutableMapOf<String, UsageStats>()
            stats.forEach { pkgStats ->
                if (pkgStats.totalTimeInForeground > 0) {
                    try {
                        val appInfo: ApplicationInfo = packageManagerInstance
                            .getApplicationInfo(pkgStats.packageName, 0)
                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        // Only include user-installed apps
                        if (!isSystemApp) {
                            val label = appInfo.loadLabel(packageManagerInstance).toString()
                            appLabelMap[pkgStats.packageName] = label

                            // Aggregate stats for the same package
                            val existingStats = map[pkgStats.packageName]
                            if (existingStats == null) {
                                map[pkgStats.packageName] = pkgStats
                            } else {
                                existingStats.add(pkgStats)
                            }
                        }

                    } catch (e: PackageManager.NameNotFoundException) {
                        // Package may be gone
                        Log.w(TAG, "App not found for package: ${pkgStats.packageName}", e)
                    }
                }
            }
            packageStats.addAll(map.values)
        }

        // Sort by usage time by default
        packageStats.sortWith(UsageTimeComparator)

        // Convert to AppUsageData objects for the adapter
        packageStats.forEach { stats ->
            try {
                val appInfo = packageManagerInstance.getApplicationInfo(stats.packageName, 0)
                val appIcon = packageManagerInstance.getApplicationIcon(stats.packageName)
                val appName = appLabelMap[stats.packageName] ?: stats.packageName

                val lastTimeUsedFormatted = DateUtils.formatSameDayTime(
                    stats.lastTimeUsed,
                    System.currentTimeMillis(),
                    DateFormat.MEDIUM,
                    DateFormat.MEDIUM
                ).toString()

                // Load the limit for this app
                val limit = loadLimit(stats.packageName)

                val appUsage = AppUsageData(
                    packageName = stats.packageName,
                    appName = appName,
                    usageTimeMillis = stats.totalTimeInForeground,
                    lastTimeUsed = lastTimeUsedFormatted,
                    icon = appIcon,
                    usageLimitMillis = limit, // ✅ Use the loaded limit
                    isMonitored = limit > 0   // ✅ Automatically mark as monitored if limit exists
                )
                appUsageDataList.add(appUsage)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Failed to get app info for ${stats.packageName}", e)
            }
        }

        // Update the adapter with the new data
        appUsageAdapter.setAppUsageData(appUsageDataList)

        // Show message if no stats are available
        if (appUsageDataList.isEmpty()) {
            activity?.runOnUiThread {
                Toast.makeText(context, "No usage data available or permission not granted",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshStats() {

        loadAppUsageData()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (::appUsageAdapter.isInitialized) {
            when (position) {
                DISPLAY_ORDER_USAGE_TIME -> appUsageAdapter.sortByUsageTime()
                DISPLAY_ORDER_LAST_TIME_USED -> appUsageAdapter.sortByLastTimeUsed()
                DISPLAY_ORDER_APP_NAME -> appUsageAdapter.sortByAppName()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // do nothing
    }

    private fun hasUsageStatsPermission(): Boolean {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 60 * 60 * 24,
            System.currentTimeMillis()
        )
        return !stats.isNullOrEmpty()
    }

    private fun hasRequiredPermissions(): Boolean {
        return hasUsageStatsPermission() && Settings.canDrawOverlays(requireContext())
    }

    private val usageStatsSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Called when the user returns from the Usage Access settings screen
        if (hasUsageStatsPermission()) {
            Toast.makeText(context, "Usage access granted", Toast.LENGTH_SHORT).show()
            initializeAdapter()
        } else {
            Toast.makeText(context, "Usage access not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(requireContext())) {
            // Overlay permission granted, you can now proceed with showing the overlay
            Toast.makeText(requireContext(), "Overlay permission granted!", Toast.LENGTH_SHORT).show()
            // You might want to trigger the overlay display here or set a flag
        } else {
            // Overlay permission denied, inform the user
            Toast.makeText(
                requireContext(),
                "Overlay permission is required to block apps.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestPermissions() {
        val context = requireContext()

        // 1️⃣ Usage-stats permission
        if (!hasUsageStatsPermission()) {
            Toast.makeText(
                context,
                "Please grant usage access permission for this app to work correctly",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsSettingsLauncher.launch(intent)
        }

        // Check if Overlay permission is granted
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                "Please allow overlay permission to block apps when overused",
                Toast.LENGTH_LONG
            ).show()
            // Start activity using ActivityResultLauncher
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            requestOverlayPermissionLauncher.launch(intent)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_USAGE_STATS_PERMISSION -> {
                if (hasUsageStatsPermission()) {
                    // If usage stats permission is granted, proceed
                    Toast.makeText(requireContext(), "Usage stats permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // If not granted, notify the user
                    Toast.makeText(requireContext(), "Usage stats permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(requireContext())) {
                    // If overlay permission is granted, proceed
                    Toast.makeText(requireContext(), "Overlay permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // If not granted, notify the user
                    Toast.makeText(requireContext(), "Overlay permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // Request codes for identifying the requests
    companion object {
        private const val REQUEST_USAGE_STATS_PERMISSION = 1001
        private const val REQUEST_OVERLAY_PERMISSION = 1002
        private const val TAG = "MonitorFragment"
    }
}