package com.example.aauapp

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FloorPoint(
    val x: Float,
    val y: Float,
    val photoUri: String? = null
)

class FloorPlanViewModel(application: Application) :
    AndroidViewModel(application) {

    private val prefs =
        application.getSharedPreferences("floor_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    private val _points = MutableStateFlow<List<FloorPoint>>(emptyList())
    val points: StateFlow<List<FloorPoint>> = _points

    init {
        loadPoints()
    }

    private fun savePoints() {
        val json = gson.toJson(_points.value)
        prefs.edit().putString("points", json).apply()
    }

    private fun loadPoints() {
        val json = prefs.getString("points", null)
        if (json != null) {
            val type = object : TypeToken<List<FloorPoint>>() {}.type
            _points.value = gson.fromJson(json, type)
        }
    }

    fun addPoint(offset: Offset) {
        _points.value =
            _points.value + FloorPoint(offset.x, offset.y)
        savePoints()
    }

    fun attachPhoto(uri: String) {
        val list = _points.value.toMutableList()
        val last = list.lastOrNull() ?: return

        list[list.lastIndex] =
            last.copy(photoUri = uri)

        _points.value = list
        savePoints()
    }

    fun clearPoints() {
        _points.value = emptyList()
        savePoints()
    }
}