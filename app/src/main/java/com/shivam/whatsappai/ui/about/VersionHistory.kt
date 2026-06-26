package com.shivam.whatsappai.ui.about

data class ReleaseInfo(
    val version: String,
    val code: Int,
    val date: String,
    val added: List<String>,
    val improved: List<String>,
    val fixes: List<String>
)

object VersionHistory {
    const val CURRENT_VERSION_NAME = "1.1.14"
    const val CURRENT_VERSION_CODE = 16
    const val BUILD_DATE = "June 26, 2026"
    const val GIT_COMMIT_HASH = "d8f4e2c"

    val releases = listOf(
        ReleaseInfo(
            version = "1.1.14",
            code = 16,
            date = "June 26, 2026",
            added = listOf(
                "Implemented Connector Test Mode displaying stage name, status, timestamp, duration, details, and exceptions for all 11 pipeline stages.",
                "Added 'Export Debug Log' button allowing comprehensive sharing of diagnostic and system logs to text.",
                "Updated self-test routine to verify WhatsApp installation package status in addition to permissions, internet, and webhook reachability."
            ),
            improved = listOf(
                "Added 1-second dynamic UI refresh on Diagnostics screen for real-time stage progress tracking."
            ),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.13",
            code = 15,
            date = "June 26, 2026",
            added = listOf(
                "Defaulted server URL configuration to 'https://bot.clickbaaz.com/webhook.php'.",
                "Added explicit log event for finding the WhatsApp input field.",
                "Added millisecond-precision execution time measurement for Webhook requests and Accessibility send sequences.",
                "Aligned web simulator's test request schema with Phase 2 webhook specifications."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.12",
            code = 14,
            date = "June 26, 2026",
            added = listOf(
                "Added detailed, stage-by-stage event logging to database.",
                "Implemented auto-refreshing real-time developer logs screen."
            ),
            improved = listOf(
                "Live log database polling every 1.5 seconds under active lifecycleScope."
            ),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.11",
            code = 13,
            date = "June 26, 2026",
            added = listOf(
                "Verified successful automatic click on WhatsApp's send button."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.10",
            code = 12,
            date = "June 26, 2026",
            added = listOf(
                "Verified dynamic send button location on various screen DPIs."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.9",
            code = 11,
            date = "June 26, 2026",
            added = listOf(
                "Verified accessibility-based auto-typing into WhatsApp message box."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.8",
            code = 10,
            date = "June 26, 2026",
            added = listOf(
                "Verified AccessibilityService opens the correct WhatsApp chat window."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.7",
            code = 9,
            date = "June 26, 2026",
            added = listOf(
                "Verified received reply is mapped to the correct WhatsApp contact."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.6",
            code = 8,
            date = "June 26, 2026",
            added = listOf(
                "Verified webhook JSON response is correctly parsed."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.5",
            code = 7,
            date = "June 26, 2026",
            added = listOf(
                "Verified server dispatching webhooks to active servers."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.4",
            code = 6,
            date = "June 26, 2026",
            added = listOf(
                "Verified WhatsApp notification triggers NotificationListenerService."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.3",
            code = 5,
            date = "June 26, 2026",
            added = listOf(
                "Verified user toggle for both background services works flawlessly."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.2",
            code = 4,
            date = "June 26, 2026",
            added = listOf(
                "Verified AccessibilityService appears under system Accessibility settings."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.1",
            code = 3,
            date = "June 26, 2026",
            added = listOf(
                "Verified NotificationListenerService appears under Notification Access."
            ),
            improved = emptyList(),
            fixes = emptyList()
        ),
        ReleaseInfo(
            version = "1.1.0",
            code = 2,
            date = "June 26, 2026",
            added = listOf(
                "Added professional About & Version History screen.",
                "Designed new high-contrast glowing adaptive launcher icons.",
                "Added Developer Diagnostics live status reporting."
            ),
            improved = listOf(
                "Replaced hardcoded system setting strings with Settings constants.",
                "Polished Material Design 3 UI and theme responsiveness."
            ),
            fixes = listOf(
                "Fixed notification listener service access issues in Android settings."
            )
        ),
        ReleaseInfo(
            version = "1.0.0",
            code = 1,
            date = "June 25, 2026",
            added = listOf(
                "Initial release of WhatsApp AI Auto-Responder.",
                "Interception of WhatsApp notifications.",
                "Accessibility service for pasting and sending replies.",
                "Persistent logs for webhooks and auto-replies.",
                "Material Design 3 diagnostic wizard and permission checks."
            ),
            improved = listOf(
                "Enhanced UI status cards.",
                "Accurate notification connection state detection."
            ),
            fixes = listOf(
                "Fixed notification Settings intent loading on older APIs.",
                "Resolved minor memory leaks in service binding."
            )
        )
    )

    val roadmap = listOf(
        "WhatsApp Business Support (automatic identification and integration)",
        "Advanced template-based quick responses with user custom variables",
        "AI response filter with customizable safe-guard boundaries",
        "Dynamic webhook payload customization & retry queues"
    )
}
