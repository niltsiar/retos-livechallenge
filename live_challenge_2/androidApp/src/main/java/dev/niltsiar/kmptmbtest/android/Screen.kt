package dev.niltsiar.kmptmbtest.android

sealed class Screen(val route: String, val name: String) {
    object SubwayLinesScreen : Screen("subwayLines", "Metro Lines")
    object SubwayPathsScreen : Screen("subwayPaths", "Metro map")
    object BusStopsScreen : Screen("busStops", "Bus Stops")
}
