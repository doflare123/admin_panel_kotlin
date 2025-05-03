package network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import network.models.FilmSearchResponse
import java.net.URLEncoder
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.plugins.*
import io.ktor.client.statement.bodyAsText

suspend fun searchMovie(query: String): FilmSearchResponse? {
    val dotenv = dotenv()
    val apiKey = dotenv["KINOPISK_API_KEY"]
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            header("Accept", "application/json")
            header("X-API-KEY", apiKey)
        }
    }

    return try {
        val response = client.get("https://api.kinopoisk.dev/v1.4/movie/search") {
            parameter("page", 1)
            parameter("limit", 1)
            parameter("query", query)
        }
        
        return response.body<FilmSearchResponse>()        
    } catch (e: Exception) {
        println("Error fetching movie: ${e.message}")
        null
    } finally {
        client.close()
    }
}
