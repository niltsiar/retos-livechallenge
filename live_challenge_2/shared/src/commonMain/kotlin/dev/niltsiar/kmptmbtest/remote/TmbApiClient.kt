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
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.pathComponents
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val HOST = "https://api.tmb.cat/v1/"

class TmbApiClient(
    private val client: HttpClient
) {

    suspend fun getSubwayLines(): List<Feature<SubwayLineProperties>> {
        val SUBWAY_LINES_PATH = "transit/linies/metro"
        val getSubwayLinesUrl = URLBuilder(HOST).pathComponents(SUBWAY_LINES_PATH).build()

        return getFeaturesFromWs(getSubwayLinesUrl)
    }

    suspend fun getStationFromSubwayLine(lineCode: Int): List<Feature<SubwayStationProperties>> {
        val SUBWAY_STATIONS_PATH = "transit/linies/metro/{codi_linia}/estacions"
            .replace("{codi_linia}", "$lineCode")
        val getStationsFromSubwayLineUrl = URLBuilder(HOST).pathComponents(SUBWAY_STATIONS_PATH).build()

        return getFeaturesFromWs(getStationsFromSubwayLineUrl)
    }

    suspend fun getBusStops(): List<Feature<BusStopProperties>> {
        val BUS_STOP_PATH = "transit/parades"
        val getBusStopsUrl = URLBuilder(HOST).pathComponents(BUS_STOP_PATH).build()

        return getFeaturesFromWs(getBusStopsUrl)
    }

    suspend fun getTimesFromBusStop(stopCode: Int): List<BusStopTimesContent> {
        val BUS_STOP_TIMES_PATH = "ibus/stops/{codi_parada}"
            .replace("{codi_parada}", "$stopCode")
        val getTimesFromBusStopUrl = URLBuilder(HOST).pathComponents(BUS_STOP_TIMES_PATH).build()

        return try {
            val response = client.get<HttpResponse>(getTimesFromBusStopUrl, HttpRequestBuilder::setAuthKeys)

            if (response.status.isSuccess()) {
                response.receive<BusStopTimesResponse>().data.content
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private suspend inline fun <reified T> getFeaturesFromWs(url: Url): List<Feature<T>> {
        return try {
            val response = client.get<HttpResponse>(url, HttpRequestBuilder::setAuthKeys)

            if (response.status.isSuccess()) {
                response.receive<FeatureCollection<T>>().features
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

@Serializable
data class BusStopProperties(
    @SerialName("CODI_PARADA") val stopCode: Int,
    @SerialName("NOM_PARADA") val stopName: String,
)

@Serializable
data class BusStopTimesResponse(
    val status: String,
    val data: BusStopTimesData,
)

@Serializable
data class BusStopTimesData(
    @SerialName("ibus") val content: List<BusStopTimesContent>,
)

@Serializable
data class BusStopTimesContent(
    val line: String,
    @SerialName("t-in-min") val remainingTime: Int,
)
