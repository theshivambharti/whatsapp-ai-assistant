package com.shivam.whatsappai.ui.diagnostics

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shivam.whatsappai.DiagnosticsManager
import com.shivam.whatsappai.R
import com.shivam.whatsappai.WhatsAppAccessibilityService
import com.shivam.whatsappai.WhatsAppAssistantApp
import com.shivam.whatsappai.WhatsAppNotificationListenerService
import com.shivam.whatsappai.data.model.TestRequest
import com.shivam.whatsappai.data.network.RetrofitClient
import com.shivam.whatsappai.databinding.ActivityDiagnosticsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiagnosticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiagnosticsBinding
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
        checkPermissionsAndStatus()
    }

    private var refreshJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        checkPermissionsAndStatus()
    }

    override fun onResume() {
        super.onResume()
        startPeriodicRefresh()
    }

    override fun onPause() {
        super.onPause()
        refreshJob?.cancel()
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            val app = application as WhatsAppAssistantApp
            while (true) {
                checkPermissionsAndStatus()
                try {
                    val isTestMode = app.dataStoreManager.testModeFlow.first()
                    if (isTestMode) {
                        binding.cardTestModeStages.visibility = android.view.View.VISIBLE
                        renderStages()
                    } else {
                        binding.cardTestModeStages.visibility = android.view.View.GONE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupListeners() {
        binding.btnFixNotiAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        binding.btnFixAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.btnFixPostNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(this, "Notification permission not required for this Android version", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFixBattery.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }

        binding.btnTestWebhook.setOnClickListener {
            runWebhookTest()
        }

        binding.btnRunSelfTest.setOnClickListener {
            runCompleteSelfTest()
        }

        binding.btnExportDebugLog.setOnClickListener {
            exportDebugLog()
        }
    }

    @SuppressLint("BatteryLife")
    private fun checkPermissionsAndStatus() {
        val app = application as WhatsAppAssistantApp

        // 1. Notification Access
        val notiAccessEnabled = isNotificationAccessEnabled()
        if (notiAccessEnabled) {
            binding.tvPermNotiStatus.text = "Granted"
            binding.tvPermNotiStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
            binding.btnFixNotiAccess.isEnabled = false
            binding.btnFixNotiAccess.text = "OK"
        } else {
            binding.tvPermNotiStatus.text = "Not Granted"
            binding.tvPermNotiStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.btnFixNotiAccess.isEnabled = true
            binding.btnFixNotiAccess.text = "FIX"
        }

        // 2. Accessibility
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        if (accessibilityEnabled) {
            binding.tvPermAccessStatus.text = "Granted"
            binding.tvPermAccessStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
            binding.btnFixAccessibility.isEnabled = false
            binding.btnFixAccessibility.text = "OK"
        } else {
            binding.tvPermAccessStatus.text = "Not Granted"
            binding.tvPermAccessStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.btnFixAccessibility.isEnabled = true
            binding.btnFixAccessibility.text = "FIX"
        }

        // 3. POST_NOTIFICATIONS (Android 13+)
        val postNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (postNotificationsEnabled) {
            binding.tvPermPostStatus.text = "Granted"
            binding.tvPermPostStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
            binding.btnFixPostNotifications.isEnabled = false
            binding.btnFixPostNotifications.text = "OK"
        } else {
            binding.tvPermPostStatus.text = "Not Granted"
            binding.tvPermPostStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.btnFixPostNotifications.isEnabled = true
            binding.btnFixPostNotifications.text = "FIX"
        }

        // 4. Battery Exemption
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
        if (isIgnoringBattery) {
            binding.tvPermBatteryStatus.text = "Granted"
            binding.tvPermBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
            binding.btnFixBattery.isEnabled = false
            binding.btnFixBattery.text = "OK"
        } else {
            binding.tvPermBatteryStatus.text = "Not Granted"
            binding.tvPermBatteryStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.btnFixBattery.isEnabled = true
            binding.btnFixBattery.text = "FIX"
        }

        // 5. Diagnostics details
        val notiConnected = WhatsAppNotificationListenerService.isConnectedState
        binding.tvDiagNotiConnected.text = if (notiConnected) "CONNECTED" else "NOT CONNECTED"
        binding.tvDiagNotiConnected.setTextColor(
            ContextCompat.getColor(this, if (notiConnected) R.color.primary else R.color.error)
        )

        val accessConnected = WhatsAppAccessibilityService.isConnectedState
        binding.tvDiagAccessConnected.text = if (accessConnected) "CONNECTED" else "NOT CONNECTED"
        binding.tvDiagAccessConnected.setTextColor(
            ContextCompat.getColor(this, if (accessConnected) R.color.primary else R.color.error)
        )

        val foregroundRunning = WhatsAppNotificationListenerService.isRunning
        binding.tvDiagForegroundRunning.text = if (foregroundRunning) "RUNNING" else "STOPPED"
        binding.tvDiagForegroundRunning.setTextColor(
            ContextCompat.getColor(this, if (foregroundRunning) R.color.primary else R.color.error)
        )

        binding.tvDiagWhatsappVersion.text = DiagnosticsManager.getWhatsAppVersion(this)

        // Read dynamic logs from DiagnosticsManager
        binding.tvDiagLastNotification.text = DiagnosticsManager.lastNotification
        binding.tvDiagLastReq.text = DiagnosticsManager.lastWebhookRequest
        binding.tvDiagLastRes.text = DiagnosticsManager.lastWebhookResponse
        binding.tvDiagLastReply.text = DiagnosticsManager.lastReplySent
        binding.tvDiagLastError.text = DiagnosticsManager.lastError
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_NOTIFICATION_LISTENERS)
        return !enabledListeners.isNullOrBlank() && enabledListeners.contains(packageName)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val serviceName = "$packageName/${WhatsAppAccessibilityService::class.java.name}"
        return !enabledServices.isNullOrBlank() && enabledServices.contains(serviceName)
    }

    private fun runWebhookTest() {
        val app = application as WhatsAppAssistantApp
        binding.tvPermWebhookStatus.text = "TESTING..."
        binding.tvPermWebhookStatus.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))

        lifecycleScope.launch {
            val serverUrl = app.dataStoreManager.serverUrlFlow.first()
            if (serverUrl.isBlank()) {
                binding.tvPermWebhookStatus.text = "NO URL"
                binding.tvPermWebhookStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.error))
                Toast.makeText(this@DiagnosticsActivity, "Please configure Webhook URL in settings", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val isOnline = isNetworkConnected()
            if (!isOnline) {
                binding.tvPermWebhookStatus.text = "NO INTERNET"
                binding.tvPermWebhookStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.error))
                Toast.makeText(this@DiagnosticsActivity, "Device is offline", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val testRequest = TestRequest("ping")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.sendTestRequest(serverUrl, emptyMap(), testRequest)
                }
                if (response.isSuccessful) {
                    binding.tvPermWebhookStatus.text = "REACHABLE"
                    binding.tvPermWebhookStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.primary))
                    Toast.makeText(this@DiagnosticsActivity, "Webhook reachable!", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvPermWebhookStatus.text = "UNREACHABLE"
                    binding.tvPermWebhookStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.error))
                    Toast.makeText(this@DiagnosticsActivity, "Server responded with code ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.tvPermWebhookStatus.text = "FAIL"
                binding.tvPermWebhookStatus.setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.error))
                Toast.makeText(this@DiagnosticsActivity, "Test failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    private fun runCompleteSelfTest() {
        lifecycleScope.launch {
            val app = application as WhatsAppAssistantApp
            val builder = StringBuilder()
            var passedAll = true

            // 1. Notification Access
            val noti = isNotificationAccessEnabled()
            builder.append("• Notification Access: ").append(if (noti) "PASS\n" else "FAIL\n")
            if (!noti) passedAll = false

            // 2. Accessibility
            val acc = isAccessibilityServiceEnabled()
            builder.append("• Accessibility Service: ").append(if (acc) "PASS\n" else "FAIL\n")
            if (!acc) passedAll = false

            // 3. Post notifications
            val postNoti = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this@DiagnosticsActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            builder.append("• Post Notifications: ").append(if (postNoti) "PASS\n" else "FAIL\n")
            if (!postNoti) passedAll = false

            // 4. Battery Optimization
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val battery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(packageName)
            } else {
                true
            }
            builder.append("• Battery Optimization: ").append(if (battery) "PASS\n" else "FAIL\n")
            if (!battery) passedAll = false

            // 5. WhatsApp Installed Check
            val whatsappVersion = DiagnosticsManager.getWhatsAppVersion(this@DiagnosticsActivity)
            val whatsappInstalled = whatsappVersion != "Not Installed"
            builder.append("• WhatsApp Installed: ").append(if (whatsappInstalled) "PASS ($whatsappVersion)\n" else "FAIL (Not Installed)\n")
            if (!whatsappInstalled) passedAll = false

            // 6. Network
            val net = isNetworkConnected()
            builder.append("• Internet Connection: ").append(if (net) "PASS\n" else "FAIL\n")
            if (!net) passedAll = false

            // 7. Webhook
            val serverUrl = app.dataStoreManager.serverUrlFlow.first()
            var web = false
            if (serverUrl.isNotBlank() && net) {
                try {
                    val testRequest = TestRequest("ping")
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.sendTestRequest(serverUrl, emptyMap(), testRequest)
                    }
                    web = response.isSuccessful
                } catch (e: Exception) {
                    web = false
                }
            }
            builder.append("• Webhook Reachability: ").append(if (web) "PASS\n" else "FAIL\n")
            if (!web) passedAll = false

            val title = if (passedAll) "DIAGNOSTICS: PASS" else "DIAGNOSTICS: FAIL"
            val icon = if (passedAll) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert

            AlertDialog.Builder(this@DiagnosticsActivity)
                .setTitle(title)
                .setIcon(icon)
                .setMessage("System diagnostic report:\n\n$builder")
                .setPositiveButton("OK", null)
                .show()

            checkPermissionsAndStatus()
        }
    }

    private fun renderStages() {
        val container = binding.layoutStagesContainer
        container.removeAllViews()

        val stages = DiagnosticsManager.stages
        for (stage in stages) {
            val itemLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 12, 0, 12)
            }

            // Top Row: Status, Name, and Duration
            val topRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val statusIndicator = android.widget.TextView(this).apply {
                text = when (stage.status) {
                    "SUCCESS" -> "🟢 "
                    "FAILURE" -> "🔴 "
                    else -> "🟡 "
                }
                textSize = 14f
            }

            val nameView = android.widget.TextView(this).apply {
                text = "Stage ${stage.number}: ${stage.name}"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.on_surface))
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val durationView = android.widget.TextView(this).apply {
                text = if (stage.durationMs >= 0) "${stage.durationMs}ms" else ""
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.on_surface_variant))
                gravity = android.view.Gravity.END
            }

            topRow.addView(statusIndicator)
            topRow.addView(nameView)
            topRow.addView(durationView)
            itemLayout.addView(topRow)

            // Second Row: Timestamp and Details
            if (stage.status != "WAITING") {
                val detailsLayout = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(24, 4, 0, 0)
                }

                if (stage.timestamp.isNotEmpty()) {
                    val timeView = android.widget.TextView(this).apply {
                        text = "Timestamp: ${stage.timestamp}"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.on_surface_variant))
                    }
                    detailsLayout.addView(timeView)
                }

                val detailsView = android.widget.TextView(this).apply {
                    text = stage.details
                    textSize = 12f
                    setTextColor(
                        if (stage.status == "FAILURE")
                            ContextCompat.getColor(this@DiagnosticsActivity, R.color.error)
                        else
                            ContextCompat.getColor(this@DiagnosticsActivity, R.color.on_surface_variant)
                    )
                }
                detailsLayout.addView(detailsView)

                // Exception Stack Trace
                if (stage.exception != null) {
                    val excView = android.widget.TextView(this).apply {
                        text = "Stack Trace:\n${stage.exception}"
                        textSize = 10f
                        typeface = android.graphics.Typeface.MONOSPACE
                        setTextColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.error))
                        setPadding(8, 8, 8, 8)
                        setBackgroundColor(0x11FF0000) // Translucent light red background
                    }
                    detailsLayout.addView(excView)
                }

                itemLayout.addView(detailsLayout)
            }

            // Separator/Divider
            if (stage.number < 11) {
                val divider = android.view.View(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        setMargins(0, 8, 0, 0)
                    }
                    setBackgroundColor(ContextCompat.getColor(this@DiagnosticsActivity, R.color.surface_variant))
                }
                itemLayout.addView(divider)
            }

            container.addView(itemLayout)
        }
    }

    private fun exportDebugLog() {
        lifecycleScope.launch {
            val app = application as WhatsAppAssistantApp
            val logDbHelper = app.logDbHelper
            val report = StringBuilder()

            report.append("==================================================\n")
            report.append("WHATSAPP AI ASSISTANT - SYSTEM DEBUG REPORT\n")
            report.append("Generated on: ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())).append("\n")
            report.append("==================================================\n\n")

            // Part 1: System Info
            report.append("[SYSTEM ENVIRONMENT]\n")
            report.append("• Notification Access: ").append(if (isNotificationAccessEnabled()) "ENABLED" else "DISABLED").append("\n")
            report.append("• Accessibility Service: ").append(if (isAccessibilityServiceEnabled()) "ENABLED" else "DISABLED").append("\n")
            report.append("• WhatsApp Version: ").append(DiagnosticsManager.getWhatsAppVersion(this@DiagnosticsActivity)).append("\n")

            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val batteryIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(packageName)
            } else {
                true
            }
            report.append("• Battery Optimization Ignored: ").append(if (batteryIgnoring) "YES" else "NO").append("\n")
            report.append("• Background Service Active: ").append(if (WhatsAppNotificationListenerService.isRunning) "YES" else "NO").append("\n")
            report.append("• Network Connected: ").append(if (isNetworkConnected()) "YES" else "NO").append("\n")

            val serverUrl = app.dataStoreManager.serverUrlFlow.first()
            report.append("• Configured Server URL: ").append(serverUrl).append("\n\n")

            // Part 2: Stage States
            report.append("[TEST MODE STAGES PIPELINE]\n")
            DiagnosticsManager.stages.forEach { stage ->
                report.append("Stage ${stage.number}: ${stage.name}\n")
                report.append("  Status: ${stage.status}\n")
                if (stage.status != "WAITING") {
                    report.append("  Timestamp: ${stage.timestamp}\n")
                    report.append("  Duration: ${stage.durationMs}ms\n")
                    report.append("  Details: ${stage.details}\n")
                    if (stage.exception != null) {
                        report.append("  Exception: ${stage.exception}\n")
                    }
                }
                report.append("\n")
            }
            report.append("\n")

            // Part 3: SQLite Logs
            report.append("[RECENT SYSTEM DATABASE LOGS (LAST 200)]\n")
            val logs = withContext(Dispatchers.IO) {
                logDbHelper.getAllLogs()
            }
            if (logs.isEmpty()) {
                report.append("No database logs found.\n")
            } else {
                logs.forEach { log ->
                    report.append("[${log.timestamp}] [${log.type}] ${log.message}\n")
                }
            }

            // Share/Save log report
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, report.toString())
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "WhatsApp AI Assistant - Debug Report")
            }

            val shareIntent = Intent.createChooser(sendIntent, "Export Debug Report")
            startActivity(shareIntent)
        }
    }
}
