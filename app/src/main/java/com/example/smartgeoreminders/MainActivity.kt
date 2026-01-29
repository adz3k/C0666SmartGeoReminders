package com.example.smartgeoreminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.smartgeoreminders.ui.theme.SmartGeoRemindersTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartGeoRemindersTheme {

                val db = remember {
                    Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        "smartgeo_reminders.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                }

                val reminderDao = remember { db.reminderDao() }
                val userDao = remember { db.userDao() }

                val reminderRepo = remember { ReminderRepository(reminderDao) }
                val authRepo = remember { AuthRepository(userDao) }
                val sessionManager = remember { SessionManager(applicationContext) }

                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                val reminders by reminderRepo.reminders.collectAsState(initial = emptyList())

                val startDestination = remember {
                    if (sessionManager.isLoggedIn()) "home" else "login"
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable("login") {
                        LoginScreen(
                            authRepository = authRepo,
                            sessionManager = sessionManager,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoRegister = { navController.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            authRepository = authRepo,
                            onRegisterSuccessGoLogin = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            reminders = reminders,
                            onCreateReminderClick = { navController.navigate("create") },
                            onReminderClick = { r -> navController.navigate("edit/${r.id}") },
                            onLogout = {
                                sessionManager.clearSession()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("create") {
                        CreateReminderScreen(
                            onBack = { navController.popBackStack() },
                            onSave = { title, notes, radiusText ->
                                scope.launch {
                                    val radiusInt = radiusText.toIntOrNull() ?: 100
                                    val label = "Location not set, ${radiusInt}m radius"
                                    reminderRepo.insert(
                                        ReminderEntity(
                                            title = title.trim(),
                                            notes = notes.trim(),
                                            radiusMeters = radiusInt,
                                            locationLabel = label,
                                            isActive = true
                                        )
                                    )
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable("edit/{id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                        if (id == null) {
                            navController.popBackStack()
                            return@composable
                        }

                        var reminder by remember(id) { mutableStateOf<ReminderEntity?>(null) }

                        LaunchedEffect(id) {
                            reminder = reminderRepo.getById(id)
                            if (reminder == null) navController.popBackStack()
                        }

                        reminder?.let { r ->
                            EditReminderScreen(
                                reminder = r,
                                onBack = { navController.popBackStack() },
                                onDelete = {
                                    scope.launch {
                                        reminderRepo.delete(r)
                                        navController.popBackStack()
                                    }
                                },
                                onSave = { updated ->
                                    scope.launch {
                                        reminderRepo.update(updated)
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    reminders: List<ReminderEntity>,
    onCreateReminderClick: () -> Unit,
    onReminderClick: (ReminderEntity) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SmartGeo Reminders") },
                actions = { TextButton(onClick = onLogout) { Text("Logout") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onCreateReminderClick) { Text("+") } }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Your reminders", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            if (reminders.isEmpty()) {
                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No reminders yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Create one and we’ll notify you when you enter the area.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onCreateReminderClick) { Text("Create Reminder") }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(reminders) { r ->
                        Card(onClick = { onReminderClick(r) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(r.locationLabel, style = MaterialTheme.typography.bodyMedium)
                                }
                                AssistChip(
                                    onClick = { },
                                    label = { Text(if (r.isActive) "Active" else "Off") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
