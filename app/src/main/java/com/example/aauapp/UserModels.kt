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

    // Navigation preferences
    val wheelchairOnly: Boolean = false,
    val avoidStairs: Boolean = true,
    val voiceEnabled: Boolean = false,
    val elevatorsOnly: Boolean = false,

    // MFA
    val mfaEnabled: Boolean = false,
    val mfaMethod: String? = null,

    // Stats
    val totalDistanceKm: Double = 4.2,
    val steps: Int = 5430
)