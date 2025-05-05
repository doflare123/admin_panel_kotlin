package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import Page

@Composable
fun Navigation(currentPage: Page, onNavigate: (Page) -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primaryContainer)
        .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavigationButton("Главная", currentPage == Page.HOME) {
                onNavigate(Page.HOME)
            }

            NavigationButton("Критики", currentPage == Page.USERS) {
                onNavigate(Page.USERS)
            }
            NavigationButton("Новый фильм", currentPage == Page.NEWFILM) {
                onNavigate(Page.NEWFILM)
            }
            NavigationButton("Новый фильм", currentPage == Page.PREVIEWREV) {
                onNavigate(Page.PREVIEWREV)
            }
        }
    }
}

@Composable
fun NavigationButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text)
    }
}
