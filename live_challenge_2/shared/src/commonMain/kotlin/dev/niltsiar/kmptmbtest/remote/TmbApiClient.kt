package dev.niltsiar.kmptmbtest.remote

import dev.niltsiar.kmptmbtest.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.pathComponents
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val HOST = "https://api.tmb.cat/v1/transit/"
private const val SUBWAY_LINES_PATH = "linies/metro"

class TmbApiClient(
    private val client: HttpClient
) {

    suspend fun getSubwayLines(): List<Feature> {
        val getSubwayLinesUrl = URLBuilder(HOST).pathComponents(SUBWAY_LINES_PATH).build()
        return try {
            val response = client.get<HttpResponse>(getSubwayLinesUrl) {
                parameter("app_id", BuildKonfig.APP_ID)
                parameter("app_key", BuildKonfig.APP_KEY)
            }

            if (response.status.isSuccess()) {
                response.receive<FeatureCollection>().features
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }
}

@Serializable
data class FeatureCollection(
    val features: List<Feature>
)

@Serializable
data class Feature(
    val properties: Properties
)

@Serializable
data class Properties(
    @SerialName("NOM_LINIA") val lineName: String
)
