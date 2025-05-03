package models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object Films : IntIdTable() {
    val title = varchar("title", 255)
    val description = text("description")
    val year = integer("year")
    val status = enumerationByName("status", 50, FilmStatus::class)
}

object Genres : IntIdTable() {
    val name = varchar("name", 100).uniqueIndex()
}

object FilmGenres : Table() {
    val film = reference("film_id", Films)
    val genre = reference("genre_id", Genres)
    override val primaryKey = PrimaryKey(film, genre)
}

enum class FilmStatus {
    ON_REVIEW,          // Ожидает оценку ожидания
    WAITING_WATCH,      // Ожидает просмотра
    WATCHED_WAIT_RATE,  // Просмотрен, ждёт оценку
    COMPLETED           // Оценён после просмотра
}
