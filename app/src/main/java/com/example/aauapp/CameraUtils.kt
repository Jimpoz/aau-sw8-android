package com.example.aauapp

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun createImageUri(context: Context): Uri {
    val imageFile = File(
        context.cacheDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )

    return FileProvider.getUriForFile(
        context,
        "com.example.aauapp.provider",
        imageFile
    )
}