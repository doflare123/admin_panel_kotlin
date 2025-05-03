import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.components.Navigation
import ui.pages.HomePage

enum class Page { HOME /*, ADD, PENDING, ... */ }

fun main() = application {
    var currentPage by remember { mutableStateOf(Page.HOME) }

    Window(onCloseRequest = ::exitApplication, title = "admin panel") {
        Navigation(currentPage) { selectedPage: Page ->
            currentPage = selectedPage
        }

        when (currentPage) {
            Page.HOME -> HomePage()
        }
    }
}
