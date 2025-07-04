package com.drgt.drwearable.presentation.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drgt.drwearable.presentation.data.WaggledanceRepository
import com.drgt.drwearable.presentation.network.WaggleDanceApi
import com.drgt.drwearable.presentation.ui.screens.gate.GateViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                as Application

            try {
                Log.d("ViewModelFactory", "Creating GateViewModel")
                GateViewModel(
                    repository = WaggledanceRepository(
                        service = WaggleDanceApi.service
                    ),
                    application = application
                )
            } catch (e: Exception) {
                Log.e("ViewModelFactory", "Failed to create GateViewModel", e)
                throw e
            }
        }
    }
}
