package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import Page

@Composable
fun Navigation(currentPage: Page, onNavigate: (Page) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Button(onClick = { onNavigate(Page.HOME) }) {
            Text("Главная")
        }
    }
}
