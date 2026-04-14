package com.example.aauapp

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun uriToBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap {
    val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }

    return original.copy(Bitmap.Config.ARGB_8888, true)
}