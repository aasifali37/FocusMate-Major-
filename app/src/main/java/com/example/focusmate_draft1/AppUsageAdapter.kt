package com.example.focusmate_draft1

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

/*// Data class to hold app usage information
data class AppUsageData(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val lastTimeUsed: String,
    val icon: Drawable,
    var usageLimitMillis: Long = 0,
    var isMonitored: Boolean = false
)*/

class AppUsageAdapter(
    private val context: Context,
    private var appUsageList: List<AppUsageData>
) : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var limitSetter: MonitorFragment.LimitSetter? = null
    // Add this function to set the LimitSetter
    fun setLimitSetter(limitSetter: MonitorFragment.LimitSetter) {
        this.limitSetter = limitSetter
    }

    // Refresh stats with new data
    fun refreshStats(newAppUsageList: List<AppUsageData>) {
        appUsageList = newAppUsageList
        notifyDataSetChanged()
    }

    // Sort methods transferred from the old adapter
    fun sortByUsageTime() {
        appUsageList = appUsageList.sortedByDescending { it.usageTimeMillis }
        notifyDataSetChanged()
    }

    fun sortByLastTimeUsed() {
        appUsageList = appUsageList.sortedByDescending { it.lastTimeUsed }
        notifyDataSetChanged()
    }

    fun sortByAppName() {
        appUsageList = appUsageList.sortedBy { it.appName }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app_usage, parent, false)
        return AppUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        val appUsage = appUsageList[position]
        holder.bind(appUsage)
    }

    // Return list of apps that are being monitored
    fun getMonitoredApps(): List<AppUsageData> {
        return appUsageList.filter { it.isMonitored && it.usageLimitMillis > 0 }
    }

    // Function to update appUsageList when data changes
    fun setAppUsageData(newList: List<AppUsageData>) {
        appUsageList = newList
        notifyDataSetChanged()
    }

    private fun showLimitDialog(appUsage: AppUsageData) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Set Usage Limit (in minutes)")

        val input = android.widget.EditText(context)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("Set") { dialog, _ ->
            val minutes = input.text.toString().toLongOrNull() ?: 0L
            appUsage.usageLimitMillis = minutes * 60 * 1000
            appUsage.isMonitored = true
            // 🔥 Save the limit to SharedPreferences
            //saveLimit(appUsage.packageName, appUsage.usageLimitMillis)
            limitSetter?.onLimitSet(appUsage.packageName, appUsage.usageLimitMillis)
            notifyDataSetChanged()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun getItemCount(): Int = appUsageList.size

    // ViewHolder to bind app usage data
    inner class AppUsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val usageTimeTextView: TextView = itemView.findViewById(R.id.usageTimeTextView)
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        private val limitTextView: TextView = itemView.findViewById(R.id.limitTextView)
        private val setLimitButton: View = itemView.findViewById(R.id.setLimitButton)
        private val lastTimeUsedTextView: TextView = itemView.findViewById(R.id.lastTimeUsedTextView)

        fun bind(appUsage: AppUsageData) {
            appNameTextView.text = appUsage.appName
            usageTimeTextView.text = formatMillis(appUsage.usageTimeMillis)
            appIconImageView.setImageDrawable(appUsage.icon)
            lastTimeUsedTextView.text = "Last used: ${appUsage.lastTimeUsed}"

            limitTextView.text = if (appUsage.usageLimitMillis > 0) {
                "Limit: ${formatMillis(appUsage.usageLimitMillis)}"
            } else {
                "No Limit"
            }

            setLimitButton.setOnClickListener {
                showLimitDialog(appUsage)
            }
        }

        private fun formatMillis(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            return if (hours > 0) "$hours hr $minutes min" else "$minutes min"
        }
    }
}