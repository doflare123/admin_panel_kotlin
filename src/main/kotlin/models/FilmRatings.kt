import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import models.Films
import models.FilmStatus
import models.Users
import models.UserExpectations

fun getFilmsOnReview(): List<Film> = transaction {
    Films.select { Films.status eq FilmStatus.ON_REVIEW }
        .map {
            Film(
                id = it[Films.id].value,
                title = it[Films.title],
                description = it[Films.description],
                year = it[Films.year],
                status = it[Films.status]
            )
        }
}

fun getAllUsers(): List<User> = transaction {
    Users.selectAll().map {
        User(it[Users.id].value, it[Users.name])
    }
}

fun getUserExpectation(filmId: Int, userId: Int): Double? = transaction {
    UserExpectations.select {
        (UserExpectations.film eq filmId) and (UserExpectations.user eq userId)
    }.firstOrNull()?.get(UserExpectations.rating)
}

fun saveUserExpectationAndUpdateStatus(filmId: Int, userId: Int, value: String): Boolean = transaction {
    val rating = when {
        value == "-" -> -1.0
        value.toDoubleOrNull()?.let { it in 0.0..10.0 } == true -> value.toDouble()
        else -> return@transaction false
    }

    // Upsert
    val existing = UserExpectations.select {
        (UserExpectations.film eq filmId) and (UserExpectations.user eq userId)
    }.firstOrNull()

    if (existing != null) {
        UserExpectations.update({
            (UserExpectations.film eq filmId) and (UserExpectations.user eq userId)
        }) {
            it[UserExpectations.rating] = rating
        }
    } else {
        UserExpectations.insert {
            it[UserExpectations.film] = filmId
            it[UserExpectations.user] = userId
            it[UserExpectations.rating] = rating
        }
    }

    // Пересчёт средней оценки
    val ratings = UserExpectations
        .select { UserExpectations.film eq filmId }
        .map { it[UserExpectations.rating] }
        .filter { it >= 0 } // исключаем '-'

    val avg = if (ratings.isNotEmpty()) ratings.average() else 0.0

    if (avg >= 5.0) {
        Films.update({ Films.id eq filmId }) {
            it[status] = FilmStatus.WAITING_WATCH
        }
    }

    true
}


data class Film(
    val id: Int,
    val title: String,
    val description: String,
    val year: Int,
    var status: FilmStatus
)

data class User(
    val id: Int,
    val name: String
)
