package models

import org.jetbrains.exposed.dao.id.IntIdTable

object Films : IntIdTable() {
    val title = varchar("title", 255)
    val description = text("description")
    val genres = varchar("genres", 255)
    val status = enumerationByName("status", 50, FilmStatus::class)
}

enum class FilmStatus {
    ON_REVIEW,          // Ожидает оценку ожидания
    WAITING_WATCH,      // Ожидает просмотра
    WATCHED_WAIT_RATE,  // Просмотрен, ждёт оценку
    COMPLETED           // Оценён после просмотра
}
