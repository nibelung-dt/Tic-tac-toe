package com.tarasov.tic_tac_toe.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val username: String,
    val presenceStatus: PresenceStatus = PresenceStatus.OFFLINE
)

enum class PresenceStatus(val value: String) {
    ONLINE("online"),
    OFFLINE("offline")
}