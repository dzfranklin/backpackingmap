package com.backpackingmap.backpackingmap.ui.screen.map_screen

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.model.Coord
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.toFeature
import com.backpackingmap.backpackingmap.ui.view.CoordDialog
import com.backpackingmap.backpackingmap.ui.view.CoordView
import com.backpackingmap.backpackingmap.ui.view.MapboxState
import com.backpackingmap.backpackingmap.ui.view.TouchArea
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// TODO: Replace with a crosshairs where you pan the map under it

@Composable
fun MapCreateMarker(repo: Repo, mapbox: MapboxState) {
    val mode = repo.coordinateMode().collectAsState(initial = CoordinateMode.LatLng)
    val point = remember(mapbox) { mutableStateOf(Coord.ZERO) }

    val scope = rememberCoroutineScope()
    val markerSource = remember(mapbox) { GeoJsonSource(MARKER_SOURCE) }
    val markerBitmap = rememberMarkerBitmap()

    val typeCoordOpen = remember(mapbox) { mutableStateOf(false) }

    DisposableEffect(mapbox) {
        val effect = scope.launch {
            val map = mapbox.awaitMap()
            val style = mapbox.awaitStyle()

            val initialPoint = map.cameraPosition.target?.let { Coord(it) } ?: Coord.ZERO
            point.value = initialPoint

            style.apply {
                // NOTE: Cleaned up in onDispose
                addImage(MARKER_IMAGE, markerBitmap)
                addSource(markerSource)
                addLayer(
                    SymbolLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
                        PropertyFactory.iconImage(MARKER_IMAGE),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconOffset(arrayOf(-0.5f, -0.5f))
                    )
                )
            }

            val touch = TouchArea(
                id = MARKER_AREA_ID,
                center = initialPoint.toMapbox(),
                onDrag = { _, mapPoint ->
                    point.value = Coord(mapPoint)
                    true
                }
            )
            // NOTE: Cleaned up in onDispose
            mapbox.registerTouchArea(touch)

            snapshotFlow { point.value }
                .filterNotNull()
                .collect {
                    val mb = it.toMapbox()
                    markerSource.setGeoJson(mb.toFeature())
                    touch.center = mb
                }
        }

        onDispose {
            scope.launch {
                val style = mapbox.awaitStyle()
                style.apply {
                    removeImage(MARKER_IMAGE)
                    removeSource(markerSource)
                    removeLayer(MARKER_LAYER)
                }
                mapbox.deregisterTouchArea(MARKER_AREA_ID)
                effect.cancel()
            }
        }
    }

    CoordDialog(
        isOpen = typeCoordOpen.value,
        initialCoord = point.value,
        closeWith = {
            point.value = it
            typeCoordOpen.value = false
        },
        mode = mode.value,
        setMode = { scope.launch { repo.setCoordinateMode(it) } }
    )

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (headerRef, gestureRef) = createRefs()

        Row(
            Modifier
                .constrainAs(headerRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.wrapContent
                    width = Dimension.fillToConstraints
                }
                .background(MaterialTheme.colors.surface.copy(alpha = 0.7f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { TODO() }) {
                Text(stringResource(R.string.cancel))
            }

            val currentPoint = point.value
            val clip = RoundedCornerShape(3.dp)
            CoordView(
                currentPoint,
                mode.value,
                Modifier
                    .clip(clip)
                    .clickable(onClickLabel = stringResource(R.string.edit_coordinate)) {
                        typeCoordOpen.value = true
                    }
                    .alpha(0.7f)
                    .background(MaterialTheme.colors.surface)
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), clip)
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            )

            Button(onClick = { /*TODO*/ }) {
                Text(stringResource(R.string.create))
            }
        }

        Box(Modifier.constrainAs(gestureRef) {
            linkTo(
                top = headerRef.top,
                bottom = parent.bottom,
                start = parent.start,
                end = parent.end
            )
        })
    }
}

@Composable
fun rememberMarkerBitmap(tint: Color = Color.DarkGray): Bitmap {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val painter = painterResource(R.drawable.marker)

    return remember(painter, context, density, layoutDirection, tint) {
        val size = painter.intrinsicSize
        val image = ImageBitmap(size.width.roundToInt(), size.height.roundToInt())
        val canvas = Canvas(image)
        CanvasDrawScope().draw(density, layoutDirection, canvas, size) {
            with(painter) {
                draw(size, colorFilter = ColorFilter.tint(tint))
            }
        }
        image.asAndroidBitmap()
    }
}

private const val MARKER_AREA_ID = "create_marker"
private const val MARKER_LAYER = "create_marker_layer"
private const val MARKER_SOURCE = "create_marker_source"
private const val MARKER_IMAGE = "create_marker_image"