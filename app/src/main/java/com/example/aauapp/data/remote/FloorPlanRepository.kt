package com.example.aauapp.data.remote

class FloorPlanRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun getFloor(floorId: String): FloorDto {
        return api.getFloor(floorId)
    }

    suspend fun getFloorSpaces(floorId: String): List<SpaceDisplayDto> {
        return api.getFloorDisplay(floorId)
    }

    suspend fun navigate(
        fromSpaceId: String,
        toSpaceId: String,
        accessibleOnly: Boolean = false
    ): NavigationResultDto {
        return api.navigate(
            fromSpaceId = fromSpaceId,
            toSpaceId = toSpaceId,
            accessibleOnly = accessibleOnly
        )
    }
}