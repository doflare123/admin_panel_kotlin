import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.insertFilmToDb
import models.filmExistsInDb
import models.UserExpectations
import network.searchMovie
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRatingsScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var films by remember { mutableStateOf(listOf<Film>()) }
    var users by remember { mutableStateOf(listOf<User>()) }
    val ratings = remember { mutableStateMapOf<Pair<Int, Int>, String>() }
    val averages = remember { mutableStateMapOf<Int, Double>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedFilms = getFilmsOnReview()
            val loadedUsers = getAllUsers()
            val loadedRatings = mutableMapOf<Pair<Int, Int>, String>()
            val loadedAverages = mutableMapOf<Int, Double>()

            for (film in loadedFilms) {
                val filmRatings = mutableListOf<Double>()
                for (user in loadedUsers) {
                    val value = getUserExpectation(film.id, user.id)
                    if (value != null) {
                        loadedRatings[film.id to user.id] = if (value < 0) "-" else value.toString()
                        if (value >= 0) filmRatings += value
                    }
                }
                loadedAverages[film.id] = if (filmRatings.isNotEmpty()) filmRatings.average() else 0.0
            }

            films = loadedFilms
            users = loadedUsers
            ratings.putAll(loadedRatings)
            averages.putAll(loadedAverages)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.padding(16.dp).verticalScroll(scrollState)) {
            Text("Оценка ожиданий", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth()) {
                Text("Фильм", Modifier.weight(2f))
                users.forEach { user ->
                    Text(user.name, Modifier.weight(1f), maxLines = 1)
                }
                Text("Средняя", Modifier.width(80.dp))
            }

            Spacer(Modifier.height(8.dp))

            films.forEach { film ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(film.title, Modifier.weight(2f))
    
                    users.forEach { user ->
                        val key = film.id to user.id
                        var input by remember { mutableStateOf(ratings[key] ?: "") }
    
                        TextField(
                            value = input,
                            onValueChange = {
                                input = it
                                ratings[key] = it
    
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        saveUserExpectationAndUpdateStatus(film.id, user.id, it)
                                    }
    
                                    if (result) {
                                        withContext(Dispatchers.IO) {
                                            val updatedRatings = transaction {
                                                UserExpectations.select {
                                                    UserExpectations.film eq film.id
                                                }.map { it[UserExpectations.rating] }
                                            }.filter { it.toInt() >= 0 }
    
                                            val newAvg = if (updatedRatings.isNotEmpty()) updatedRatings.average() else 0.0
                                            averages[film.id] = newAvg
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Ошибка сохранения")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "%.2f".format(averages[film.id] ?: 0.0),
                        modifier = Modifier.width(80.dp),
                        color = if ((averages[film.id] ?: 0.0) >= 5.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
