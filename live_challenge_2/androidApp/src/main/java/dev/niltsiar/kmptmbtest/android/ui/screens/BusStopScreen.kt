package dev.niltsiar.kmptmbtest.android.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import dev.niltsiar.kmptmbtest.android.R
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
    var busStops by remember { mutableStateOf(emptyList<Feature<BusStopProperties>>()) }
    var selectedBusStop by remember { mutableStateOf<BusStopInfo?>(null) }

    val onSelectedLine: (Int) -> Unit = { stopCode ->
        scope.launch {
            val busStopProperties = busStops.firstOrNull { it.properties.stopCode == stopCode } ?: return@launch
            val times = apiClient.getTimesFromBusStop(stopCode)
            selectedBusStop = BusStopInfo(busStopProperties, times)
        }
    }

    LaunchedEffect(true) {
        busStops = apiClient.getBusStops()
    }

    Surface(color = MaterialTheme.colors.background) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            ExpandedDropDownMenu(
                label = "Bus stops",
                items = busStops.sortedBy { it.properties.stopName }
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

@SuppressLint("PotentialBehaviorOverride")
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

    val layoutInflater = LayoutInflater.from(LocalContext.current)

    AndroidView({ map }) { mapView ->
        // Reading zoom so that AndroidView recomposes when it changes. The getMapAsync lambda
        // is stored for later, Compose doesn't recognize state reads
        val mapZoom = zoom
        coroutineScope.launch {
            val googleMap = mapView.awaitMap()
            googleMap.setZoom(mapZoom)

            googleMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {

                val snippetView = layoutInflater.inflate(R.layout.custom_info_contents, null)

                override fun getInfoWindow(p0: Marker): View? = null

                override fun getInfoContents(marker: Marker): View? {
                    val titleUi: TextView = snippetView.findViewById(R.id.title)
                    val snippetUi: TextView = snippetView.findViewById(R.id.snippet)

                    val title: String? = marker.title
                    if (title != null) {
                        // Spannable string allows us to edit the formatting of the text.
                        titleUi.text = SpannableString(title).apply {
                            setSpan(ForegroundColorSpan(Color.RED), 0, length, 0)
                        }
                    } else {
                        titleUi.text = ""
                    }

                    val snippet: String? = marker.snippet
                    if (snippet != null && snippet.length > 12) {
                        snippetUi.text = SpannableString(snippet).apply {
                            setSpan(ForegroundColorSpan(Color.BLUE), 0, length, 0)
                        }
                    } else {
                        snippetUi.text = ""
                    }

                    return snippetView
                }
            })
            // Move camera to the same place to trigger the zoom update
            selectedBusStop?.let { busStopInfo ->
                val point = busStopInfo.busStopProperties.geometry as? Point ?: return@let
                val latLng = LatLng(point.coordinates.latitude, point.coordinates.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                currentMarker?.remove()
                val title =
                    "Stop ${busStopInfo.busStopProperties.properties.stopCode} - ${busStopInfo.busStopProperties.properties.stopName}\n"
                val times = busStopInfo.busStopTimes.map { "Line ${it.line} / Next bus in ${it.remainingTime} minutes" }
                    .ifEmpty { listOf("No information about waiting times") }
                currentMarker = googleMap.addMarker {
                    position(latLng)
                    title(title)
                    snippet(times.joinToString("\n"))
                }
            }
        }
    }
}

private const val InitialZoom = 12f
private val InitialLatLng = LatLng(41.3851, 2.1734)

@Preview(showBackground = true)
@Composable
fun BusStopMapBodyPreview() {
    BusStopMapBody()
}

