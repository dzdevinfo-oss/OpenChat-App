package com.openchat.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openchat.app.ui.viewmodels.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> Text("Page 1: App logo + OpenChat — AI in your pocket")
                1 -> Text("Page 2: Connect any AI — API Config preview")
                2 -> Text("Page 3: Full IDE in your phone — Workspace preview")
                3 -> Text("Page 4: Let's get started")
            }
        }
        
        if (pagerState.currentPage == 3) {
            Button(onClick = { viewModel.setFirstLaunchFinished(); onFinish() }) {
                Text("Set up API Key")
            }
            TextButton(onClick = { viewModel.setFirstLaunchFinished(); onFinish() }) {
                Text("Skip for now")
            }
        }
    }
}
