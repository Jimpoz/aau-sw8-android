package com.example.aauapp.data.remote

class BackendRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun pingBackend(): HealthResponseDto = api.pingBackend()

    suspend fun getCampuses(): List<CampusDto> = api.getCampuses()

    suspend fun getCampusMapLight(campusId: String): CampusMapLightDto {
        return api.getCampusMapLight(campusId)
    }
}