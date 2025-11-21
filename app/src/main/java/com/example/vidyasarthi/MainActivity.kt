package com.example.vidyasarthi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import com.example.vidyasarthi.ui.theme.VidyaSarthiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions Required for Functionality", Toast.LENGTH_LONG).show()
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
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.CONTENT -> ContentScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.SETTINGS -> SettingsScreen(viewModel, Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Connect", Icons.Default.Call),
    CONTENT("Content", Icons.Default.Home),
    SETTINGS("Settings", Icons.Default.Settings),
}

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val transmissionState by viewModel.transmissionState.collectAsState()
    var hostPhone by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vidya Sarthi",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Status: $connectionStatus",
            style = MaterialTheme.typography.titleMedium,
            color = if (connectionStatus == "Connected") Color.Green else Color.Red,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (transmissionState == DataTransmissionManager.TransmissionState.RECEIVING || 
            transmissionState == DataTransmissionManager.TransmissionState.SENDING) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            Text(text = "Transmission: $transmissionState", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
            value = hostPhone,
            onValueChange = { hostPhone = it },
            label = { Text("Host Phone Number") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.connectToHost(hostPhone) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Connect via SMS")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { viewModel.startVoiceCall(hostPhone) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Voice Call")
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.startDataTransmission() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Receiving Data")
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { viewModel.sendData("Hello Host, requesting weather data.") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Send Test Data")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Logs:", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ContentScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val receivedText by viewModel.receivedContentText.collectAsState()

    Column(modifier = modifier.padding(16.dp)) {
        Text("Offline Content", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weather Update", style = MaterialTheme.typography.titleMedium)
                Text("Last updated: Today", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (receivedText.isNotEmpty()) {
                    Text(receivedText)
                } else {
                    Text("No data available. Connect to update.")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
             Column(modifier = Modifier.padding(16.dp)) {
                Text("Agriculture Tutorial", style = MaterialTheme.typography.titleMedium)
                Text("Introduction to Sustainable Farming")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Click to read full article (mock)")
            }
        }
        
        Button(onClick = { viewModel.simulateReceive("Simulated Weather Data: 25°C, Sunny") }) {
            Text("Simulate Receive Data")
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("User PIN: ****") // In real app, show mask/unmask
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* Reset PIN logic */ }) {
            Text("Reset PIN")
        }
    }
}
