package com.example.vidyasarthi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vidyasarthi.MainViewModel
import com.example.vidyasarthi.R

@Composable
fun ContentScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.weather_content_type),
        stringResource(R.string.agriculture_content_type),
        stringResource(R.string.news_content_type),
        stringResource(R.string.nptel_content_type)
    )

    Column(modifier = modifier.semantics { contentDescription = "Content Screen" }) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title) },
                    modifier = Modifier.semantics { contentDescription = "$title tab" })
            }
        }

        when (selectedTab) {
            0 -> ContentList(viewModel, tabs[0])
            1 -> ContentList(viewModel, tabs[1])
            2 -> ContentList(viewModel, tabs[2])
            3 -> ContentList(viewModel, tabs[3])
        }
    }
}

@Composable
fun ContentList(viewModel: MainViewModel, category: String) {
    val receivedText by viewModel.receivedContentText.collectAsState()
    // In a real app, this would be a list of content for the given category
    val items = if (receivedText.isNotEmpty() && category == stringResource(R.string.weather_content_type)) listOf(receivedText) else emptyList()

    LazyColumn(modifier = Modifier.padding(16.dp).semantics { contentDescription = "$category content list" }) {
        if (items.isEmpty()) {
            item {
                Text(stringResource(R.string.no_content_available, category))
            }
        }
        items(items) { item ->
            Card(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { /* Handle click */ }
                    .semantics { contentDescription = "Content item: $item" },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = item)
                }
            }
        }
    }
}