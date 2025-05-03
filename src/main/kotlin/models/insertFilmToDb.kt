package models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import models.Films
import org.jetbrains.exposed.sql.Database

fun connectToDb() {
    Database.connect("jdbc:sqlite:films.db", driver = "org.sqlite.JDBC")
}

suspend fun insertFilmToDb(title: String, description: String, genresStr: String, year: Int) {
    withContext(Dispatchers.IO) {
        transaction {
            // 1. Сохраняем фильм
            val filmId = Films.insertAndGetId {
                it[Films.title] = title
                it[Films.description] = description
                it[Films.status] = FilmStatus.ON_REVIEW
                it[Films.year] = year
            }

            // 2. Разбиваем жанры и вставляем в таблицу Genres (если нет)
            val genreNames = genresStr.split(",").map { it.trim().replaceFirstChar { c -> c.uppercaseChar() } }.filter { it.isNotEmpty() }

            for (genreName in genreNames) {
                val genreId = Genres.select { Genres.name eq genreName }
                    .map { it[Genres.id] }
                    .firstOrNull() ?: Genres.insertAndGetId {
                        it[name] = genreName
                    }

                // 3. Связываем фильм и жанр
                FilmGenres.insertIgnore {
                    it[film] = filmId
                    it[genre] = genreId
                }
            }
        }
    }
}

