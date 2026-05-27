package com.example.aauapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.data.remote.RouteStepDto
import com.example.aauapp.data.remote.SpaceDisplayDto
import com.example.aauapp.ui.theme.Blue600
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// default to AAU CPH
private val DEFAULT_CENTER = GeoPoint(55.6526, 12.5417)

private const val OFF_ROUTE_METERS = 15.0
private const val OFF_ROUTE_STREAK = 2

private const val MANUAL_FLOOR_PIN_MS = 5L * 60 * 1000

private const val INDOOR_ZOOM_THRESHOLD = 18.0
private const val SNAP_RADIUS_METERS = 80.0

private const val AVG_WALKING_SPEED_MS = 1.4
private const val SIM_TICK_MS = 500L
private const val SIM_FIX_FRESH_MS = 8_000L
private const val ARRIVAL_RADIUS_METERS = 6.0

@Composable
fun GoogleMapScreen(
    floorId: String?,
    floorName: String = "Ground Floor",
    canCalibrate: Boolean = false,
    viewModel: FloorPlanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val locationService = remember {
        (context.applicationContext as AAUAppApplication).appContainer.locationService
    }
    val fix by locationService.fix.collectAsState()
    DisposableEffect(Unit) {
        locationService.startUpdates()
        onDispose { locationService.stopUpdates() }
    }

    val tts = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val positioningManager = remember { PositioningManager(context) }

    val barometerService = remember {
        (context.applicationContext as AAUAppApplication).appContainer.barometerService
    }
    val barometerFloorIndex by barometerService.currentFloorIndex.collectAsState()
    DisposableEffect(Unit) {
        onDispose { barometerService.stop() }
    }
    LaunchedEffect(uiState.availableFloors, uiState.floorIndex) {
        val floors = uiState.availableFloors
        val baseline = uiState.floorIndex
        if (floors.isNotEmpty() && baseline != null) {
            barometerService.start(
                floors = floors.map { BarometerFloor(it.floor_index ?: 0, it.elevation_m) },
                baselineFloorIndex = baseline
            )
        }
    }
    LaunchedEffect(barometerFloorIndex) {
        val idx = barometerFloorIndex ?: return@LaunchedEffect
        if (idx == uiState.floorIndex) return@LaunchedEffect
        val pinnedAt = uiState.manualFloorPinAt
        if (pinnedAt != null && System.currentTimeMillis() - pinnedAt < MANUAL_FLOOR_PIN_MS) {
            return@LaunchedEffect
        }
        uiState.availableFloors.firstOrNull { it.floor_index == idx }?.let {
            viewModel.loadFloor(it.id)
        }
    }

    LaunchedEffect(uiState.isNavigating, uiState.floorId) {
        if (!uiState.isNavigating) return@LaunchedEffect
        val fId = uiState.floorId ?: return@LaunchedEffect
        var offRouteStreak = 0
        while (isActive) {
            val res = runCatching { positioningManager.locate(fId) }.getOrNull()
            val sid = res?.space_id
            if (sid != null) {
                viewModel.updateLiveLocation(sid)

                val snapshot = viewModel.uiState.value
                val located = snapshot.spaces.firstOrNull { it.id == sid }
                val lat = located?.centroid_lat
                val lng = located?.centroid_lng
                val route = snapshot.routePolyline
                    .filter { it.size >= 2 }
                    .map { GeoPoint(it[0], it[1]) }
                if (lat != null && lng != null && route.size >= 2) {
                    val deviation = routeDeviation(route, GeoPoint(lat, lng))
                    if (deviation > OFF_ROUTE_METERS) {
                        offRouteStreak++
                        if (offRouteStreak >= OFF_ROUTE_STREAK) {
                            offRouteStreak = 0
                            viewModel.rerouteFrom(sid)
                        }
                    } else {
                        offRouteStreak = 0
                    }
                }
            }
            delay(4000L)
        }
    }

    var simulatedPosition by remember { mutableStateOf<GeoPoint?>(null) }
    var lastForcedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(uiState.forcedUserSpaceId) {
        if (uiState.forcedUserSpaceId != null) lastForcedAt = System.currentTimeMillis()
    }
    LaunchedEffect(uiState.isNavigating, uiState.routePolyline) {
        if (!uiState.isNavigating) {
            simulatedPosition = null
            return@LaunchedEffect
        }
        val route = uiState.routePolyline
            .filter { it.size >= 2 }
            .map { GeoPoint(it[0], it[1]) }
        if (route.size < 2) {
            simulatedPosition = null
            return@LaunchedEffect
        }

        var simArcMeters = run {
            val snapshot = viewModel.uiState.value
            val forced = snapshot.spaces.firstOrNull { it.id == snapshot.forcedUserSpaceId }
            val anchor = forced?.let {
                val la = it.centroid_lat; val ln = it.centroid_lng
                if (la != null && ln != null) GeoPoint(la, ln) else null
            }
            if (anchor != null) projectArcLengthMeters(route, anchor) else 0.0
        }
        var lastTick = System.currentTimeMillis()
        simulatedPosition = null

        while (isActive) {
            delay(SIM_TICK_MS)
            val now = System.currentTimeMillis()
            val dtSec = (now - lastTick) / 1000.0
            lastTick = now

            val snapshot = viewModel.uiState.value

            val forced = snapshot.spaces.firstOrNull { it.id == snapshot.forcedUserSpaceId }
            val anchor = forced?.let {
                val la = it.centroid_lat; val ln = it.centroid_lng
                if (la != null && ln != null) GeoPoint(la, ln) else null
            }
            val fixIsFresh = (now - lastForcedAt) < SIM_FIX_FRESH_MS

            if (fixIsFresh && anchor != null) {
                simArcMeters = projectArcLengthMeters(route, anchor)
                simulatedPosition = null
                continue
            }

            simArcMeters += dtSec * AVG_WALKING_SPEED_MS
            val simPos = coordAtArcLengthMeters(route, simArcMeters)
            simulatedPosition = simPos

            if (simPos != null && haversineMeters(simPos, route.last()) <= ARRIVAL_RADIUS_METERS) {
                tts.speak("You have reached your destination.")
                viewModel.stopNavigation()
                break
            }
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(floorId) {
        if (!floorId.isNullOrBlank() && floorId != uiState.floorId) {
            viewModel.loadFloor(floorId)
        }
    }

    val suggestions = remember(searchText, uiState.spaces) {
        val q = searchText.trim()
        if (q.isBlank()) emptyList()
        else uiState.spaces.filter {
            it.display_name.orEmpty().contains(q, ignoreCase = true) ||
                it.short_name.orEmpty().contains(q, ignoreCase = true) ||
                it.id.contains(q, ignoreCase = true)
        }.take(5)
    }

    val selectedSpace = uiState.spaces.firstOrNull { it.id == uiState.selectedSpaceId }
    val routeCardVisible = selectedSpace != null || uiState.routeSteps.isNotEmpty()

    val navAnchor: GeoPoint? = run {
        val forced = uiState.spaces.firstOrNull { it.id == uiState.forcedUserSpaceId }
        val fLat = forced?.centroid_lat
        val fLng = forced?.centroid_lng
        val gLat = fix.latitude
        val gLng = fix.longitude
        when {
            simulatedPosition != null -> simulatedPosition
            fLat != null && fLng != null -> GeoPoint(fLat, fLng)
            gLat != null && gLng != null -> GeoPoint(gLat, gLng)
            else -> null
        }
    }

    val stepsLeft = if (uiState.isNavigating && navAnchor != null) {
        remainingSteps(uiState.routeSteps, navAnchor)
    } else {
        uiState.routeSteps.size
    }

    val bottomControlsPadding = if (routeCardVisible) 188.dp else 32.dp

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var rotationOverlayRef by remember { mutableStateOf<RotationGestureOverlay?>(null) }
    var fittedFloorId by remember { mutableStateOf<String?>(null) }
    var zoomBelowThreshold by remember { mutableStateOf(true) }
    val floorPolygons = remember { mutableMapOf<String, Polygon>() }
    var lastSpacesKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchVisibleBuildings() }

    fun flyTo(lat: Double?, lng: Double?) {
        if (lat == null || lng == null) return
        mapViewRef?.controller?.animateTo(GeoPoint(lat, lng))
        mapViewRef?.controller?.setZoom(20.0)
    }

    LaunchedEffect(uiState.routePolyline, uiState.routePolylinesByFloor, uiState.floorIndex) {
        val floorIndex = uiState.floorIndex
        val pts: List<GeoPoint> = when {
            floorIndex != null && uiState.routePolylinesByFloor.containsKey(floorIndex) ->
                uiState.routePolylinesByFloor[floorIndex]!!
                    .filter { it.size >= 2 }
                    .map { GeoPoint(it[0], it[1]) }
            else ->
                uiState.routePolyline
                    .filter { it.size >= 2 }
                    .map { GeoPoint(it[0], it[1]) }
        }
        if (pts.size >= 2) {
            val bbox = BoundingBox.fromGeoPoints(pts)
            mapViewRef?.post { mapViewRef?.zoomToBoundingBox(bbox, true, 160) }
        }
    }

    LaunchedEffect(uiState.pendingFocusSpaceId, uiState.spaces) {
        val id = uiState.pendingFocusSpaceId ?: return@LaunchedEffect
        val space = uiState.spaces.firstOrNull { it.id == id } ?: return@LaunchedEffect
        fittedFloorId = uiState.floorId   // suppress the floor-wide auto-fit
        flyTo(space.centroid_lat, space.centroid_lng)
        viewModel.clearFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE9EEF5))) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    setFlingEnabled(true)
                    minZoomLevel = 4.0
                    maxZoomLevel = 22.0
                    controller.setZoom(16.0)
                    controller.setCenter(DEFAULT_CENTER)

                    val rotation = RotationGestureOverlay(this).apply {
                        isEnabled = true
                    }
                    overlays.add(rotation)
                    rotationOverlayRef = rotation

                    val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    if (hasLocationPermission) locOverlay.enableMyLocation()
                    overlays.add(locOverlay)
                    locationOverlayRef = locOverlay

                    addMapListener(object : MapListener {
                        private var lastSnapProbeMs = 0L

                        override fun onScroll(event: ScrollEvent?): Boolean {
                            updateZoomFlag()
                            maybeProbeBuildingSnap()
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            updateZoomFlag()
                            maybeProbeBuildingSnap()
                            return false
                        }

                        private fun updateZoomFlag() {
                            val below = zoomLevelDouble < INDOOR_ZOOM_THRESHOLD
                            if (below != zoomBelowThreshold) {
                                zoomBelowThreshold = below
                            }
                        }

                        private fun maybeProbeBuildingSnap() {
                            val now = System.currentTimeMillis()
                            if (now - lastSnapProbeMs < 250) return
                            lastSnapProbeMs = now
                            val center = mapCenter as? GeoPoint ?: return
                            maybeAutoSnapToBuilding(
                                center = center,
                                zoom = zoomLevelDouble,
                                buildings = uiState.visibleBuildings,
                                currentBuildingId = uiState.currentBuildingId,
                                onSnap = { id -> viewModel.enterBuilding(id) }
                            )
                        }
                    })

                    onResume()
                    mapViewRef = this
                }
            },
            update = { mapView ->
                val locOverlay = locationOverlayRef

                val forcedSpace = uiState.spaces.firstOrNull { it.id == uiState.forcedUserSpaceId }
                val exactLat = uiState.forcedUserLatitude
                val exactLng = uiState.forcedUserLongitude
                val forcedPoint = when {
                    exactLat != null && exactLng != null ->
                        GeoPoint(exactLat, exactLng)
                    forcedSpace?.centroid_lat != null && forcedSpace.centroid_lng != null ->
                        GeoPoint(forcedSpace.centroid_lat, forcedSpace.centroid_lng)
                    else -> null
                }
                val liveDot: GeoPoint? = simulatedPosition ?: forcedPoint

                if (hasLocationPermission && liveDot == null) {
                    locOverlay?.enableMyLocation()
                } else {
                    locOverlay?.disableMyLocation()
                }

                val rotOverlay = rotationOverlayRef

                val spacesKey = uiState.spaces.joinToString("|") { it.id }
                if (spacesKey != lastSpacesKey) {
                    floorPolygons.values.forEach { mapView.overlays.remove(it) }
                    floorPolygons.clear()
                    uiState.spaces.forEach { space ->
                        val pts = space.polygon_global.orEmpty()
                            .filter { it.size >= 2 }
                            .map { GeoPoint(it[0], it[1]) }
                        if (pts.size >= 3) {
                            val poly = Polygon(mapView).apply {
                                points = pts
                                fillPaint.color = floorFillColor(space).toArgb()
                                outlinePaint.color = Color(0xFF1F2937).toArgb()
                                outlinePaint.strokeWidth = 3f
                                title = space.display_name ?: space.short_name ?: space.id
                                setOnClickListener { _, _, _ ->
                                    viewModel.selectSpace(space.id)
                                    true
                                }
                            }
                            mapView.overlays.add(poly)
                            floorPolygons[space.id] = poly
                        }
                    }
                    lastSpacesKey = spacesKey
                }

                // Cheap: just retint the selected polygon.
                floorPolygons.forEach { (id, poly) ->
                    val space = uiState.spaces.firstOrNull { it.id == id } ?: return@forEach
                    val selected = id == uiState.selectedSpaceId
                    poly.fillPaint.color =
                        (if (selected) Color(0xEE009DFF) else floorFillColor(space)).toArgb()
                    poly.outlinePaint.strokeWidth = if (selected) 6f else 3f
                }

                val preserved = floorPolygons.values.toSet()
                mapView.overlays.removeAll {
                    it !== locOverlay && it !== rotOverlay && it !in preserved
                }

                val currentFloorIndex = uiState.floorIndex
                val routePts: List<GeoPoint> = when {
                    currentFloorIndex != null &&
                        uiState.routePolylinesByFloor.containsKey(currentFloorIndex) ->
                        uiState.routePolylinesByFloor[currentFloorIndex]!!
                            .filter { it.size >= 2 }
                            .map { GeoPoint(it[0], it[1]) }
                    else ->
                        uiState.routePolyline
                            .filter { it.size >= 2 }
                            .map { GeoPoint(it[0], it[1]) }
                }
                if (routePts.size >= 2) {
                    val (walked, remainingRaw) =
                        if (uiState.isNavigating && navAnchor != null) {
                            splitRoute(routePts, navAnchor)
                        } else {
                            emptyList<GeoPoint>() to routePts
                        }

                    val origin: GeoPoint? = liveDot ?: run {
                        val gLat = fix.latitude
                        val gLng = fix.longitude
                        if (gLat != null && gLng != null) GeoPoint(gLat, gLng) else null
                    }
                    val remaining: List<GeoPoint> =
                        if (origin != null) listOf(origin) + remainingRaw else remainingRaw

                    if (walked.size >= 2) {
                        val trail = Polyline(mapView).apply {
                            setPoints(walked)
                            outlinePaint.color = Color(0x66667085).toArgb()
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                        }
                        mapView.overlays.add(trail)
                    }

                    if (remaining.size >= 2) {
                        val halo = Polyline(mapView).apply {
                            setPoints(remaining)
                            outlinePaint.color = Color.White.toArgb()
                            outlinePaint.strokeWidth = 20f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                        }
                        val main = Polyline(mapView).apply {
                            setPoints(remaining)
                            outlinePaint.color = Color(0xFF0A84FF).toArgb()
                            outlinePaint.strokeWidth = 11f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                        }
                        mapView.overlays.add(halo)
                        mapView.overlays.add(main)
                    }
                }

                if (liveDot != null) {
                    val marker = Marker(mapView).apply {
                        position = liveDot
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = if (simulatedPosition != null) {
                            "You are here (estimated)"
                        } else {
                            forcedSpace?.display_name ?: "You are here"
                        }
                    }
                    mapView.overlays.add(marker)
                }

                if (zoomBelowThreshold) {
                    uiState.visibleBuildings.forEach { b ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(b.origin_lat, b.origin_lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = b.name
                            snippet = b.address ?: b.campus_name
                            setOnMarkerClickListener { _, _ ->
                                viewModel.enterBuilding(b.id)
                                mapView.controller.animateTo(GeoPoint(b.origin_lat, b.origin_lng))
                                mapView.controller.setZoom(INDOOR_ZOOM_THRESHOLD + 0.5)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                if (uiState.floorId != null && uiState.floorId != fittedFloorId) {
                    val bbox = floorBoundingBox(uiState.spaces)
                    if (bbox != null) {
                        mapView.post { mapView.zoomToBoundingBox(bbox, true, 120) }
                        fittedFloorId = uiState.floorId
                    }
                }

                mapView.invalidate()
            }
        )

        DisposableEffect(Unit) {
            onDispose { mapViewRef?.onPause() }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
            )
        }

        SearchOverlay(
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            suggestions = suggestions,
            onSuggestionClick = { space ->
                searchText = ""
                viewModel.selectSpace(space.id)
                flyTo(space.centroid_lat, space.centroid_lng)
            },
            onNavigateClick = { viewModel.computeRouteToSelected() }
        )

        if (uiState.forcedUserSpaceId != null) {
            val pinnedName = uiState.spaces
                .firstOrNull { it.id == uiState.forcedUserSpaceId }
                ?.let { it.display_name ?: it.short_name ?: it.id }
                ?: "registered landmark"
            PinnedLocationBanner(
                roomName = pinnedName,
                methodLabel = fix.mode.label,
                onClear = { viewModel.clearForcedLocation() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 116.dp, start = 16.dp, end = 84.dp)
            )
        } else {
            LocationPill(
                label = fix.mode.label,
                accuracyMeters = fix.accuracyMeters,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 126.dp, start = 16.dp)
            )
        }

        RightControls(
            floorName = uiState.floorName ?: floorName,
            floors = uiState.availableFloors,
            currentFloorId = uiState.floorId,
            onSelectFloor = { fId -> viewModel.loadFloor(fId, userInitiated = true) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 118.dp, end = 16.dp),
            onRefresh = {
                (floorId ?: uiState.floorId)?.let { viewModel.loadFloor(it) }
            },
            onZoomIn = { mapViewRef?.controller?.zoomIn() },
            onZoomOut = { mapViewRef?.controller?.zoomOut() }
        )

        IconButton(
            onClick = {
                val mv = mapViewRef ?: return@IconButton
                val bbox = floorBoundingBox(uiState.spaces)
                if (bbox != null) {
                    mv.zoomToBoundingBox(bbox, true, 120)
                } else {
                    val me = locationOverlayRef?.myLocation
                    mv.controller.animateTo(me ?: DEFAULT_CENTER)
                    mv.controller.setZoom(16.0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = bottomControlsPadding)
                .size(74.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF))
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        PositioningControls(
            floorId = uiState.floorId ?: floorId ?: "",
            spaces = uiState.spaces,
            buildingId = uiState.currentBuildingId,
            floors = uiState.availableFloors,
            canCalibrate = canCalibrate,
            onLocated = { spaceId, fId ->
                viewModel.forceUserSpace(spaceId, fId, source = "wifi")
                val locatedSpace = uiState.spaces.firstOrNull { it.id == spaceId }
                flyTo(locatedSpace?.centroid_lat, locatedSpace?.centroid_lng)
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = bottomControlsPadding)
        )

        if (routeCardVisible) {
            BottomRouteCard(
                selectedSpace = selectedSpace,
                routeSteps = uiState.routeSteps,
                hasRoute = uiState.routeSteps.isNotEmpty(),
                isNavigating = uiState.isNavigating,
                stepsLeft = stepsLeft,
                onNavigate = { viewModel.computeRouteToSelected() },
                onStart = { viewModel.startNavigation() },
                onStop = { viewModel.stopNavigation() },
                onClose = { viewModel.clearSelection() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 34.dp)
            )
        }

        uiState.error?.let {
            ErrorPill(
                text = it,
                modifier = Modifier.align(Alignment.Center).padding(20.dp)
            )
        }
    }
}

private fun splitRoute(route: List<GeoPoint>, anchor: GeoPoint): Pair<List<GeoPoint>, List<GeoPoint>> {
    if (route.size < 2) return emptyList<GeoPoint>() to route

    var bestSeg = 0
    var bestProj = route.first()
    var bestDist = Double.MAX_VALUE
    for (i in 0 until route.size - 1) {
        val (proj, dist) = projectOntoSegment(anchor, route[i], route[i + 1])
        if (dist < bestDist) {
            bestDist = dist
            bestSeg = i
            bestProj = proj
        }
    }

    val walked = route.subList(0, bestSeg + 1) + bestProj
    val remaining = listOf(bestProj) + route.subList(bestSeg + 1, route.size)
    return walked to remaining
}

private fun routeDeviation(route: List<GeoPoint>, anchor: GeoPoint): Double {
    if (route.size < 2) return 0.0
    var best = Double.MAX_VALUE
    for (i in 0 until route.size - 1) {
        val (_, d) = projectOntoSegment(anchor, route[i], route[i + 1])
        if (d < best) best = d
    }
    return best
}

private fun remainingSteps(steps: List<RouteStepDto>, anchor: GeoPoint): Int {
    val located = steps.withIndex().filter { it.value.centroid_lat != null && it.value.centroid_lng != null }
    if (located.isEmpty()) return steps.size
    val nearest = located.minByOrNull {
        val (_, d) = projectOntoSegment(
            anchor,
            GeoPoint(it.value.centroid_lat!!, it.value.centroid_lng!!),
            GeoPoint(it.value.centroid_lat!!, it.value.centroid_lng!!)
        )
        d
    } ?: return steps.size
    return (steps.size - nearest.index).coerceAtLeast(1)
}

private fun projectOntoSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Pair<GeoPoint, Double> {
    val mPerDegLat = 111_320.0
    val mPerDegLng = 111_320.0 * Math.cos(Math.toRadians(a.latitude))
    val ax = a.longitude * mPerDegLng; val ay = a.latitude * mPerDegLat
    val bx = b.longitude * mPerDegLng; val by = b.latitude * mPerDegLat
    val px = p.longitude * mPerDegLng; val py = p.latitude * mPerDegLat
    val dx = bx - ax; val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
    val projLat = a.latitude + t * (b.latitude - a.latitude)
    val projLng = a.longitude + t * (b.longitude - a.longitude)
    val ex = px - (ax + t * dx); val ey = py - (ay + t * dy)
    return GeoPoint(projLat, projLng) to Math.sqrt(ex * ex + ey * ey)
}

private fun projectArcLengthMeters(route: List<GeoPoint>, p: GeoPoint): Double {
    if (route.size < 2) return 0.0
    var bestDist = Double.MAX_VALUE
    var bestArc = 0.0
    var acc = 0.0
    for (i in 0 until route.size - 1) {
        val segLen = haversineMeters(route[i], route[i + 1])
        val (proj, dist) = projectOntoSegment(p, route[i], route[i + 1])
        if (dist < bestDist) {
            bestDist = dist
            bestArc = acc + haversineMeters(route[i], proj)
        }
        acc += segLen
    }
    return bestArc
}

private fun coordAtArcLengthMeters(route: List<GeoPoint>, target: Double): GeoPoint? {
    if (route.isEmpty()) return null
    if (target <= 0) return route.first()
    var acc = 0.0
    for (i in 0 until route.size - 1) {
        val segLen = haversineMeters(route[i], route[i + 1])
        if (acc + segLen >= target) {
            val t = ((target - acc) / segLen.coerceAtLeast(1e-4)).coerceIn(0.0, 1.0)
            val lat = route[i].latitude + t * (route[i + 1].latitude - route[i].latitude)
            val lng = route[i].longitude + t * (route[i + 1].longitude - route[i].longitude)
            return GeoPoint(lat, lng)
        }
        acc += segLen
    }
    return route.last()
}

private fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
    val la1 = Math.toRadians(a.latitude)
    val la2 = Math.toRadians(b.latitude)
    val dlat = Math.toRadians(b.latitude - a.latitude)
    val dlng = Math.toRadians(b.longitude - a.longitude)
    val h = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
            Math.cos(la1) * Math.cos(la2) *
            Math.sin(dlng / 2) * Math.sin(dlng / 2)
    return 2 * 6_371_000.0 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
}

private fun maybeAutoSnapToBuilding(
    center: GeoPoint,
    zoom: Double,
    buildings: List<com.example.aauapp.data.remote.VisibleBuildingDto>,
    currentBuildingId: String?,
    onSnap: (String) -> Unit
) {
    if (zoom < INDOOR_ZOOM_THRESHOLD || buildings.isEmpty()) return
    val nearest = buildings
        .map { b ->
            val (_, d) = projectOntoSegment(
                center,
                GeoPoint(b.origin_lat, b.origin_lng),
                GeoPoint(b.origin_lat, b.origin_lng)
            )
            b to d
        }
        .minByOrNull { it.second } ?: return
    if (nearest.second <= SNAP_RADIUS_METERS && nearest.first.id != currentBuildingId) {
        onSnap(nearest.first.id)
    }
}

private fun floorBoundingBox(spaces: List<SpaceDisplayDto>): BoundingBox? {
    val pts = spaces.flatMap { it.polygon_global.orEmpty() }
        .filter { it.size >= 2 }
        .map { GeoPoint(it[0], it[1]) }
    if (pts.isEmpty()) return null
    return BoundingBox.fromGeoPoints(pts)
}

@Composable
private fun PinnedLocationBanner(
    roomName: String,
    methodLabel: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = Color(0xFFFFEB3B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pinned to: $roomName",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = "Tap × to return to $methodLabel",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onClear, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Return to live location",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchOverlay(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    suggestions: List<SpaceDisplayDto>,
    onSuggestionClick: (SpaceDisplayDto) -> Unit,
    onNavigateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 38.dp, start = 16.dp, end = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().height(66.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )

                TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    placeholder = {
                        Text(
                            "Search destinations...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = Blue600
                    )
                )

                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    suggestions.forEach { space ->
                        TextButton(
                            onClick = { onSuggestionClick(space) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = space.display_name ?: space.short_name ?: space.id,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = space.space_type ?: "Space",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPill(
    label: String,
    accuracyMeters: Float?,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {},
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        val text = accuracyMeters?.let { "$label · ±${it.toInt()}m" } ?: label
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun RightControls(
    floorName: String,
    floors: List<FloorMapDto>,
    currentFloorId: String?,
    onSelectFloor: (String) -> Unit,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        WhiteFloatingIcon(icon = Icons.Default.Refresh, onClick = onRefresh)

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
        ) {
            Column(modifier = Modifier.width(52.dp)) {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                IconButton(onClick = onZoomOut, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FloorSwitcher(
            floorName = floorName,
            floors = floors,
            currentFloorId = currentFloorId,
            onSelectFloor = onSelectFloor
        )
    }
}

@Composable
private fun FloorSwitcher(
    floorName: String,
    floors: List<FloorMapDto>,
    currentFloorId: String?,
    onSelectFloor: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val switchable = floors.size > 1

    Box {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = { if (switchable) expanded = true },
            modifier = Modifier.size(88.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = floorName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                    if (switchable) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            floors.forEach { floor ->
                val selected = floor.id == currentFloorId
                DropdownMenuItem(
                    text = {
                        Text(
                            text = floor.display_name ?: floorLabel(floor.floor_index),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (selected) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        expanded = false
                        if (!selected) onSelectFloor(floor.id)
                    }
                )
            }
        }
    }
}

private fun floorLabel(index: Int?): String = when {
    index == null -> "Floor"
    index == 0 -> "Ground"
    index < 0 -> "Basement ${-index}"
    else -> "Floor $index"
}

@Composable
private fun WhiteFloatingIcon(icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(58.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BottomRouteCard(
    selectedSpace: SpaceDisplayDto?,
    routeSteps: List<RouteStepDto>,
    hasRoute: Boolean,
    isNavigating: Boolean,
    stepsLeft: Int,
    onNavigate: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val destinationTitle = selectedSpace?.display_name
        ?: selectedSpace?.short_name
        ?: "Destination"

    val instruction = routeSteps.firstOrNull()?.instruction
        ?: if (selectedSpace == null) "Search or select a destination"
        else "Route to $destinationTitle"

    val actionIcon = when {
        !hasRoute -> Icons.Default.Navigation
        isNavigating -> Icons.Default.Stop
        else -> Icons.Default.PlayArrow
    }
    val actionDesc = when {
        !hasRoute -> "Compute route"
        isNavigating -> "Stop navigation"
        else -> "Start navigation"
    }
    val onAction: () -> Unit = when {
        !hasRoute -> onNavigate
        isNavigating -> onStop
        else -> onStart
    }

    Card(
        modifier = modifier.fillMaxWidth().height(132.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0057D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasRoute) {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        } else {
                            Icons.Default.Navigation
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 28.dp)) {
                    Text(
                        text = if (isNavigating) "NOW" else "READY",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = instruction,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2
                    )
                    Text(
                        text = "${stepsLeft.coerceAtLeast(0)} steps left • $destinationTitle",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onAction,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(actionIcon, contentDescription = actionDesc, tint = Color.White)
                }
            }

            // Dismiss the card (clears the selection/route).
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorPill(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
        )
    ) {
        Text(text = text, color = Color.White, modifier = Modifier.padding(14.dp))
    }
}

private fun floorFillColor(space: SpaceDisplayDto): Color {
    val type = space.space_type.orEmpty().uppercase()
    return when {
        "CORRIDOR" in type || "PASSAGE" in type || "LOBBY" in type ->
            Color(0xEE009DFF)

        "RESTROOM" in type || "WC" in type || "TOILET" in type || "BATHROOM" in type ->
            Color(0xFFFF73B3)

        "STAIR" in type || "ELEVATOR" in type || "ESCALATOR" in type ||
            "RAMP" in type || "CONNECTOR" in type || "BRIDGE" in type ||
            "TUNNEL" in type || "WALKWAY" in type ->
            Color(0xFFFF9519)

        else ->
            Color(0xCCB6DFFF)
    }
}
