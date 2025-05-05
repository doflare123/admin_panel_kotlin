package ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class UserStats(
    val name: String,
    val avgExpectation: Double,
    val avgImpression: Double,
    val watchedCount: Int
)

@Composable
fun HomePage() {
    val scrollState = rememberScrollState()

    var totalStats by remember { mutableStateOf(mapOf<String, String>()) }
    var userStats by remember { mutableStateOf(listOf<UserStats>()) }
    var statusChartData by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
    var genreChartData by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            transaction {
                val stats = mutableMapOf<String, String>()
                val usersList = mutableListOf<UserStats>()
                val statusCounts = mutableMapOf<String, Int>()
                val genreCounts = mutableMapOf<String, Int>()

                // Подсчёт по статусам фильмов
                FilmStatus.values().forEach { status ->
                    val count = Films.select { Films.status eq status }.count()
                    statusCounts[status.name] = count
                }

                val avgImpression = UserImpressions.slice(UserImpressions.rating.avg())
                    .selectAll().first()[UserImpressions.rating.avg()] ?: 0.0

                val validExpectations = UserExpectations.selectAll()
                    .mapNotNull { it[UserExpectations.rating].takeIf { r -> r >= 0 } }
                val avgExpectation = if (validExpectations.isNotEmpty()) validExpectations.average() else 0.0

                stats["Просмотрено фильмов"] = statusCounts[FilmStatus.COMPLETED.name].toString()
                stats["Ожидают оценки"] = statusCounts[FilmStatus.ON_REVIEW.name].toString()
                stats["Ждут просмотра"] = statusCounts[FilmStatus.WAITING_WATCH.name].toString()
                stats["Оценены после просмотра"] = statusCounts[FilmStatus.WATCHED_WAIT_RATE.name].toString()
                stats["Отвергнуты"] = statusCounts[FilmStatus.Failed_Selection.name].toString()
                stats["Средняя оценка"] = "%.2f".format(avgImpression)
                stats["Средняя оценка ожиданий"] = "%.2f".format(avgExpectation)

                // Статистика по пользователям
                val users = Users.selectAll()
                for (user in users) {
                    val uid = user[Users.id].value
                    val name = user[Users.name]

                    val userExp = UserExpectations.select { UserExpectations.user eq uid }
                        .map { it[UserExpectations.rating] }.filter { it >= 0 }

                    val userImp = UserImpressions.select { UserImpressions.user eq uid }
                        .map { it[UserImpressions.rating] }

                    val avgExp = if (userExp.isNotEmpty()) userExp.average() else 0.0
                    val avgImp = if (userImp.isNotEmpty()) userImp.average() else 0.0

                    usersList += UserStats(name, avgExp, avgImp, userImp.size)
                }

                // Жанры: количество фильмов каждого жанра
                val joined = FilmGenres.innerJoin(Genres)
                val genreGrouped = joined
                    .slice(Genres.name, FilmGenres.film.count())
                    .selectAll()
                    .groupBy(Genres.name)

                genreGrouped.forEach {
                    genreCounts[it[Genres.name]] = it[FilmGenres.film.count()].toInt()
                }

                totalStats = stats
                userStats = usersList
                statusChartData = statusCounts.toList()
                genreChartData = genreCounts.toList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Главная", style = MaterialTheme.typography.headlineMedium)

        // Общая статистика
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                totalStats.forEach { (label, value) ->
                    StatItem(label = label, value = value)
                }
            }
        }

        // 📊 Диаграмма по статусам
        if (statusChartData.isNotEmpty()) {
            Text("Статистика по статусам", style = MaterialTheme.typography.titleLarge)
            PieChartWithLabels(statusChartData)
        }

        // 📊 Диаграмма по жанрам
        if (genreChartData.isNotEmpty()) {
            Text("Популярность жанров", style = MaterialTheme.typography.titleLarge)
            BarChartWithLabels(genreChartData)
        }

        // 📋 Статистика по пользователям
        Text("Статистика по участникам", style = MaterialTheme.typography.titleLarge)
        userStats.forEach {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(it.name, style = MaterialTheme.typography.titleMedium)
                    StatItem("Средняя оценка ожидания", "%.2f".format(it.avgExpectation))
                    StatItem("Средняя оценка после просмотра", "%.2f".format(it.avgImpression))
                    StatItem("Фильмов просмотрено", it.watchedCount.toString())
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
