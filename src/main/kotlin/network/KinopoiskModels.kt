package network.models

import kotlinx.serialization.Serializable

@Serializable
data class FilmSearchResponse(val docs: List<KinopoiskMovie>)

@Serializable
data class KinopoiskMovie(
    val name: String,
    val year: Int,
    val description: String,
    val genres: List<Genre>
)

@Serializable
data class Genre(val name: String)
