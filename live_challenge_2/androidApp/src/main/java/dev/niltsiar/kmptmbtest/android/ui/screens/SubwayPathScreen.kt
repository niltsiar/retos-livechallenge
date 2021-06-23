package dev.niltsiar.kmptmbtest.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.ktx.awaitMap
import dev.niltsiar.kmptmbtest.android.apiClient
import dev.niltsiar.kmptmbtest.android.ui.rememberMapViewWithLifecycle
import dev.niltsiar.kmptmbtest.android.ui.setZoom
import dev.niltsiar.kmptmbtest.remote.Feature
import dev.niltsiar.kmptmbtest.remote.SubwayLineProperties
import io.github.dellisd.spatialk.geojson.Geometry
import io.github.dellisd.spatialk.geojson.GeometryCollection
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.geojson.MultiLineString
import io.github.dellisd.spatialk.geojson.MultiPoint
import io.github.dellisd.spatialk.geojson.MultiPolygon
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SubwayMapBody() {
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf(emptyList<Feature<SubwayLineProperties>>()) }
    var selectedLine by remember { mutableStateOf<Feature<SubwayLineProperties>?>(null) }

    Surface(color = MaterialTheme.colors.background) {

        scope.launch {
            lines = apiClient.getSubwayLines()
        }

        val onSelectedLine: (Int) -> Unit = { lineId ->
            selectedLine = lines.firstOrNull { it.properties.lineCode == lineId }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            ExpandedDropDownMenu(
                label = "Subway Lines",
                items = lines.map { it.properties.lineCode to it.properties.lineName },
                onClick = onSelectedLine
            )

            Spacer(Modifier.height(16.dp))

            MapView(InitialLatLng, selectedLine)
        }
    }
}

@Composable
fun ExpandedDropDownMenu(
    modifier: Modifier = Modifier,
    label: String,
    items: List<Pair<Int, String>>,
    onClick: ((Int) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }

    val icon = if (expanded) {
        Icons.Filled.ExpandLess
    } else {
        Icons.Filled.ExpandMore
    }

    Column() {
        OutlinedTextField(
            value = selectedText,
            onValueChange = { selectedText = it },
            modifier = modifier.fillMaxWidth(),
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(icon, contentDescription = null, modifier.clickable { expanded = !expanded })
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(onClick = {
                    onClick?.invoke(item.first)
                    selectedText = item.second
                    expanded = false
                }) {
                    Text(text = item.second)
                }
            }
        }
    }
}

@Composable
private fun MapView(initialPosition: LatLng, selectedLine: Feature<SubwayLineProperties>?) {
    // The MapView lifecycle is handled by this composable. As the MapView also needs to be updated
    // with input from Compose UI, those updates are encapsulated into the MapViewContainer
    // composable. In this way, when an update to the MapView happens, this composable won't
    // recompose and the MapView won't need to be recreated.
    val mapView = rememberMapViewWithLifecycle()
    MapViewContainer(mapView, initialPosition, selectedLine)
}

@Composable
private fun MapViewContainer(
    map: MapView,
    initialPosition: LatLng,
    selectedLine: Feature<SubwayLineProperties>?
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

    var currentLineLayer: GeoJsonLayer? by remember { mutableStateOf(null) }

    val padding = with(LocalDensity.current) { 16.dp.roundToPx() }

    val coroutineScope = rememberCoroutineScope()
    AndroidView({ map }) { mapView ->
        // Reading zoom so that AndroidView recomposes when it changes. The getMapAsync lambda
        // is stored for later, Compose doesn't recognize state reads
        val mapZoom = zoom
        coroutineScope.launch {
            val googleMap = mapView.awaitMap()
            googleMap.setZoom(mapZoom)
            // Move camera to the same place to trigger the zoom update

            selectedLine?.let {
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        selectedLine.geometry.calculateBoundingBox(), padding
                    )
                )
                currentLineLayer?.removeLayerFromMap()
                val data = selectedLine.geometry.json
                val json = JSONObject(data)
                currentLineLayer = GeoJsonLayer(googleMap, json).also { layer ->
                    layer.addLayerToMap()
                }
            }
        }
    }
}

fun Geometry.getCoordinates(): List<Position> {
    return when (this) {
        is GeometryCollection -> geometries.map { it.getCoordinates() }.flatten()
        is LineString -> coordinates
        is MultiLineString -> coordinates.flatten()
        is MultiPoint -> coordinates
        is MultiPolygon -> coordinates.flatten().flatten()
        is Point -> listOf(coordinates)
        is Polygon -> coordinates.flatten()
    }
}

fun Geometry.calculateBoundingBox(): LatLngBounds {
    val builder = LatLngBounds.builder()
    getCoordinates().map { LatLng(it.latitude, it.longitude) }
        .forEach { builder.include(it) }
    return builder.build()
}

private const val InitialZoom = 12f
private val InitialLatLng = LatLng(41.3851, 2.1734)

@Preview(showBackground = true)
@Composable
fun ExpandedDropDownMenuPreview() {
    ExpandedDropDownMenu(
        label = "Lines",
        items = listOf(
            1 to "Line 1",
            2 to "Line 2"
        )
    )
}

@Preview(showBackground = true)
@Composable
fun SubwayMapBodyPreview() {
    SubwayMapBody()
}
