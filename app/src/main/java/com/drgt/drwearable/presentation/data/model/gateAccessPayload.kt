package com.drgt.drwearable.presentation.data.model

data class GateAccessPayload(
    val position: String,
    val isAccessGranted: Boolean
)