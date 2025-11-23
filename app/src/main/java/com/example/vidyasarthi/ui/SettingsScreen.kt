package com.example.vidyasarthi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vidyasarthi.MainViewModel
import com.example.vidyasarthi.R

@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val userPin by viewModel.userPin.collectAsState()
    val dataRate by viewModel.dataRate.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val autoRetry by viewModel.autoRetry.collectAsState()
    val muteAudio by viewModel.muteAudio.collectAsState()

    Column(modifier = modifier
        .padding(16.dp)
        .semantics { contentDescription = "Settings Screen" }) {
        Text(stringResource(R.string.settings_screen_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.user_pin_prefix) + userPin)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.resetUserPin() }) {
            Text(stringResource(R.string.reset_pin_button))
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingItem(stringResource(R.string.data_rate_setting, dataRate), stringResource(R.string.data_rate_subtitle)) {
            Slider(
                value = when (dataRate) {
                    "Low" -> 0f
                    "Medium" -> 1f
                    else -> 2f
                },
                onValueChange = {
                    viewModel.setDataRate(
                        when (it.toInt()) {
                            0 -> "Low"
                            1 -> "Medium"
                            else -> "High"
                        }
                    )
                },
                steps = 1,
                valueRange = 0f..2f,
                modifier = Modifier.semantics { contentDescription = "Data rate slider" }
            )
        }

        SettingItem(stringResource(R.string.cache_size_setting, cacheSize), stringResource(R.string.cache_size_subtitle)) {
            Slider(
                value = cacheSize.toFloat(),
                onValueChange = { viewModel.setCacheSize(it.toInt()) },
                valueRange = 10f..50f,
                modifier = Modifier.semantics { contentDescription = "Cache size slider" }
            )
        }

        SettingItem(stringResource(R.string.auto_retry_setting, autoRetry), stringResource(R.string.auto_retry_subtitle)) {
            Slider(
                value = autoRetry.toFloat(),
                onValueChange = { viewModel.setAutoRetry(it.toInt()) },
                steps = 1,
                valueRange = 1f..2f,
                modifier = Modifier.semantics { contentDescription = "Auto-retry slider" }
            )
        }

        SettingItem(stringResource(R.string.mute_call_audio_setting), stringResource(R.string.mute_call_audio_subtitle), isSwitch = true) {
            Switch(checked = muteAudio, onCheckedChange = { viewModel.setMuteAudio(it) },
                modifier = Modifier.semantics { contentDescription = "Mute call audio switch" })
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, isSwitch: Boolean = false, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        if (isSwitch) {
            content()
        }
    }
    if (!isSwitch) {
        content()
    }
}
