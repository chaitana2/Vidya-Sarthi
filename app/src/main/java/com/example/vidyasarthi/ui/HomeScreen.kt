package com.example.vidyasarthi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vidyasarthi.MainViewModel
import com.example.vidyasarthi.R
import com.example.vidyasarthi.core.transmission.DataTransmissionManager

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val logs by viewModel.logs.collectAsState()
    var hostPhone by remember { mutableStateOf("") }
    val contentTypes = listOf(
        stringResource(R.string.weather_content_type),
        stringResource(R.string.agriculture_content_type),
        stringResource(R.string.news_content_type),
        stringResource(R.string.nptel_content_type)
    )
    var selectedContent by remember { mutableStateOf(contentTypes[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "Home Screen" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleAndStatus(viewModel)
        ConnectionInputs(
            hostPhone, selectedContent, contentTypes,
            onHostPhoneChange = { hostPhone = it },
            onContentChange = { selectedContent = it }
        )
        ActionButtons(viewModel, hostPhone, selectedContent)
        Column(modifier = Modifier.weight(1f)) {
            LogsSection(logs)
        }
    }
}

@Composable
private fun TitleAndStatus(viewModel: MainViewModel) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val transmissionState by viewModel.transmissionState.collectAsState()

    Text(
        text = stringResource(R.string.home_screen_title),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(bottom = 32.dp)
    )

    Text(
        text = stringResource(R.string.status_prefix) + connectionStatus,
        style = MaterialTheme.typography.titleMedium,
        color = if (connectionStatus == "Connected") Color.Green else Color.Red,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    if (transmissionState == DataTransmissionManager.TransmissionState.RECEIVING ||
        transmissionState == DataTransmissionManager.TransmissionState.SENDING
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = stringResource(R.string.transmission_prefix) + transmissionState,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionInputs(
    hostPhone: String,
    selectedContent: String,
    contentTypes: List<String>,
    onHostPhoneChange: (String) -> Unit,
    onContentChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = hostPhone,
        onValueChange = onHostPhoneChange,
        label = { Text(stringResource(R.string.host_phone_number_label)) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    )

    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedContent,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.content_type_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = "Select Content Type" }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                contentTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onContentChange(type)
                            expanded = false
                        },
                        modifier = Modifier.semantics { contentDescription = "Content type $type" }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(viewModel: MainViewModel, hostPhone: String, selectedContent: String) {
    Button(
        onClick = { viewModel.connectToHost(hostPhone, selectedContent) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(stringResource(R.string.connect_via_sms_button))
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { viewModel.startVoiceCall(hostPhone) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(stringResource(R.string.start_voice_call_button))
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { viewModel.startDataTransmission() },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(stringResource(R.string.start_receiving_data_button))
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { viewModel.sendData("Hello Host, requesting $selectedContent data.", hostPhone) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(stringResource(R.string.send_test_data_button))
    }
}

@Composable
private fun LogsSection(logs: List<String>) {
    Spacer(modifier = Modifier.height(24.dp))

    Text(stringResource(R.string.logs_title), style = MaterialTheme.typography.titleSmall)
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
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