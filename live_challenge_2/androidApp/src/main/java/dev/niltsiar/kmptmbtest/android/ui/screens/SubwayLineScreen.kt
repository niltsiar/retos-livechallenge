package dev.niltsiar.kmptmbtest.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.niltsiar.kmptmbtest.android.apiClient
import dev.niltsiar.kmptmbtest.remote.SubwayLineProperties
import dev.niltsiar.kmptmbtest.remote.SubwayStationProperties
import kotlinx.coroutines.launch

@Composable
fun SubwayLinesBody() {
    val scope = rememberCoroutineScope()

    Surface(color = MaterialTheme.colors.background) {
        Column(modifier = Modifier.fillMaxHeight()) {

            val (lines, setLines) = remember { mutableStateOf(emptyList<SubwayLineProperties>()) }

            scope.launch {
                setLines(apiClient.getSubwayLines().map { it.properties })
            }

            SubwayLinesList(lines = lines, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SubwayLinesList(
    modifier: Modifier = Modifier,
    lines: List<SubwayLineProperties>
) {
    val (selectedLine, setSelectedLine) = remember { mutableStateOf(Int.MIN_VALUE) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(items = lines) { line ->
            SubwayLineCell(
                subwayLine = line,
                onClick = setSelectedLine,
                isSelected = selectedLine == line.lineCode
            )
        }
    }
}

@Composable
fun SubwayLineCell(
    subwayLine: SubwayLineProperties,
    onClick: (Int) -> Unit,
    isSelected: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val (stations, setStations) = remember { mutableStateOf<List<SubwayStationProperties>>(emptyList()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.onSurface, RoundedCornerShape(4.dp))
            .clickable { onClick(subwayLine.lineCode) },
    ) {
        if (isSelected) {
            scope.launch {
                setStations(apiClient.getStationFromSubwayLine(subwayLine.lineCode).map { it.properties })
            }
        } else {
            setStations(emptyList())
        }
        Column {
            Text(text = subwayLine.lineName, modifier = Modifier.padding(16.dp))
            if (stations.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    stations.sortedBy { it.stationOrder }
                        .forEach {
                            Surface(
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colors.secondary
                                )
                            ) {
                                Text(
                                    text = it.stationName,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SubwayLineCellPreview() {
    SubwayLineCell(
        subwayLine = SubwayLineProperties(1, "L4"),
        onClick = {},
        isSelected = true
    )
}
