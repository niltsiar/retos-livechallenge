package dev.niltsiar.kmptmbtest.remote

import dev.niltsiar.kmptmbtest.BuildKonfig
import io.github.dellisd.spatialk.geojson.Geometry
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.pathComponents
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val HOST = "https://api.tmb.cat/v1/transit/"

class TmbApiClient(
    private val client: HttpClient
) {

    suspend fun getSubwayLines(): List<Feature<SubwayLineProperties>> {
        val SUBWAY_LINES_PATH = "linies/metro"
        val getSubwayLinesUrl = URLBuilder(HOST).pathComponents(SUBWAY_LINES_PATH).build()
        return try {
            val response = client.get<HttpResponse>(getSubwayLinesUrl, HttpRequestBuilder::setAuthKeys)

            if (response.status.isSuccess()) {
                response.receive<FeatureCollection<SubwayLineProperties>>().features
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    suspend fun getStationFromSubwayLine(lineCode: Int): List<Feature<SubwayStationProperties>> {
        val SUBWAY_STATIONS_PATH = "linies/metro/{codi_linia}/estacions"
            .replace("{codi_linia}", "$lineCode")
        val getStationsFromSubwayLineUrl = URLBuilder(HOST).pathComponents(SUBWAY_STATIONS_PATH).build()

        return try {
            val response = client.get<HttpResponse>(getStationsFromSubwayLineUrl, HttpRequestBuilder::setAuthKeys)

            if (response.status.isSuccess()) {
                response.receive<FeatureCollection<SubwayStationProperties>>().features
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }
}

private fun HttpRequestBuilder.setAuthKeys() {
    parameter("app_id", BuildKonfig.APP_ID)
    parameter("app_key", BuildKonfig.APP_KEY)
}

@Serializable
data class FeatureCollection<T>(
    val features: List<Feature<T>>
)

@Serializable
data class Feature<T>(
    val properties: T,
    val geometry: Geometry
)

interface Properties

@Serializable
data class SubwayLineProperties(
    @SerialName("CODI_LINIA") val lineCode: Int,
    @SerialName("NOM_LINIA") val lineName: String,
) : Properties

@Serializable
data class SubwayStationProperties(
    @SerialName("NOM_ESTACIO") val stationName: String,
    @SerialName("ORDRE_ESTACIO") val stationOrder: Int
) : Properties
