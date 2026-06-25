package com.shivam.whatsappai.ui.logs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shivam.whatsappai.WhatsAppAssistantApp
import com.shivam.whatsappai.data.db.LogEntry
import com.shivam.whatsappai.databinding.ActivityLogsBinding
import com.shivam.whatsappai.databinding.ItemLogBinding

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private val logsList = mutableListOf<LogEntry>()
    private lateinit var logsAdapter: LogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadLogs()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        logsAdapter = LogsAdapter(logsList)
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = logsAdapter
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            loadLogs()
        }

        binding.btnClearLogs.setOnClickListener {
            val app = application as WhatsAppAssistantApp
            app.logDbHelper.clearAllLogs()
            loadLogs()
        }
    }

    private fun loadLogs() {
        val app = application as WhatsAppAssistantApp
        val dbLogs = app.logDbHelper.getAllLogs()
        
        logsList.clear()
        logsList.addAll(dbLogs)
        logsAdapter.notifyDataSetChanged()

        if (logsList.isEmpty()) {
            binding.tvEmptyLogs.visibility = View.VISIBLE
            binding.rvLogs.visibility = View.GONE
        } else {
            binding.tvEmptyLogs.visibility = View.GONE
            binding.rvLogs.visibility = View.VISIBLE
        }
    }

    inner class LogsAdapter(private val logs: List<LogEntry>) :
        RecyclerView.Adapter<LogsAdapter.LogViewHolder>() {

        inner class LogViewHolder(val itemBinding: ItemLogBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val itemBinding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return LogViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val log = logs[position]
            holder.itemBinding.tvLogTime.text = log.timestamp
            holder.itemBinding.tvLogMessage.text = log.message
            holder.itemBinding.tvLogType.text = log.type

            // Dynamic background with rounded corners for the badge
            val badgeColor = when (log.type) {
                "INCOMING" -> Color.parseColor("#1D84B5") // Ocean Blue
                "WEBHOOK_REQ" -> Color.parseColor("#E07A5F") // Terracotta Orange
                "WEBHOOK_RES" -> Color.parseColor("#3D9A5A") // Emerald Green
                "REPLY_SENT" -> Color.parseColor("#6F2DBD") // Grape Purple
                "ERROR" -> Color.parseColor("#D90429") // Deep Red
                else -> Color.parseColor("#4A5568") // Slate Gray
            }

            val badgeBackground = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(badgeColor)
            }
            holder.itemBinding.tvLogType.background = badgeBackground
            holder.itemBinding.tvLogType.setTextColor(Color.WHITE)
        }

        override fun getItemCount(): Int = logs.size
    }
}
