import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.searchMovie
import models.insertFilmToDb

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding -> Column(modifier = 
                                    Modifier.fillMaxSize().padding(16.dp), 
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) 
    {
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
                        searchResult = SearchUiData(
                            title = film.name,
                            description = film.description,
                            genres = film.genres.joinToString(", ") { it.name },
                            year = film.year.toString()
                        )
                        title = film.name
                        description = film.description
                        genres = film.genres.joinToString(", ") { it.name }
                        year = film.year.toString()
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
            Text("Фильм не найден. Введите данные вручную.", color = MaterialTheme.colorScheme.error)
        }

        TextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
        TextField(value = description, onValueChange = { description = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
        TextField(value = genres, onValueChange = { genres = it }, label = { Text("Жанры (через запятую)") }, modifier = Modifier.fillMaxWidth())
        TextField(value = year, onValueChange = { year = it }, label = { Text("Год выпуска") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                scope.launch {
                    // можно вставку в БД тоже здесь, если insertFilmToDb не блокирует поток
                    withContext(Dispatchers.IO) {
                        insertFilmToDb(title, description, genres, year.toIntOrNull() ?: 0)
                    }

                    // UI обновляем сразу в этом же scope
                    title = ""
                    description = ""
                    genres = ""
                    year = ""

                    snackbarHostState.showSnackbar("Фильм успешно добавлен в базу данных")
                }
            },
            enabled = title.isNotBlank()
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
