package dev.niltsiar.kmptmbtest.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import dev.niltsiar.kmptmbtest.android.apiClient
import dev.niltsiar.kmptmbtest.android.ui.rememberMapViewWithLifecycle
import dev.niltsiar.kmptmbtest.android.ui.setZoom
import dev.niltsiar.kmptmbtest.remote.BusStopProperties
import dev.niltsiar.kmptmbtest.remote.BusStopTimesContent
import dev.niltsiar.kmptmbtest.remote.Feature
import io.github.dellisd.spatialk.geojson.Point
import kotlinx.coroutines.launch

@Composable
fun BusStopMapBody() {
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf(emptyList<Feature<BusStopProperties>>()) }
    var selectedBusStop by remember { mutableStateOf<BusStopInfo?>(null) }

    Surface(color = MaterialTheme.colors.background) {

        scope.launch {
            lines = apiClient.getBusStops()
        }

        val onSelectedLine: (Int) -> Unit = { stopCode ->
            scope.launch {
                val busStopProperties = lines.firstOrNull { it.properties.stopCode == stopCode } ?: return@launch
                val times = apiClient.getTimesFromBusStop(stopCode)
                selectedBusStop = BusStopInfo(busStopProperties, times)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            ExpandedDropDownMenu(
                label = "Bus stops",
                items = lines.sortedBy { it.properties.stopName }
                    .map { it.properties.stopCode to it.properties.stopName },
                onClick = onSelectedLine
            )

            Spacer(Modifier.height(16.dp))

            MapView(InitialLatLng, selectedBusStop)
        }
    }
}

data class BusStopInfo(
    val busStopProperties: Feature<BusStopProperties>,
    val busStopTimes: List<BusStopTimesContent>
)

@Composable
private fun MapView(initialPosition: LatLng, selectedBusStop: BusStopInfo?) {
    // The MapView lifecycle is handled by this composable. As the MapView also needs to be updated
    // with input from Compose UI, those updates are encapsulated into the MapViewContainer
    // composable. In this way, when an update to the MapView happens, this composable won't
    // recompose and the MapView won't need to be recreated.
    val mapView = rememberMapViewWithLifecycle()
    MapViewContainer(mapView, initialPosition, selectedBusStop)
}

@Composable
private fun MapViewContainer(
    map: MapView,
    initialPosition: LatLng,
    selectedBusStop: BusStopInfo?
) {
    var mapInitialized by remember(map) { mutableStateOf(false) }
    LaunchedEffect(map, mapInitialized) {
        if (!mapInitialized) {
            val googleMap = map.awaitMap()
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(initialPosition))
            mapInitialized = true
        }
    }

    var zoom by rememberSaveable(map) { mutableStateOf(InitialZoom) }

    var currentMarker: Marker? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()
    AndroidView({ map }) { mapView ->

        // Reading zoom so that AndroidView recomposes when it changes. The getMapAsync lambda
        // is stored for later, Compose doesn't recognize state reads
        val mapZoom = zoom
        coroutineScope.launch {
            val googleMap = mapView.awaitMap()
            googleMap.setZoom(mapZoom)
            // Move camera to the same place to trigger the zoom update
            selectedBusStop?.let { busStopInfo ->
                currentMarker?.remove()
                val point = busStopInfo.busStopProperties.geometry as? Point ?: return@let
                val latLng = LatLng(point.coordinates.latitude, point.coordinates.longitude)
                val title =
                    "${busStopInfo.busStopProperties.properties.stopCode} - ${busStopInfo.busStopProperties.properties.stopName}\n"
                val times = busStopInfo.busStopTimes.map { "Line ${it.line} / Wait for ${it.remainingTime} minutes" }
                    .ifEmpty { listOf("No information about waiting times") }
                currentMarker = googleMap.addMarker {
                    position(latLng)
                    title(title)
                    snippet(times.joinToString("\n"))
                }
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLng(latLng)
                )
            }
        }
    }
}

private const val InitialZoom = 12f
private val InitialLatLng = LatLng(41.6523, -4.7245)

@Preview(showBackground = true)
@Composable
fun BusStopMapBodyPreview() {
    BusStopMapBody()
}

