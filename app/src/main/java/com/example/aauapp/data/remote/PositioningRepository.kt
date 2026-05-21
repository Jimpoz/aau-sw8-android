package com.example.aauapp.data.remote

class PositioningRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun locate(
        floorId: String,
        readings: Map<String, Float>,
        rttDistancesMm: Map<String, Float>?
    ): WifiLocateResponse =
        api.wifiLocate(
            WifiLocateRequest(
                floor_id = floorId,
                readings = readings,
                rtt_distances_mm = rttDistancesMm?.ifEmpty { null }
            )
        )

    suspend fun saveFingerprint(
        spaceId: String,
        floorId: String?,
        readings: Map<String, Float>,
        rttDistancesMm: Map<String, Float>?
    ): WifiFingerprintResponse =
        api.createWifiFingerprint(
            WifiFingerprintRequest(
                space_id = spaceId,
                floor_id = floorId,
                readings = readings,
                rtt_distances_mm = rttDistancesMm?.ifEmpty { null }
            )
        )

    suspend fun floorSurvey(floorId: String): WifiFloorSurveyResponse =
        api.wifiFloorSurvey(floorId)
}
