package com.example.aauapp

data class UserProfileUi(
    val id: String = "demo-user-1",
    val email: String = "rupesh@example.com",
    val displayName: String = "John Doe",
    val membershipTier: String = "Gold Member",
    val campusId: String? = null,
    val buildingId: String? = null,
    val defaultFloorId: String? = null,
    val avoidStairs: Boolean = true,
    val voiceEnabled: Boolean = false,
    val elevatorsOnly: Boolean = false
)