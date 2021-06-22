package dev.niltsiar.kmptmbtest.android

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.niltsiar.kmptmbtest.android.ui.screens.Screen
import dev.niltsiar.kmptmbtest.android.ui.screens.SubwayLinesBody
import dev.niltsiar.kmptmbtest.android.ui.screens.SubwayMapBody
import dev.niltsiar.kmptmbtest.android.ui.theme.AppTheme
import dev.niltsiar.kmptmbtest.remote.TmbApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import kotlinx.serialization.json.Json

private val client = HttpClient(OkHttp) {
    install(JsonFeature) {
        serializer = KotlinxSerializer(Json {
            ignoreUnknownKeys = true
        })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Log.v("KTOR", message)
            }
        }
        level = LogLevel.ALL
    }
}

val apiClient = TmbApiClient(client)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KmpTestApp()
        }
    }
}

@Composable
fun KmpTestApp() {
    AppTheme(darkTheme = false) {
        val navController = rememberNavController()
        val screens = listOf(Screen.SubwayLinesScreen, Screen.SubwayPathsScreen, Screen.BusStopsScreen)

        Scaffold(
            bottomBar = {
                BottomNavigation {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    screens.forEach { screen ->
                        BottomNavigationItem(
                            label = { Text(screen.name) },
                            icon = { Icon(imageVector = screen.icon, contentDescription = null) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            })
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.SubwayLinesScreen.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.SubwayLinesScreen.route) {
                    SubwayLinesBody()
                }
                composable(Screen.SubwayPathsScreen.route) {
                    Surface(color = MaterialTheme.colors.secondary) {
                        SubwayMapBody()
                    }
                }
                composable(Screen.BusStopsScreen.route) {
                    Surface(color = MaterialTheme.colors.primaryVariant) {

                    }
                }
            }
        }
    }
}
