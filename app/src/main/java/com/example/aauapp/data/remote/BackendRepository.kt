package com.example.aauapp.data.remote

class BackendRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun pingBackend(): HealthResponseDto = api.pingBackend()

    suspend fun getCampuses(): List<CampusDto> = api.getCampuses()

    suspend fun getCampusMapLight(campusId: String): CampusMapLightDto {
        return try {
            api.getCampusMapLight(campusId)
        } catch (_: Exception) {
            api.exportCampus(campusId)
        }
    }

    suspend fun getBuildingFloors(buildingId: String): List<FloorMapDto> {
        return api.getBuildingFloors(buildingId)
    }

    suspend fun getFloor(floorId: String): FloorDto = api.getFloor(floorId)

    suspend fun getFloorDisplay(floorId: String): List<SpaceDisplayDto> {
        return api.getFloorDisplay(floorId)
    }

    suspend fun navigate(
        fromSpaceId: String,
        toSpaceId: String,
        accessibleOnly: Boolean = false
    ): NavigationResultDto {
        return api.navigate(fromSpaceId, toSpaceId, accessibleOnly)
    }
}