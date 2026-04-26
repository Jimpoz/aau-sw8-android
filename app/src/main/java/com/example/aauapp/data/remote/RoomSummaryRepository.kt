package com.example.aauapp.data.remote

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class RoomSummaryRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun getRooms(): List<RoomListItemDto> {
        return api.getRoomSummaryRooms().names.map { roomName ->
            RoomListItemDto(room_name = roomName)
        }
    }

    suspend fun uploadRoomPhotos(
        roomName: String,
        contentResolver: ContentResolver,
        northUri: Uri,
        eastUri: Uri,
        southUri: Uri,
        westUri: Uri
    ): RoomObjectSetupResponseDto {
        val textType = "text/plain".toMediaType()

        return api.uploadRoomPhotos(
            roomName = roomName.toRequestBody(textType),
            north_image = uriToImagePart(contentResolver, northUri, "north_image"),
            east_image = uriToImagePart(contentResolver, eastUri, "east_image"),
            south_image = uriToImagePart(contentResolver, southUri, "south_image"),
            west_image = uriToImagePart(contentResolver, westUri, "west_image")
        )
    }

    private fun uriToImagePart(
        contentResolver: ContentResolver,
        uri: Uri,
        partName: String
    ): MultipartBody.Part {
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: ByteArray(0)

        val body = bytes.toRequestBody("image/jpeg".toMediaType())

        return MultipartBody.Part.createFormData(
            name = partName,
            filename = "$partName.jpg",
            body = body
        )
    }
}