package com.example.aauapp.data.remote

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class RoomSummaryRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun getRooms(): List<RoomListItemDto> {
        return api.getRoomSummaryRooms().names.map {
            RoomListItemDto(room_id = it, room_name = it)
        }
    }

    suspend fun uploadRoomPhotos(
        contentResolver: ContentResolver,
        roomName: String,
        northUri: Uri,
        eastUri: Uri,
        southUri: Uri,
        westUri: Uri
    ): RoomObjectSetupResponseDto {
        val roomNameBody = roomName.toRequestBody("text/plain".toMediaTypeOrNull())

        return api.uploadRoomPhotos(
            roomName = roomNameBody,
            north_image = createImagePart(contentResolver, "north_image", northUri),
            east_image = createImagePart(contentResolver, "east_image", eastUri),
            south_image = createImagePart(contentResolver, "south_image", southUri),
            west_image = createImagePart(contentResolver, "west_image", westUri)
        )
    }

    private fun createImagePart(
        contentResolver: ContentResolver,
        partName: String,
        uri: Uri
    ): MultipartBody.Part {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image: $uri")

        return MultipartBody.Part.createFormData(
            partName,
            "$partName.jpg",
            bytes.toRequestBody("image/*".toMediaTypeOrNull())
        )
    }
}