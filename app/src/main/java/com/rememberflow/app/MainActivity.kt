package com.rememberflow.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rememberflow.app.ui.MainViewModel
import com.rememberflow.app.ui.RememberFlowRoot
import com.rememberflow.app.ui.theme.RememberFlowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Set when the activity is opened from a reminder notification; the UI scrolls to that reminder. */
    private var openReminderId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        openReminderId = extractReminderId(intent)
        setContent {
            RememberFlowTheme {
                RememberFlowRoot(viewModel = viewModel, openReminderId = openReminderId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openReminderId = extractReminderId(intent)
    }

    private fun extractReminderId(intent: Intent?): Long? {
        val id = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: -1L
        return if (id >= 0) id else null
    }

    companion object {
        const val EXTRA_REMINDER_ID = "open_reminder_id"
    }
}
