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
            try {
                Log.d("ViewModelFactory", "Creating GateViewModel")
                GateViewModel(
                    repository = WaggledanceRepository(
                        service = WaggleDanceApi.service
                    )
                )
            } catch (e: Exception) {
                Log.e("ViewModelFactory", "Failed to create GateViewModel", e)
                throw e
            }
        }
    }
}
