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
        checkPermissionsAndStatus()
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

            // 5. Network
            val net = isNetworkConnected()
            builder.append("• Internet Connection: ").append(if (net) "PASS\n" else "FAIL\n")
            if (!net) passedAll = false

            // 6. Webhook
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
}
