package com.example.aauapp.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class HealthResponseDto(
    val status: String
)

data class CampusDto(
    val id: String,
    val name: String
)

data class CampusMapLightDto(
    val schema_version: String? = null,
    val campus: CampusMapCampusDto
)

data class CampusMapCampusDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val buildings: List<BuildingMapDto> = emptyList()
)

data class BuildingMapDto(
    val id: String,
    val name: String,
    val short_name: String? = null,
    val floors: List<FloorMapDto> = emptyList()
)

data class FloorMapDto(
    val id: String,
    val floor_index: Int? = null,
    val display_name: String? = null
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
    val floor_index: Int? = null,
    val building_id: String? = null,
    val campus_id: String? = null,
    val centroid_x: Double? = null,
    val centroid_y: Double? = null,
    val centroid_lat: Double? = null,
    val centroid_lon: Double? = null,
    val polygon: List<List<Double>>? = null,
    val polygon_global: List<List<Double>>? = null,
    val is_accessible: Boolean? = true,
    val is_navigable: Boolean? = true,
    val capacity: Int? = null,
    val tags: List<String>? = emptyList()
)

data class RouteStepDto(
    val space_id: String,
    val display_name: String? = null,
    val space_type: String? = null,
    val floor_index: Int? = null,
    val building_id: String? = null,
    val centroid_x: Double? = null,
    val centroid_y: Double? = null,
    val instruction: String? = null,
    val cost: Double? = null
)

data class NavigationResultDto(
    val from_space_id: String,
    val to_space_id: String,
    val total_cost: Double,
    val steps: List<RouteStepDto> = emptyList()
)

data class AssistantChatRequest(
    val user_query: String,
    val campus_id: String
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
    val object_counts: Map<String, Int> = emptyMap(),
    val text_counts: Map<String, Int> = emptyMap()
)

data class RoomObjectSetupResponseDto(
    val room_name: String,
    val room_object_counts: Map<String, Int> = emptyMap(),
    val room_text_counts: Map<String, Int> = emptyMap(),
    val stored_views: List<String> = emptyList(),
    val room_summary: List<ViewSummaryDto> = emptyList()
)

interface BackendApi {

    @GET("health")
    suspend fun pingBackend(): HealthResponseDto

    @GET("api/v1/mobile/campuses")
    suspend fun getCampuses(): List<CampusDto>

    @GET("api/v1/mobile/campuses/{campusId}/map/light")
    suspend fun getCampusMapLight(
        @Path("campusId") campusId: String
    ): CampusMapLightDto

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
}