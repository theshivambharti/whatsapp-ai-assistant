package com.shivam.whatsappai.ui.settings

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.shivam.whatsappai.R
import com.shivam.whatsappai.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        observeViewModel()
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
        binding.btnSave.setOnClickListener {
            val url = binding.etServerUrl.text?.toString()?.trim() ?: ""
            val hName = binding.etHeaderName.text?.toString()?.trim() ?: ""
            val hValue = binding.etHeaderValue.text?.toString()?.trim() ?: ""

            if (validateInputs(url)) {
                viewModel.saveSettings(url, hName, hValue)
            }
        }

        binding.btnSendTest.setOnClickListener {
            val url = binding.etServerUrl.text?.toString()?.trim() ?: ""
            val hName = binding.etHeaderName.text?.toString()?.trim() ?: ""
            val hValue = binding.etHeaderValue.text?.toString()?.trim() ?: ""

            if (validateInputs(url)) {
                viewModel.saveSettings(url, hName, hValue)
                viewModel.sendTestRequest()
            }
        }
    }

    private fun validateInputs(url: String): Boolean {
        if (url.isBlank()) {
            binding.tilServerUrl.error = "Server URL cannot be empty"
            return false
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            binding.tilServerUrl.error = "Please enter a valid URL (e.g. http://example.com)"
            return false
        }
        binding.tilServerUrl.error = null
        return true
    }

    private fun observeViewModel() {
        viewModel.serverUrl.observe(this) { url ->
            if (binding.etServerUrl.text?.toString().isNullOrBlank() && !url.isNullOrBlank()) {
                binding.etServerUrl.setText(url)
            }
        }

        viewModel.headerName.observe(this) { name ->
            if (binding.etHeaderName.text?.toString().isNullOrBlank() && !name.isNullOrBlank()) {
                binding.etHeaderName.setText(name)
            }
        }

        viewModel.headerValue.observe(this) { value ->
            if (binding.etHeaderValue.text?.toString().isNullOrBlank() && !value.isNullOrBlank()) {
                binding.etHeaderValue.setText(value)
            }
        }

        viewModel.saveStatus.observe(this) { saved ->
            if (saved) {
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveStatus()
            }
        }

        viewModel.testRequestState.observe(this) { state ->
            when (state) {
                is TestRequestState.Idle -> {
                    setLoadingState(false)
                }
                is TestRequestState.Loading -> {
                    setLoadingState(true)
                }
                is TestRequestState.Success -> {
                    setLoadingState(false)
                    showResponseDialog(state.response.code, state.response.body, state.response.isSuccess)
                    viewModel.resetTestState()
                }
                is TestRequestState.Error -> {
                    setLoadingState(false)
                    showErrorDialog(state.message)
                    viewModel.resetTestState()
                }
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        if (loading) {
            binding.btnSendTest.text = "Sending..."
            binding.btnSendTest.isEnabled = false
            binding.btnSave.isEnabled = false
        } else {
            binding.btnSendTest.text = getString(R.string.btn_send_test)
            binding.btnSendTest.isEnabled = true
            binding.btnSave.isEnabled = true
        }
    }

    private fun showResponseDialog(code: Int, body: String, isSuccess: Boolean) {
        val statusText = if (isSuccess) "Success (HTTP $code)" else "Error (HTTP $code)"
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.test_request_success))
            .setMessage("${getString(R.string.response_code)}: $statusText\n\n${getString(R.string.response_body)}:\n$body")
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showErrorDialog(error: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.test_request_failed))
            .setMessage("An error occurred during network transfer:\n\n$error")
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
