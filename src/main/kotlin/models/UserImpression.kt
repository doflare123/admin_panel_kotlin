package models

import org.jetbrains.exposed.dao.id.IntIdTable

object UserImpressions : IntIdTable() {
    val user = reference("user_id", Users)
    val film = reference("film_id", Films)
    val rating = double("rating")
}
