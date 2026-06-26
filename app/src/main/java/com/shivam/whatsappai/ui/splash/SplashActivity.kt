package com.shivam.whatsappai.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.shivam.whatsappai.databinding.ActivitySplashBinding
import com.shivam.whatsappai.ui.home.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set values from VersionHistory dynamically
        binding.splashVersionInfo.text = "Version ${com.shivam.whatsappai.ui.about.VersionHistory.CURRENT_VERSION_NAME} (${com.shivam.whatsappai.ui.about.VersionHistory.CURRENT_VERSION_CODE})"
        binding.splashDateInfo.text = com.shivam.whatsappai.ui.about.VersionHistory.BUILD_DATE
        binding.splashCommitInfo.text = "Git: ${com.shivam.whatsappai.ui.about.VersionHistory.GIT_COMMIT_HASH}"
        binding.splashDeveloperInfo.text = "Developer\nShivam Bharti\n\nUUID: ${com.shivam.whatsappai.BuildInfo.BUILD_UUID}"

        // Delay for 2 seconds then navigate to Home Screen
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}
