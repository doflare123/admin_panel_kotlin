package ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import models.UserRepository
import models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementPage() {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf(listOf<User>()) }
    var nameInput by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var isAddingUser by remember { mutableStateOf(false) }

    fun refreshUsers() {
        scope.launch(Dispatchers.IO) {
            users = UserRepository.getAllUsers()
        }
    }

    LaunchedEffect(Unit) {
        refreshUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        if (!isAddingUser && selectedUser == null) {
            Button(onClick = {
                isAddingUser = true
                nameInput = ""
            }) {
                Text("Добавить пользователя")
            }
        }

        if (isAddingUser || selectedUser != null) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Имя") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (selectedUser == null) {
                                UserRepository.addUser(nameInput)
                            } else {
                                UserRepository.updateUser(selectedUser!!.id, nameInput)
                                selectedUser = null
                            }
                            nameInput = ""
                            isAddingUser = false
                            refreshUsers()
                        }
                    }
                ) {
                    Text(if (selectedUser == null) "Сохранить" else "Обновить")
                }

                OutlinedButton(onClick = {
                    selectedUser = null
                    nameInput = ""
                    isAddingUser = false
                }) {
                    Text("Отмена")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(users) { user ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = user.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                selectedUser = user
                                nameInput = user.name
                                isAddingUser = false
                            }) {
                                Text("Изм.")
                            }
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    UserRepository.deleteUser(user.id)
                                    refreshUsers()
                                }
                            }) {
                                Text("Удал.")
                            }
                        }
                    }
                }
            }
        }
    }
}