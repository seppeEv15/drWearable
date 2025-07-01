package com.example.drwearable.presentation.data.model

data class GateResponse(
    val position: String,
    val state: String
)

// Can maybe be placed in a separate gateState file
enum class GateState(val value: String) {
    CLOSED("Closed"),
    GETTING_DATA("GettingData"),
    WAITING_FOR_APPROVEMENT("WaitingForApprovement"),
    CARD_REJECTED("CardRejected"),
    ACCESS_GRANTED("AccessGranted"),
    ACCESS_DENIED("AccessDenied"),
    STAFF_ENTERED("StaffEntered"),
    PRINTING_TICKETS("PrintingTickets"),
    READY_FOR_USE("ReadyForUse");

    companion object {
        fun from(value: String): GateState? = GateState.entries.find { it.value == value }
    }
}