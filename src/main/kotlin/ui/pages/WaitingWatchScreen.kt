package ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import models.*
import kotlin.collections.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class User(val id: Int, val name: String)

data class Film(
    val id: Int,
    val title: String,
    val description: String,
    val year: Int,
    val status: FilmStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingWatchScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var films by remember { mutableStateOf(listOf<Film>()) }
    var users by remember { mutableStateOf(listOf<User>()) }
    val ratings = remember { mutableStateMapOf<Pair<Int, Int>, String>() }
    val expectations = remember { mutableStateMapOf<Int, Double>() }
    val actualAverages = remember { mutableStateMapOf<Int, Double>() }
    val skippedUsers = remember { mutableStateMapOf<Pair<Int, Int>, Boolean>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedFilms = getFilmsByStatus(FilmStatus.WAITING_WATCH)
            val loadedUsers = getAllUsers()
            val loadedRatings = mutableMapOf<Pair<Int, Int>, String>()
            val loadedExpectations = mutableMapOf<Int, Double>()
            val loadedActuals = mutableMapOf<Int, Double>()
            val skipped = mutableMapOf<Pair<Int, Int>, Boolean>()
    
            for (film in loadedFilms) {
                var expectationSum = 0.0
                var expectationCount = 0
                var actualSum = 0.0
                var actualCount = 0
    
                for (user in loadedUsers) {
                    val rating = getUserImpression(film.id, user.id)
                    if (rating != null) {
                        loadedRatings[film.id to user.id] = rating.toString()
                        actualSum += rating
                        actualCount++
                    }
    
                    val expectation = transaction {
                        UserExpectations
                            .select { (UserExpectations.film eq film.id) and (UserExpectations.user eq user.id) }
                            .singleOrNull()?.get(UserExpectations.rating)
                    }
    
                    if (expectation != null) {
                        if (expectation < 0) {
                            skipped[film.id to user.id] = true
                        } else {
                            expectationSum += expectation
                            expectationCount++
                        }
                    }
                }
    
                // Расчёт средних значений
                val avgExpectation = if (expectationCount > 0) expectationSum / expectationCount else 0.0
                val avgActual = if (actualCount > 0) actualSum / actualCount else 0.0
    
                loadedExpectations[film.id] = avgExpectation
                loadedActuals[film.id] = avgActual
            }
    
            films = loadedFilms
            users = loadedUsers
            ratings.putAll(loadedRatings)
            expectations.putAll(loadedExpectations)
            actualAverages.putAll(loadedActuals)
            skippedUsers.putAll(skipped)
        }
    }    

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        Column(Modifier.padding(16.dp).verticalScroll(verticalScrollState)) {
            Text("Фильмы, ожидающие просмотра", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth()) {
                Text("Фильм", Modifier.weight(2f))
            }

            Spacer(Modifier.height(8.dp))

            // Обертка для горизонтальной прокрутки таблицы
            Box(
                modifier = Modifier
                    .horizontalScroll(horizontalScrollState)
                    .fillMaxWidth()
            ) {
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Фильм", Modifier.width(200.dp))

                        users.forEach { user ->
                            Text(user.name, Modifier.width(100.dp), maxLines = 1)
                        }

                        Text("Ожид.", Modifier.width(80.dp))
                        Text("Реальн.", Modifier.width(80.dp))
                        Text("Разн.", Modifier.width(80.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    films.forEach { film ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(film.title, Modifier.width(200.dp))

                            users.forEach { user ->
                                val key = film.id to user.id
                                var input by remember { mutableStateOf(ratings[key] ?: "") }

                                val isSkipped = skippedUsers[key] == true
                                TextField(
                                    value = input,
                                    onValueChange = {
                                        input = it
                                        ratings[key] = it

                                        scope.launch {
                                            val success = withContext(Dispatchers.IO) {
                                                saveUserImpression(film.id, user.id, it)
                                            }

                                            if (!success) {
                                                snackbarHostState.showSnackbar("Ошибка при сохранении оценки")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(100.dp)
                                        .padding(2.dp),
                                    singleLine = true,
                                    enabled = !isSkipped // Запрет ввода, если "-" в ожиданиях
                                )
                            }

                            val expectation = expectations[film.id] ?: 0.0
                            val actual = actualAverages[film.id] ?: 0.0
                            val diff = actual - expectation

                            Text("%.2f".format(expectation), Modifier.width(80.dp))
                            Text("%.2f".format(actual), Modifier.width(80.dp))
                            Text("%.2f".format(diff), Modifier.width(80.dp))
                        }
                    }
                }
            }
            }
        }
    }


// Вспомогательные функции

fun getFilmsByStatus(status: FilmStatus): List<Film> = transaction {
    Films.select { Films.status eq status }.map { rowToFilm(it) }
}

fun getUserImpression(filmId: Int, userId: Int): Double? = transaction {
    UserImpressions.select {
        (UserImpressions.film eq filmId) and (UserImpressions.user eq userId)
    }.singleOrNull()?.get(UserImpressions.rating)
}

fun saveUserImpression(filmId: Int, userId: Int, input: String): Boolean = runCatching {
    val rating = input.toDoubleOrNull() ?: return false
    transaction {
        val exists = UserImpressions.select {
            (UserImpressions.film eq filmId) and (UserImpressions.user eq userId)
        }.count() > 0

        if (exists) {
            UserImpressions.update({
                (UserImpressions.film eq filmId) and (UserImpressions.user eq userId)
            }) {
                it[UserImpressions.rating] = rating
            }
        } else {
            UserImpressions.insert {
                it[UserImpressions.film] = filmId
                it[UserImpressions.user] = userId
                it[UserImpressions.rating] = rating
            }
        }
    }
    true
}.getOrDefault(false)

// Вспомогательная функция для преобразования строки в Film
fun rowToFilm(row: ResultRow): Film {
    return Film(
        id = row[Films.id].value,
        title = row[Films.title],
        description = row[Films.description],
        year = row[Films.year],
        status = row[Films.status]
    )
}

fun getAllUsers(): List<User> = transaction {
    Users.selectAll().map { row ->
        User(
            id = row[Users.id].value,
            name = row[Users.name]
        )
    }
}
