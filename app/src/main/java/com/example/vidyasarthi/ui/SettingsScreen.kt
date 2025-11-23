package com.example.vidyasarthi.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vidyasarthi.MainViewModel
import com.example.vidyasarthi.R
import com.example.vidyasarthi.VidyaSarthiApplication
import com.example.vidyasarthi.core.ui.LocaleHelper
import java.util.Locale

private const val DATA_RATE_LOW = 0f
private const val DATA_RATE_MEDIUM = 1f
private const val DATA_RATE_HIGH = 2f
private const val DATA_RATE_STEPS = 1
private const val CACHE_SIZE_MIN = 10f
private const val CACHE_SIZE_MAX = 50f
private const val AUTO_RETRY_MIN = 1f
private const val AUTO_RETRY_MAX = 2f
private const val AUTO_RETRY_STEPS = 1

@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory((LocalContext.current.applicationContext as VidyaSarthiApplication).settingsManager)
    )
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .semantics { contentDescription = "Settings Screen" }
    ) {
        Text(stringResource(R.string.settings_screen_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        UserPinSetting(mainViewModel)
        Spacer(modifier = Modifier.height(24.dp))
        LanguageSetting()
        DataRateSetting(settingsViewModel)
        CacheSizeSetting(settingsViewModel)
        AutoRetrySetting(settingsViewModel)
        MuteAudioSetting(settingsViewModel)
    }
}

@Composable
private fun UserPinSetting(viewModel: MainViewModel) {
    val userPin by viewModel.userPin.collectAsState()
    Text(stringResource(R.string.user_pin_prefix) + userPin)
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { viewModel.resetUserPin() }) {
        Text(stringResource(R.string.reset_pin_button))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSetting() {
    var expanded by remember { mutableStateOf(false) }
    val supportedLocales = LocaleHelper.getSupportedLocales()
    val currentLocale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
    var selectedLocale by remember { mutableStateOf(currentLocale) }

    SettingItem(
        title = stringResource(R.string.language_setting),
        subtitle = stringResource(R.string.language_setting_subtitle)
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedLocale.getDisplayName(selectedLocale),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                supportedLocales.forEach { locale ->
                    DropdownMenuItem(
                        text = { Text(locale.getDisplayName(locale)) },
                        onClick = {
                            selectedLocale = locale
                            LocaleHelper.setLocale(locale.toLanguageTag())
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DataRateSetting(viewModel: SettingsViewModel) {
    val dataRate by viewModel.dataRate.collectAsState()
    SettingItem(
        stringResource(R.string.data_rate_setting, dataRate),
        stringResource(R.string.data_rate_subtitle)
    ) {
        Slider(
            value = when (dataRate) {
                "Low" -> DATA_RATE_LOW
                "Medium" -> DATA_RATE_MEDIUM
                else -> DATA_RATE_HIGH
            },
            onValueChange = {
                viewModel.setDataRate(
                    when (it.toInt()) {
                        DATA_RATE_LOW.toInt() -> "Low"
                        DATA_RATE_MEDIUM.toInt() -> "Medium"
                        else -> "High"
                    }
                )
            },
            steps = DATA_RATE_STEPS,
            valueRange = DATA_RATE_LOW..DATA_RATE_HIGH,
            modifier = Modifier.semantics { contentDescription = "Data rate slider" }
        )
    }
}

@Composable
private fun CacheSizeSetting(viewModel: SettingsViewModel) {
    val cacheSize by viewModel.cacheSize.collectAsState()
    SettingItem(
        stringResource(R.string.cache_size_setting, cacheSize),
        stringResource(R.string.cache_size_subtitle)
    ) {
        Slider(
            value = cacheSize.toFloat(),
            onValueChange = { viewModel.setCacheSize(it.toInt()) },
            valueRange = CACHE_SIZE_MIN..CACHE_SIZE_MAX,
            modifier = Modifier.semantics { contentDescription = "Cache size slider" }
        )
    }
}

@Composable
private fun AutoRetrySetting(viewModel: SettingsViewModel) {
    val autoRetry by viewModel.autoRetry.collectAsState()
    SettingItem(
        stringResource(R.string.auto_retry_setting, autoRetry),
        stringResource(R.string.auto_retry_subtitle)
    ) {
        Slider(
            value = autoRetry.toFloat(),
            onValueChange = { viewModel.setAutoRetry(it.toInt()) },
            steps = AUTO_RETRY_STEPS,
            valueRange = AUTO_RETRY_MIN..AUTO_RETRY_MAX,
            modifier = Modifier.semantics { contentDescription = "Auto-retry slider" }
        )
    }
}

@Composable
private fun MuteAudioSetting(viewModel: SettingsViewModel) {
    val muteAudio by viewModel.muteAudio.collectAsState()
    SettingItem(
        stringResource(R.string.mute_call_audio_setting),
        stringResource(R.string.mute_call_audio_subtitle),
        isSwitch = true
    ) {
        Switch(
            checked = muteAudio,
            onCheckedChange = { viewModel.setMuteAudio(it) },
            modifier = Modifier.semantics { contentDescription = "Mute call audio switch" })
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