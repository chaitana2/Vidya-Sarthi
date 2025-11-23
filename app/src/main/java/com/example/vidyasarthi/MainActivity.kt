package com.example.vidyasarthi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.vidyasarthi.ui.ContentScreen
import com.example.vidyasarthi.ui.HomeScreen
import com.example.vidyasarthi.ui.SettingsScreen
import com.example.vidyasarthi.ui.theme.VidyaSarthiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as VidyaSarthiApplication
        ViewModelFactory(
            app.repository,
            app.smsHandler,
            app.dataTransmissionManager,
            app.voiceUiManager,
            app.callManager
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Toast.makeText(this, R.string.permissions_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.permissions_required, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        setContent {
            VidyaSarthiTheme {
                VidyaSarthiApp(viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_CALL_LOG
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}

@Composable
fun VidyaSarthiApp(viewModel: MainViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = stringResource(destination.label)) },
                        label = { Text(stringResource(destination.label)) },
                        selected = destination == currentDestination,
                        onClick = { currentDestination = destination }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(viewModel, Modifier.padding(innerPadding))
            AppDestinations.CONTENT -> ContentScreen(viewModel, Modifier.padding(innerPadding))
            AppDestinations.SETTINGS -> SettingsScreen(mainViewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}

enum class AppDestinations(
    val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.destination_connect, Icons.Filled.Call),
    CONTENT(R.string.destination_content, Icons.Filled.Home),
    SETTINGS(R.string.destination_settings, Icons.Filled.Settings),
}