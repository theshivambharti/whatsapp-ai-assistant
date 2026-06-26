package com.shivam.whatsappai.ui.about

import android.content.Context
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shivam.whatsappai.DiagnosticsManager
import com.shivam.whatsappai.R
import com.shivam.whatsappai.WhatsAppAccessibilityService
import com.shivam.whatsappai.WhatsAppNotificationListenerService
import com.shivam.whatsappai.WhatsAppAssistantApp
import com.shivam.whatsappai.databinding.ActivityAboutBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadStaticSpecs()
        populateVersionHistory()
        populateRoadmap()
        updateLiveDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        updateLiveDiagnostics()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadStaticSpecs() {
        // App version specs
        binding.tvAppVersionHeader.text = "v${VersionHistory.CURRENT_VERSION_NAME} (Build ${VersionHistory.CURRENT_VERSION_CODE})\nUUID: ${com.shivam.whatsappai.BuildInfo.BUILD_UUID}"
        binding.tvSpecBuildDate.text = VersionHistory.BUILD_DATE
        binding.tvSpecCommitHash.text = "${VersionHistory.GIT_COMMIT_HASH} (Local Build)"

        // Min & Target SDKs
        binding.tvSpecTargetSdk.text = "34 (Android 14)"
        binding.tvSpecMinSdk.text = "26 (Android 8.0)"

        val app = application as WhatsAppAssistantApp
        lifecycleScope.launch {
            val serverUrl = app.dataStoreManager.serverUrlFlow.first()
            binding.tvSpecWebhookUrl.text = if (serverUrl.isBlank()) "Not Configured" else serverUrl

            // AI Status
            val isOnline = isNetworkConnected()
            if (serverUrl.isBlank()) {
                binding.tvSpecAiStatus.text = "Not Configured / Idle"
                binding.tvSpecAiStatus.setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.on_surface_variant))
            } else if (!isOnline) {
                binding.tvSpecAiStatus.text = "Offline (No Internet)"
                binding.tvSpecAiStatus.setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.error))
            } else {
                binding.tvSpecAiStatus.text = "Active & Running"
                binding.tvSpecAiStatus.setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.primary))
            }
        }
    }

    private fun updateLiveDiagnostics() {
        val app = application as WhatsAppAssistantApp
        val dbHelper = app.logDbHelper

        // Notification Listener Status
        val notiConnected = WhatsAppNotificationListenerService.isConnectedState
        binding.tvDiagNotiStatus.text = if (notiConnected) "Connected (Active)" else "Disconnected"
        binding.tvDiagNotiStatus.setTextColor(
            ContextCompat.getColor(this, if (notiConnected) R.color.primary else R.color.error)
        )

        // Accessibility Service Status
        val accessConnected = WhatsAppAccessibilityService.isConnectedState
        binding.tvDiagAccessStatus.text = if (accessConnected) "Connected (Active)" else "Disconnected"
        binding.tvDiagAccessStatus.setTextColor(
            ContextCompat.getColor(this, if (accessConnected) R.color.primary else R.color.error)
        )

        // Battery optimization exemption status
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
        binding.tvDiagBatteryStatus.text = if (isIgnoringBattery) "Exempt (Unlimited)" else "Optimized (Restricted)"
        binding.tvDiagBatteryStatus.setTextColor(
            ContextCompat.getColor(this, if (isIgnoringBattery) R.color.primary else R.color.on_surface_variant)
        )

        // Internet connection
        val isOnline = isNetworkConnected()
        binding.tvDiagInternetStatus.text = if (isOnline) "Connected" else "Offline"
        binding.tvDiagInternetStatus.setTextColor(
            ContextCompat.getColor(this, if (isOnline) R.color.primary else R.color.error)
        )

        // Last events from DiagnosticsManager or SQLite logs
        val lastNoti = DiagnosticsManager.lastNotification.takeIf { it != "None" }
            ?: dbHelper.getLastLogMessage("INCOMING")
        binding.tvDiagLastNoti.text = lastNoti

        val lastReq = DiagnosticsManager.lastWebhookRequest.takeIf { it != "None" }
            ?: dbHelper.getLastLogMessage("WEBHOOK_REQ")
        binding.tvDiagLastWebhook.text = lastReq

        val lastRes = DiagnosticsManager.lastWebhookResponse.takeIf { it != "None" }
            ?: dbHelper.getLastLogMessage("WEBHOOK_RES")
        binding.tvDiagLastAi.text = lastRes

        val lastReply = DiagnosticsManager.lastReplySent.takeIf { it != "None" }
            ?: dbHelper.getLastLogMessage("REPLY_SENT")
        binding.tvDiagLastReply.text = lastReply

        // Total counters
        val totalReplies = dbHelper.getLogCountContaining("REPLY_SENT", "successfully")
        val realTotalReplies = if (totalReplies > 0) totalReplies else dbHelper.getLogCount("REPLY_SENT")
        binding.tvDiagTotalReplies.text = realTotalReplies.toString()

        binding.tvDiagTotalErrors.text = dbHelper.getLogCount("ERROR").toString()
    }

    private fun populateVersionHistory() {
        binding.layoutChangelogContainer.removeAllViews()

        for (release in VersionHistory.releases) {
            // Create a styled card for the release
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(12)
                }
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(1).toFloat()
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                strokeWidth = 0
            }

            val cardContent = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }

            // Release Header (Title & Date)
            val headerLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "Version ${release.version}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            }

            val dateView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = release.date
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            }

            headerLayout.addView(titleView)
            headerLayout.addView(dateView)
            cardContent.addView(headerLayout)

            // Features Added
            if (release.added.isNotEmpty()) {
                addChangelogSubsection(cardContent, "Features Added", release.added)
            }

            // Features Improved
            if (release.improved.isNotEmpty()) {
                addChangelogSubsection(cardContent, "Features Improved", release.improved)
            }

            // Bug Fixes
            if (release.fixes.isNotEmpty()) {
                addChangelogSubsection(cardContent, "Bug Fixes", release.fixes)
            }

            card.addView(cardContent)
            binding.layoutChangelogContainer.addView(card)
        }
    }

    private fun addChangelogSubsection(parent: LinearLayout, title: String, items: List<String>) {
        val titleView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
                bottomMargin = dpToPx(4)
            }
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            letterSpacing = 0.05f
        }
        parent.addView(titleView)

        for (item in items) {
            val itemLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(4)
                }
                orientation = LinearLayout.HORIZONTAL
            }

            val bulletView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "• "
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ContextCompat.getColor(context, R.color.primary))
            }

            val itemView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = item
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                lineSpacingMultiplier = 1.15f
            }

            itemLayout.addView(bulletView)
            itemLayout.addView(itemView)
            parent.addView(itemLayout)
        }
    }

    private fun populateRoadmap() {
        binding.layoutRoadmapContainer.removeAllViews()

        for (item in VersionHistory.roadmap) {
            val itemLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(10)
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }

            val bulletView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dpToPx(8)
                }
                text = "→"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.secondary))
            }

            val itemView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = item
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                lineSpacingMultiplier = 1.2f
            }

            itemLayout.addView(bulletView)
            itemLayout.addView(itemView)
            binding.layoutRoadmapContainer.addView(itemLayout)
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
