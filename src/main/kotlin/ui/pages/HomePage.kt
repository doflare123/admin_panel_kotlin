package ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomePage() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Главная статистика", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Просмотрено фильмов: ...")
        Text("Ожидают оценки: ...")
        Text("Ждут просмотра: ...")
        Text("Средняя оценка: ...")
        Text("Средняя оценка по участникам: ...")
    }
}
