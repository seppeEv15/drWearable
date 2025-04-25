package com.example.drwearable.presentation.ui

import android.util.Log
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.drwearable.presentation.data.WaggledanceRepository
import com.example.drwearable.presentation.network.WaggleDanceApi
import com.example.drwearable.presentation.network.WaggleDanceService
import com.example.drwearable.presentation.ui.screens.gate.GateViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            Log.d("ViewModelFactory", "Initializing GateViewModel with repository")
            GateViewModel(repository = WaggledanceRepository(
                service = WaggleDanceApi.service
            ))
        }
    }
}
