package com.example.drwearable.presentation.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drwearable.presentation.ui.screens.WearApp
import com.example.drwearable.presentation.ui.screens.gate.GateScreen
import com.example.drwearable.presentation.ui.screens.gate.GateViewModel

@Composable
fun DrWearableApp(
) {
    val gateViewModel: GateViewModel = viewModel(factory = AppViewModelProvider.Factory)
    GateScreen(gateViewModel)
//    WearApp("Seppe")
}