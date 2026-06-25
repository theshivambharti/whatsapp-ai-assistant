package com.shivam.whatsappai.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shivam.whatsappai.R
import com.shivam.whatsappai.WhatsAppAccessibilityService
import com.shivam.whatsappai.databinding.ActivityMainBinding
import com.shivam.whatsappai.ui.logs.LogsActivity
import com.shivam.whatsappai.ui.settings.SettingsActivity
import com.shivam.whatsappai.ui.settings.SettingsViewModel
import com.shivam.whatsappai.ui.settings.SettingsViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStatuses()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupListeners() {
        binding.btnToggleService.setOnClickListener {
            viewModel.toggleService()
        }

        binding.btnConfigure.setOnClickListener {
            navigateToSettings()
        }

        binding.btnGrantPermissions.setOnClickListener {
            handleGrantPermissionsClick()
        }

        binding.btnViewLogs.setOnClickListener {
            val intent = Intent(this, LogsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun observeViewModel() {
        viewModel.serviceActive.observe(this) { isActive ->
            updateServiceStatusUi(isActive)
        }

        viewModel.serverUrl.observe(this) { url ->
            binding.tvSummaryUrl.text = if (url.isNullOrBlank()) "Not Configured" else url
            updateWebhookStatus(url)
        }

        viewModel.headerName.observe(this) { name ->
            val value = viewModel.headerValue.value ?: ""
            updateHeadersSummary(name, value)
        }

        viewModel.headerValue.observe(this) { value ->
            val name = viewModel.headerName.value ?: ""
            updateHeadersSummary(name, value)
        }
    }

    private fun updateServiceStatusUi(isActive: Boolean) {
        if (isActive) {
            binding.tvServiceStatus.text = getString(R.string.service_status_active)
            binding.tvServiceDesc.text = "Actively listening for incoming WhatsApp events"
            binding.statusIcon.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.presence_online))
            binding.statusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
            binding.btnToggleService.text = getString(R.string.btn_toggle_service_off)
            binding.btnToggleService.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_variant))
        } else {
            binding.tvServiceStatus.text = getString(R.string.service_status_inactive)
            binding.tvServiceDesc.text = "Assistant is currently disabled"
            binding.statusIcon.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.presence_invisible))
            binding.statusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.on_surface_variant))
            binding.btnToggleService.text = getString(R.string.btn_toggle_service_on)
            binding.btnToggleService.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun updateHeadersSummary(name: String, value: String) {
        binding.tvSummaryHeaders.text = if (name.isNotBlank() && value.isNotBlank()) {
            "$name: ${"*".repeat(value.length.coerceAtMost(8))}"
        } else {
            "No headers specified"
        }
    }

    private fun checkPermissionsAndStatuses() {
        val notiEnabled = isNotificationAccessEnabled()
        val accessEnabled = isAccessibilityServiceEnabled()

        // Update Notification Access UI
        if (notiEnabled) {
            binding.tvStatusNotification.text = getString(R.string.status_enabled)
            binding.tvStatusNotification.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            binding.tvStatusNotification.text = getString(R.string.status_disabled)
            binding.tvStatusNotification.setTextColor(ContextCompat.getColor(this, R.color.error))
        }

        // Update Accessibility Service UI
        if (accessEnabled) {
            binding.tvStatusAccessibility.text = getString(R.string.status_enabled)
            binding.tvStatusAccessibility.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            binding.tvStatusAccessibility.text = getString(R.string.status_disabled)
            binding.tvStatusAccessibility.setTextColor(ContextCompat.getColor(this, R.color.error))
        }

        // Adjust grant permissions button visibility/text
        if (notiEnabled && accessEnabled) {
            binding.btnGrantPermissions.text = "All Access Granted"
            binding.btnGrantPermissions.isEnabled = false
            binding.btnGrantPermissions.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A5568"))
        } else {
            binding.btnGrantPermissions.text = getString(R.string.btn_grant_permissions)
            binding.btnGrantPermissions.isEnabled = true
            binding.btnGrantPermissions.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun updateWebhookStatus(url: String?) {
        if (!url.isNullOrBlank()) {
            binding.tvStatusWebhook.text = getString(R.string.status_connected)
            binding.tvStatusWebhook.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            binding.tvStatusWebhook.text = getString(R.string.status_not_connected)
            binding.tvStatusWebhook.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val packageName = packageName
        return !enabledListeners.isNullOrBlank() && enabledListeners.contains(packageName)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val serviceName = "$packageName/${WhatsAppAccessibilityService::class.java.name}"
        return !enabledServices.isNullOrBlank() && enabledServices.contains(serviceName)
    }

    private fun handleGrantPermissionsClick() {
        if (!isNotificationAccessEnabled()) {
            showNotificationAccessDialog()
        } else if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog()
        } else {
            Toast.makeText(this, "All required services are enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNotificationAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_noti_access_title)
            .setMessage(R.string.dialog_noti_access_desc)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAccessibilityServiceDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_access_title)
            .setMessage(R.string.dialog_access_desc)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
