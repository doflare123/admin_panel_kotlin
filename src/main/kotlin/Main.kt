import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.components.Navigation
import ui.pages.HomePage
import ui.pages.UserManagementPage
import models.DbSettings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

enum class Page { HOME, USERS, NEWFILM, PREVIEWREV }

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    DbSettings.init()
    var currentPage by remember { mutableStateOf(Page.HOME) }

    Window(onCloseRequest = ::exitApplication, title = "Admin Panel") {
        MaterialTheme {
            Scaffold(
                topBar = {
                    Navigation(currentPage) { selectedPage ->
                        currentPage = selectedPage
                    }
                },
                content = { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentPage) {
                            Page.HOME -> HomePage()
                            Page.USERS -> UserManagementPage()
                            Page.NEWFILM -> AddMovieScreen()
                            Page.PREVIEWREV -> ReviewRatingsScreen()
                        }
                    }
                }
            )
        }
    }
}
