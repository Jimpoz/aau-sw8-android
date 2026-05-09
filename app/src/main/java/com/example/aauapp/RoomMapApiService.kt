package com.example.aauapp

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class CampusListItemResponse(
    val id: String,
    val name: String,
)

data class MobileCampusMapResponse(
    @SerializedName("schema_version")
    val schemaVersion: String? = null,
    val campus: MobileCampusResponse,
)

data class MobileCampusResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val buildings: List<MobileBuildingResponse> = emptyList(),
)

data class MobileBuildingResponse(
    val id: String,
    val name: String,
    @SerializedName("short_name")
    val shortName: String? = null,
    @SerializedName("floor_count")
    val floorCount: Int? = null,
    val floors: List<MobileFloorResponse> = emptyList(),
)

data class MobileFloorResponse(
    val id: String,
    @SerializedName("floor_index")
    val floorIndex: Int,
    @SerializedName("display_name")
    val displayName: String? = null,
    val spaces: List<MobileSpaceResponse> = emptyList(),
)

data class MobileSpaceResponse(
    val id: String,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("short_name")
    val shortName: String? = null,
    @SerializedName("space_type")
    val spaceType: String? = null,
    val polygon: List<List<Float>> = emptyList(),
    @SerializedName("centroid_x")
    val centroidX: Float? = null,
    @SerializedName("centroid_y")
    val centroidY: Float? = null,
    @SerializedName("area_m2")
    val areaM2: Float? = null,
    val tags: List<String> = emptyList(),
    val capacity: Int? = null,
    @SerializedName("is_accessible")
    val isAccessible: Boolean? = null,
    @SerializedName("is_navigable")
    val isNavigable: Boolean? = null,
    @SerializedName("is_outdoor")
    val isOutdoor: Boolean? = null,
)

private data class ApiErrorResponse(
    val detail: String? = null,
)

data class RoomObjectDetectionSetupResponse(
    @SerializedName("room_name")
    val roomName: String,
    @SerializedName("room_objects")
    val roomObjects: List<String> = emptyList(),
    @SerializedName("room_object_counts")
    val roomObjectCounts: Map<String, Int> = emptyMap(),
    @SerializedName("room_text")
    val roomText: List<String> = emptyList(),
    @SerializedName("room_text_counts")
    val roomTextCounts: Map<String, Int> = emptyMap(),
    @SerializedName("stored_image_count")
    val storedImageCount: Int = 0,
    @SerializedName("stored_views")
    val storedViews: List<String> = emptyList(),
)

private data class UploadImage(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

class RoomMapApiService(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
    baseUrl: String = "",
    private val apiSecret: String = "",
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    fun fetchCampuses(): List<CampusListItemResponse> {
        ensureBaseUrlConfigured()

        val request = authorizedRequest("$normalizedBaseUrl/api/v1/mobile/campuses")
            .get()
            .build()

        return executeList(request, CampusListItemResponse::class.java)
    }

    fun fetchCampusLightMap(campusId: String): MobileCampusMapResponse {
        ensureBaseUrlConfigured()

        val request = authorizedRequest("$normalizedBaseUrl/api/v1/mobile/campuses/$campusId/map/light")
            .get()
            .build()

        return execute(request, MobileCampusMapResponse::class.java)
    }

    fun uploadRoomImages(
        contentResolver: ContentResolver,
        roomName: String,
        imagesByDirection: Map<UploadDirection, Uri>,
    ): RoomObjectDetectionSetupResponse {
        ensureBaseUrlConfigured()

        val uploadImages = UploadDirection.entries.associateWith { direction ->
            val uri = imagesByDirection[direction]
                ?: throw IOException("Missing ${direction.label.lowercase()} image.")
            contentResolver.toUploadImage(uri, direction)
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("room_name", roomName)
            .apply {
                UploadDirection.entries.forEach { direction ->
                    val image = uploadImages.getValue(direction)
                    addFormDataPart(
                        direction.formField,
                        image.fileName,
                        image.bytes.toRequestBody(image.mimeType.toMediaType()),
                    )
                }
            }
            .build()

        val request = authorizedRequest("$normalizedBaseUrl/api/v1/room-summary/room-objects/setup")
            .post(body)
            .build()

        return execute(request, RoomObjectDetectionSetupResponse::class.java)
    }

    private fun ensureBaseUrlConfigured() {
        if (normalizedBaseUrl.isBlank()) {
            throw IOException("Middleware base URL is not configured.")
        }
    }

    private fun <T> execute(request: Request, responseClass: Class<T>): T {
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException(parseErrorMessage(payload, response.code))
            }

            if (payload.isBlank()) {
                throw IOException("Middleware returned an empty response.")
            }

            return try {
                gson.fromJson(payload, responseClass)
                    ?: throw IOException("Middleware returned an unreadable response.")
            } catch (_: JsonSyntaxException) {
                throw IOException("Middleware returned invalid JSON.")
            }
        }
    }

    private fun <T> executeList(request: Request, elementClass: Class<T>): List<T> {
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException(parseErrorMessage(payload, response.code))
            }

            if (payload.isBlank()) {
                throw IOException("Middleware returned an empty response.")
            }

            return try {
                gson.fromJson(
                    payload,
                    com.google.gson.reflect.TypeToken.getParameterized(
                        List::class.java,
                        elementClass,
                    ).type,
                ) ?: emptyList()
            } catch (_: JsonSyntaxException) {
                throw IOException("Middleware returned invalid JSON.")
            }
        }
    }

    private fun parseErrorMessage(payload: String, code: Int): String {
        if (payload.isBlank()) {
            return "Request failed with status $code."
        }

        return try {
            val error = gson.fromJson(payload, ApiErrorResponse::class.java)
            error.detail?.takeIf { it.isNotBlank() } ?: "Request failed with status $code."
        } catch (_: JsonSyntaxException) {
            "Request failed with status $code."
        }
    }

    private fun authorizedRequest(url: String): Request.Builder {
        val builder = Request.Builder().url(url)

        if (apiSecret.isNotBlank()) {
            builder.header("X-API-Key", apiSecret)
        }

        return builder
    }
}

private fun ContentResolver.toUploadImage(uri: Uri, direction: UploadDirection): UploadImage {
    val bytes = openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException("Could not open the ${direction.label.lowercase()} image.")

    if (bytes.isEmpty()) {
        throw IOException("The ${direction.label.lowercase()} image is empty.")
    }

    val fileName = queryDisplayName(uri) ?: "${direction.name.lowercase()}.jpg"
    val mimeType = getType(uri) ?: "image/jpeg"

    return UploadImage(
        fileName = fileName,
        mimeType = mimeType,
        bytes = bytes,
    )
}

private fun ContentResolver.queryDisplayName(uri: Uri): String? {
    return query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    }
}