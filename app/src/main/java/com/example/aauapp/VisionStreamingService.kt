package com.example.aauapp

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageProxy
import com.example.aauapp.data.remote.AuthTokenStore
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream

data class RemoteDetection(
    val label: String = "",
    val confidence: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val is_landmark_match: Boolean? = null,
    val landmark_name: String? = null
)

data class RemoteLocation(
    val kind: String = "",
    val id: String = "",
    val name: String = "",
    val confidence: Double = 0.0,
    val space_id: String? = null,
    val building_id: String? = null,
    val building_name: String? = null,
    val campus_id: String? = null,
    val floor_id: String? = null,
    val floor_index: Int? = null
)

data class VisionStreamFrame(
    val detections: List<RemoteDetection> = emptyList(),
    val location: RemoteLocation? = null
)

class VisionStreamingService(
    private val onFrame: (VisionStreamFrame) -> Unit,
    private val onConnected: (Boolean) -> Unit
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var lastFrameSentAt = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var pendingCapture: ((ByteArray?) -> Unit)? = null

    fun captureNextFrame(completion: (ByteArray?) -> Unit) {
        pendingCapture = completion
    }

    fun connect(
        baseUrl: String,
        apiKey: String,
        facilityId: String
    ) {
        if (webSocket != null) return

        val wsBase = baseUrl
            .trimEnd('/')
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        val requestBuilder = Request.Builder()
            .url("$wsBase/api/v1/ml-vision/ws/stream/$facilityId")
            .addHeader("X-Api-Key", apiKey)

        AuthTokenStore.token?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        webSocket = client.newWebSocket(
            requestBuilder.build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onConnected(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        gson.fromJson(text, VisionStreamFrame::class.java)
                    }.onSuccess { frame ->
                        onFrame(frame)
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val text = bytes.utf8()
                    runCatching {
                        gson.fromJson(text, VisionStreamFrame::class.java)
                    }.onSuccess { frame ->
                        onFrame(frame)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onConnected(false)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onConnected(false)
                }
            }
        )
    }

    fun disconnect() {
        webSocket?.close(1000, "Camera closed")
        webSocket = null
        onConnected(false)
    }

    fun sendFrame(imageProxy: ImageProxy) {
        val capture = pendingCapture
        if (capture != null) {
            pendingCapture = null
            val captured = imageProxyToJpeg(imageProxy)
            mainHandler.post { capture(captured) }
            return
        }

        val now = System.currentTimeMillis()

        // Same idea as iOS: limit frame rate so backend is not overloaded.
        if (now - lastFrameSentAt < 70L) return

        lastFrameSentAt = now

        val jpeg = imageProxyToJpeg(imageProxy) ?: return
        webSocket?.send(ByteString.of(*jpeg))
    }

    private fun imageProxyToJpeg(imageProxy: ImageProxy): ByteArray? {
        val nv21 = imageProxyToNv21(imageProxy) ?: return null

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val jpegOut = ByteArrayOutputStream()

        yuvImage.compressToJpeg(
            Rect(0, 0, imageProxy.width, imageProxy.height),
            60,
            jpegOut
        )

        val rawJpeg = jpegOut.toByteArray()

        val bitmap = BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size)
            ?: return rawJpeg

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }

        val rotated = android.graphics.Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        val rotatedOut = ByteArrayOutputStream()
        rotated.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, rotatedOut)

        bitmap.recycle()
        rotated.recycle()

        return rotatedOut.toByteArray()
    }

    private fun imageProxyToNv21(imageProxy: ImageProxy): ByteArray? {
        val yBuffer = imageProxy.planes.getOrNull(0)?.buffer ?: return null
        val uBuffer = imageProxy.planes.getOrNull(1)?.buffer ?: return null
        val vBuffer = imageProxy.planes.getOrNull(2)?.buffer ?: return null

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}