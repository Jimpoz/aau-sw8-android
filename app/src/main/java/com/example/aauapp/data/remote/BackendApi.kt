package com.example.aauapp.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class HealthResponseDto(
    val status: String
)

data class AuthSignupRequest(
    val email: String,
    val password: String,
    val full_name: String? = null,
    val organization_id: String? = null
)

data class AuthLoginRequest(
    val email: String,
    val password: String,
    val organization_id: String? = null
)

data class AuthMfaLoginRequest(
    val challenge_token: String,
    val code: String
)

data class AuthUserDto(
    val id: String,
    val email: String,
    val full_name: String? = null
)

data class AuthResponseDto(
    val mfa_required: Boolean = false,
    val mfa_method: String? = null,
    val challenge_token: String? = null,
    val challenge_expires_at: String? = null,
    val user: AuthUserDto? = null,
    val organization_id: String? = null,
    val role: String? = null,
    val token: String? = null,
    val token_expires_at: String? = null
)

data class AuthMeDto(
    val id: String,
    val email: String,
    val full_name: String? = null,
    val organization_id: String? = null,
    val role: String? = null,
    val mfa_enabled: Boolean = false,
    val mfa_method: String? = null
)

data class CampusDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val organization_id: String? = null,
    val organization_name: String? = null,
    val is_public: Boolean = false
)

data class CampusMapLightDto(
    val schema_version: String? = null,
    val campus: CampusMapCampusDto
)

data class CampusMapCampusDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val organization_id: String? = null,
    val buildings: List<BuildingMapDto> = emptyList()
)

data class VisibleBuildingDto(
    val id: String,
    val name: String,
    val short_name: String? = null,
    val address: String? = null,
    val origin_lat: Double,
    val origin_lng: Double,
    val campus_id: String? = null,
    val campus_name: String? = null,
    val organization_id: String? = null,
    val organization_name: String? = null,
    val is_public: Boolean = false
)

data class BuildingMapDto(
    val id: String,
    val name: String,
    val short_name: String? = null,
    val address: String? = null,
    val origin_lat: Double? = null,
    val origin_lng: Double? = null,
    val origin_bearing: Double? = null,
    val floor_count: Int? = null,
    val floors: List<FloorMapDto> = emptyList()
)

data class FloorMapDto(
    val id: String,
    val building_id: String? = null,
    val floor_index: Int? = null,
    val display_name: String? = null,
    val elevation_m: Double? = null,
    val floor_plan_url: String? = null,
    val floor_plan_scale: Double? = null,
    val floor_plan_origin_x: Double? = null,
    val floor_plan_origin_y: Double? = null
)

data class FloorDto(
    val id: String,
    val building_id: String? = null,
    val floor_index: Int? = null,
    val display_name: String? = null,
    val elevation_m: Double? = null,
    val floor_plan_url: String? = null,
    val floor_plan_scale: Double? = null,
    val floor_plan_origin_x: Double? = null,
    val floor_plan_origin_y: Double? = null
)

data class SpaceDisplayDto(
    val id: String,
    val display_name: String? = null,
    val short_name: String? = null,
    val space_type: String? = null,
    val floor_id: String? = null,
    val floor_index: Int? = null,
    val building_id: String? = null,
    val campus_id: String? = null,
    val width_m: Double? = null,
    val length_m: Double? = null,
    val area_m2: Double? = null,
    val centroid_x: Double? = null,
    val centroid_y: Double? = null,
    val centroid_lat: Double? = null,
    val centroid_lng: Double? = null,
    val polygon: List<List<Double>>? = null,
    val polygon_global: List<List<Double>>? = null,
    val is_accessible: Boolean? = true,
    val is_navigable: Boolean? = true,
    val is_outdoor: Boolean? = false,
    val capacity: Int? = null,
    val tags: List<String>? = emptyList()
)

data class RouteStepDto(
    val space_id: String,
    val display_name: String? = null,
    val space_type: String? = null,
    val floor_id: String? = null,
    val floor_index: Int? = null,
    val building_id: String? = null,
    val centroid_x: Double? = null,
    val centroid_y: Double? = null,
    val centroid_lat: Double? = null,
    val centroid_lng: Double? = null,
    val instruction: String? = null,
    val cost: Double? = null
)

data class NavigationResultDto(
    val from_space_id: String,
    val to_space_id: String,
    val total_cost: Double,
    val steps: List<RouteStepDto> = emptyList(),
    val polyline: List<List<Double>> = emptyList()
)

data class AssistantChatRequest(
    val user_query: String,
    val campus_id: String,
    val building_id: String? = null,
    val user_lat: Double? = null,
    val user_lon: Double? = null,
    val floor_index: Int? = null
)

data class AssistantChatResponse(
    val answer: String,
    val sources: List<String> = emptyList()
)

data class RoomNamesResponseDto(
    val names: List<String> = emptyList()
)

data class RoomListItemDto(
    val room_id: String? = null,
    val room_name: String
)

data class ViewSummaryDto(
    val direction: String? = null,
    val summary: String? = null,
    val objects: List<String> = emptyList(),
    val object_counts: Map<String, Int> = emptyMap(),
    val text: List<String> = emptyList(),
    val text_counts: Map<String, Int> = emptyMap()
)

data class RoomObjectSetupResponseDto(
    val room_name: String,
    val room_objects: List<String> = emptyList(),
    val room_object_counts: Map<String, Int> = emptyMap(),
    val room_text: List<String> = emptyList(),
    val room_text_counts: Map<String, Int> = emptyMap(),
    val stored_image_count: Int = 0,
    val stored_views: List<String> = emptyList(),
    val room_summary: List<ViewSummaryDto> = emptyList()
)

data class RegisteredLandmarkDto(
    val id: String,
    val name: String,
    val space_id: String,
    val building_id: String? = null,
    val campus_id: String? = null,
    val image_width: Int? = null,
    val image_height: Int? = null,
    val created_by: String? = null,
    val created_at: String? = null
)

data class MfaConfirmRequestDto(
    val code: String
)

data class MfaDisableRequestDto(
    val password: String
)

data class MfaSetupResponseDto(
    val secret: String,
    val provisioning_uri: String,
    val recovery_codes: List<String> = emptyList()
)

data class MfaStateResponseDto(
    val mfa_enabled: Boolean,
    val mfa_method: String? = null
)

data class MfaEmailSetupResponseDto(
    val setup_challenge_token: String,
    val challenge_expires_at: String? = null,
    val recovery_codes: List<String> = emptyList()
)

data class MfaEmailConfirmRequestDto(
    val challenge_token: String,
    val code: String
)

data class PasswordChangeRequestDto(
    val current_password: String,
    val new_password: String
)

data class PasswordForgotRequestDto(
    val email: String
)

data class PasswordResetRequestDto(
    val email: String,
    val code: String,
    val new_password: String
)

data class DeleteAccountRequestDto(
    val password: String
)

data class WifiFingerprintRequest(
    val space_id: String,
    val floor_id: String? = null,
    val readings: Map<String, Float>,
    val rtt_distances_mm: Map<String, Float>? = null,
    val sample_count: Int = 1
)

data class WifiFingerprintResponse(
    val id: String,
    val space_id: String,
    val floor_id: String? = null,
    val sample_count: Int = 1
)

data class WifiLocateRequest(
    val floor_id: String,
    val readings: Map<String, Float>,
    val rtt_distances_mm: Map<String, Float>? = null
)

data class WifiLocateResponse(
    val space_id: String? = null,
    val confidence: Float = 0f,
    val supporting_count: Int = 0,
    val x: Double? = null,
    val y: Double? = null,
    val method: String = "rssi_knn"
)

data class WifiAccessPointRequest(
    val bssid: String,
    val ssid: String? = null,
    val floor_id: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val supports_rtt: Boolean = false
)

data class WifiFloorSurveyResponse(
    val floor_id: String,
    val total_fingerprints: Int,
    val per_space_counts: Map<String, Int> = emptyMap()
)

data class ActivityTotalsDto(
    val day: String,
    val distance_m: Double = 0.0,
    val steps: Int = 0
)

data class ActivityIncrementRequest(
    val day: String? = null,
    val distance_m: Double = 0.0,
    val steps: Int = 0
)

interface BackendApi {

    @GET("health")
    suspend fun pingBackend(): HealthResponseDto

    @POST("api/v1/auth/signup")
    suspend fun signup(
        @Body request: AuthSignupRequest
    ): AuthResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: AuthLoginRequest
    ): AuthResponseDto

    @POST("api/v1/auth/login/mfa")
    suspend fun loginMfa(
        @Body request: AuthMfaLoginRequest
    ): AuthResponseDto

    @POST("api/v1/auth/guest")
    suspend fun guest(): AuthResponseDto

    @GET("api/v1/auth/me")
    suspend fun me(): AuthMeDto

    @HTTP(method = "DELETE", path = "api/v1/auth/me", hasBody = true)
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequestDto
    ): Response<Unit>

    @POST("api/v1/auth/mfa/setup")
    suspend fun setupMfa(): MfaSetupResponseDto

    @POST("api/v1/auth/mfa/confirm")
    suspend fun confirmMfa(
        @Body request: MfaConfirmRequestDto
    ): MfaStateResponseDto

    @POST("api/v1/auth/mfa/disable")
    suspend fun disableMfa(
        @Body request: MfaDisableRequestDto
    ): MfaStateResponseDto

    @POST("api/v1/auth/mfa/email/setup")
    suspend fun setupMfaEmail(): MfaEmailSetupResponseDto

    @POST("api/v1/auth/mfa/email/confirm")
    suspend fun confirmMfaEmail(
        @Body request: MfaEmailConfirmRequestDto
    ): MfaStateResponseDto

    @POST("api/v1/auth/password/change")
    suspend fun changePassword(
        @Body request: PasswordChangeRequestDto
    ): Response<Unit>

    @POST("api/v1/auth/password/forgot")
    suspend fun forgotPassword(
        @Body request: PasswordForgotRequestDto
    ): Response<Unit>

    @POST("api/v1/auth/password/reset")
    suspend fun resetPassword(
        @Body request: PasswordResetRequestDto
    ): Response<Unit>

    @GET("api/v1/mobile/campuses")
    suspend fun getCampuses(): List<CampusDto>

    @GET("api/v1/mobile/campuses/{campusId}/map/light")
    suspend fun getCampusMapLight(
        @Path("campusId") campusId: String
    ): CampusMapLightDto

    @GET("api/v1/campuses/{campusId}/export")
    suspend fun exportCampus(
        @Path("campusId") campusId: String
    ): CampusMapLightDto

    @GET("api/v1/buildings/visible")
    suspend fun getVisibleBuildings(): List<VisibleBuildingDto>

    @GET("api/v1/buildings/{buildingId}/floors")
    suspend fun getBuildingFloors(
        @Path("buildingId") buildingId: String
    ): List<FloorMapDto>

    @GET("api/v1/floors/{floorId}")
    suspend fun getFloor(
        @Path("floorId") floorId: String
    ): FloorDto

    @GET("api/v1/floors/{floorId}/display")
    suspend fun getFloorDisplay(
        @Path("floorId") floorId: String
    ): List<SpaceDisplayDto>

    @GET("api/v1/navigate")
    suspend fun navigate(
        @Query("from") fromSpaceId: String,
        @Query("to") toSpaceId: String,
        @Query("accessible_only") accessibleOnly: Boolean = false
    ): NavigationResultDto

    @POST("api/v1/assistant/chat")
    suspend fun chatWithAssistant(
        @Body request: AssistantChatRequest
    ): AssistantChatResponse

    @GET("api/v1/room-summary/rooms")
    suspend fun getRoomSummaryRooms(): RoomNamesResponseDto

    @Multipart
    @POST("api/v1/room-summary/room-objects/setup")
    suspend fun uploadRoomPhotos(
        @Part("room_name") roomName: RequestBody,
        @Part north_image: MultipartBody.Part,
        @Part east_image: MultipartBody.Part,
        @Part south_image: MultipartBody.Part,
        @Part west_image: MultipartBody.Part
    ): RoomObjectSetupResponseDto

    @Multipart
    @POST("api/v1/landmarks")
    suspend fun registerLandmark(
        @Part("name") name: RequestBody,
        @Part("space_id") spaceId: RequestBody,
        @Part image: MultipartBody.Part
    ): RegisteredLandmarkDto

    @GET("api/v1/landmarks")
    suspend fun listLandmarks(
        @Query("space_id") spaceId: String? = null,
        @Query("building_id") buildingId: String? = null
    ): List<RegisteredLandmarkDto>

    @HTTP(method = "DELETE", path = "api/v1/landmarks/{id}", hasBody = false)
    suspend fun deleteLandmark(
        @Path("id") id: String
    ): Response<Unit>

    @POST("api/v1/positioning/fingerprints")
    suspend fun createWifiFingerprint(
        @Body request: WifiFingerprintRequest
    ): WifiFingerprintResponse

    @GET("api/v1/positioning/fingerprints")
    suspend fun wifiFloorSurvey(
        @Query("floor_id") floorId: String
    ): WifiFloorSurveyResponse

    @POST("api/v1/positioning/locate")
    suspend fun wifiLocate(
        @Body request: WifiLocateRequest
    ): WifiLocateResponse

    @POST("api/v1/positioning/access-points")
    suspend fun upsertWifiAccessPoint(
        @Body request: WifiAccessPointRequest
    ): Response<Unit>

    @GET("api/v1/me/activity")
    suspend fun getActivity(
        @Query("day") day: String? = null
    ): ActivityTotalsDto

    @POST("api/v1/me/activity")
    suspend fun addActivity(
        @Body request: ActivityIncrementRequest
    ): ActivityTotalsDto
}