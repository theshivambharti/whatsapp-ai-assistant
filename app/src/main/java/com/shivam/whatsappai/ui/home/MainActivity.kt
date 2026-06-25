package com.shivam.whatsappai.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shivam.whatsappai.R
import com.shivam.whatsappai.databinding.ActivityMainBinding
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
}
