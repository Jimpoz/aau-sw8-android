package com.example.aauapp.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class LandmarkRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun register(
        name: String,
        spaceId: String,
        jpeg: ByteArray,
        latitude: Double? = null,
        longitude: Double? = null
    ): RegisteredLandmarkDto {
        val text = "text/plain".toMediaTypeOrNull()
        val namePart = name.toRequestBody(text)
        val spacePart = spaceId.toRequestBody(text)
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "landmark.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        val latPart = latitude?.toString()?.toRequestBody(text)
        val lngPart = longitude?.toString()?.toRequestBody(text)
        return api.registerLandmark(namePart, spacePart, imagePart, latPart, lngPart)
    }

    suspend fun list(
        spaceId: String? = null,
        buildingId: String? = null
    ): List<RegisteredLandmarkDto> = api.listLandmarks(spaceId, buildingId)

    suspend fun delete(id: String) {
        val response = api.deleteLandmark(id)
        if (!response.isSuccessful) {
            throw Exception("Could not delete landmark (${response.code()})")
        }
    }
}
