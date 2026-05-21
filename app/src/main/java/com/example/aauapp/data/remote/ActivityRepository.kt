package com.example.aauapp.data.remote

class ActivityRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun getToday(day: String): ActivityTotalsDto = api.getActivity(day)

    suspend fun addToday(day: String, distanceM: Double, steps: Int): ActivityTotalsDto =
        api.addActivity(ActivityIncrementRequest(day = day, distance_m = distanceM, steps = steps))
}
