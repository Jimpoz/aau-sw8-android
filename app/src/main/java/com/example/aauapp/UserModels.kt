package com.example.aauapp

data class UserProfileUi(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val membershipTier: String = "Member",
    val organizationId: String? = null,
    val role: String? = null,
    val authToken: String? = null,
    val campusId: String? = null,
    val buildingId: String? = null,
    val defaultFloorId: String? = null,
    val avoidStairs: Boolean = true,
    val voiceEnabled: Boolean = false,
    val elevatorsOnly: Boolean = false,
    val mfaEnabled: Boolean = false,
    val mfaMethod: String? = null,
    val totalDistanceKm: Double = 4.2,
    val steps: Int = 5430
)