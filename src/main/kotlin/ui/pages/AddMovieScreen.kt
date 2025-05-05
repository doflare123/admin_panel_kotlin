import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.insertFilmToDb
import models.filmExistsInDb
import network.searchMovie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieScreen() {
    var titleQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<SearchUiData?>(null) }
    var notFound by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genres by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var partsCount by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Добавить фильм", style = MaterialTheme.typography.headlineMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = titleQuery,
                    onValueChange = { titleQuery = it },
                    label = { Text("Название для поиска") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = searchMovie(titleQuery)
                        if (result?.docs?.isNotEmpty() == true) {
                            val film = result.docs.first()
                            val filmTitle = film.name ?: ""
                            searchResult = SearchUiData(
                                title = filmTitle,
                                description = film.description ?: "",
                                genres = film.genres.joinToString(", ") { it.name },
                                year = film.year?.toString() ?: ""
                            )
                            title = filmTitle
                            description = film.description ?: ""
                            genres = film.genres.joinToString(", ") { it.name }
                            year = film.year?.toString() ?: ""
                            notFound = false
                        } else {
                            notFound = true
                            searchResult = null
                            title = ""
                            description = ""
                            genres = ""
                            year = ""
                        }
                    }
                }) {
                    Text("Найти")
                }
            }

            if (notFound) {
                Text(
                    "Фильм не найден. Введите данные вручную.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
            TextField(value = description, onValueChange = { description = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
            TextField(value = genres, onValueChange = { genres = it }, label = { Text("Жанры (через запятую)") }, modifier = Modifier.fillMaxWidth())
            TextField(value = year, onValueChange = { year = it }, label = { Text("Год выпуска") }, modifier = Modifier.fillMaxWidth())
            TextField(value = partsCount, onValueChange = { partsCount = it }, label = { Text("Количество частей (опционально)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val count = partsCount.toIntOrNull()

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val insertedTitles = mutableListOf<String>()

                            // 1. Вставка первой части вручную (если заполнено)
                            if (title.isNotBlank() && description.isNotBlank()) {
                                if (!filmExistsInDb(title)) {
                                    insertFilmToDb(title, description, genres, year.toIntOrNull() ?: 0)
                                    insertedTitles.add(title)
                                }
                            }

                            // 2. Вставка остальных частей, если указано число
                            if (count != null && count > 1) {
                                val start = if (title.isNotBlank()) 2 else 1

                                for (i in start..count) {
                                    val query = "$titleQuery $i"
                                    val result = searchMovie(query)
                                    val doc = result?.docs?.firstOrNull() ?: continue
                                    val newTitle = doc.name ?: continue

                                    if (!filmExistsInDb(newTitle)) {
                                        insertFilmToDb(
                                            newTitle,
                                            doc.description ?: "",
                                            doc.genres.joinToString(", ") { it.name },
                                            doc.year ?: 0
                                        )
                                        insertedTitles.add(newTitle)
                                    }
                                }
                            }

                            withContext(Dispatchers.Main) {
                                title = ""
                                description = ""
                                genres = ""
                                year = ""
                                partsCount = ""
                                snackbarHostState.showSnackbar("Добавлены фильмы: ${insertedTitles.joinToString()}")
                            }
                        }
                    }
                },
                enabled = titleQuery.isNotBlank()
            ) {
                Text("Сохранить в БД")
            }
        }
    }
}

data class SearchUiData(
    val title: String,
    val description: String,
    val genres: String,
    val year: String
)