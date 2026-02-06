package com.example.smartgeoreminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
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
import com.example.smartgeoreminders.ui.MapPickerScreen
import com.example.smartgeoreminders.ui.theme.SmartGeoRemindersTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartGeoRemindersTheme {

                val db = remember { AppDatabase.getInstance(applicationContext) }
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
                            onGoTriggerTest = { navController.navigate("triggerTest") },
                            onLogout = {
                                sessionManager.clearSession()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("triggerTest") {
                        TriggerTestScreen(
                            reminders = reminders,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("mapPicker") {
                        MapPickerScreen(
                            onSave = { lat, lng, pc ->
                                navController.previousBackStackEntry?.savedStateHandle?.apply {
                                    set("picked_lat", lat)
                                    set("picked_lng", lng)
                                    set("picked_pc", pc)
                                }
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("create") {
                        val savedStateHandle = navController.currentBackStackEntry!!.savedStateHandle

                        val pickedLat by savedStateHandle
                            .getStateFlow<Double?>("picked_lat", null)
                            .collectAsState()

                        val pickedLng by savedStateHandle
                            .getStateFlow<Double?>("picked_lng", null)
                            .collectAsState()

                        val pickedPc by savedStateHandle
                            .getStateFlow<String?>("picked_pc", null)
                            .collectAsState()

                        var selectedLat by remember { mutableStateOf<Double?>(null) }
                        var selectedLng by remember { mutableStateOf<Double?>(null) }
                        var selectedPostcode by remember { mutableStateOf<String?>(null) }
                        var locationLabel by remember { mutableStateOf("Location not set") }

                        LaunchedEffect(pickedLat, pickedLng, pickedPc) {
                            if (pickedLat != null && pickedLng != null) {
                                selectedLat = pickedLat
                                selectedLng = pickedLng
                                selectedPostcode = pickedPc
                                locationLabel = pickedPc ?: "Custom location"

                                savedStateHandle.remove<Double?>("picked_lat")
                                savedStateHandle.remove<Double?>("picked_lng")
                                savedStateHandle.remove<String?>("picked_pc")
                            }
                        }

                        CreateReminderScreen(
                            onBack = { navController.popBackStack() },
                            onPickLocation = { navController.navigate("mapPicker") },
                            locationLabel = locationLabel,
                            onSave = { title, notes, radiusText ->
                                if (title.isBlank()) return@CreateReminderScreen
                                if (selectedLat == null || selectedLng == null) return@CreateReminderScreen

                                scope.launch {
                                    val radiusInt = radiusText.toIntOrNull() ?: 100
                                    val label = "${locationLabel}, ${radiusInt}m radius"

                                    reminderRepo.insert(
                                        ReminderEntity(
                                            title = title.trim(),
                                            notes = notes.trim(),
                                            radiusMeters = radiusInt,
                                            locationLabel = label,
                                            isActive = true,
                                            latitude = selectedLat,
                                            longitude = selectedLng,
                                            postcode = selectedPostcode
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
                            val savedStateHandle = navController.currentBackStackEntry!!.savedStateHandle

                            val pickedLat by savedStateHandle
                                .getStateFlow<Double?>("picked_lat", null)
                                .collectAsState()

                            val pickedLng by savedStateHandle
                                .getStateFlow<Double?>("picked_lng", null)
                                .collectAsState()

                            val pickedPc by savedStateHandle
                                .getStateFlow<String?>("picked_pc", null)
                                .collectAsState()

                            var selectedLat by remember { mutableStateOf(r.latitude) }
                            var selectedLng by remember { mutableStateOf(r.longitude) }
                            var selectedPostcode by remember { mutableStateOf(r.postcode) }
                            var locationLabel by remember { mutableStateOf(r.postcode ?: "Custom location") }

                            LaunchedEffect(pickedLat, pickedLng, pickedPc) {
                                if (pickedLat != null && pickedLng != null) {
                                    selectedLat = pickedLat
                                    selectedLng = pickedLng
                                    selectedPostcode = pickedPc
                                    locationLabel = pickedPc ?: "Custom location"

                                    savedStateHandle.remove<Double?>("picked_lat")
                                    savedStateHandle.remove<Double?>("picked_lng")
                                    savedStateHandle.remove<String?>("picked_pc")
                                }
                            }

                            EditReminderScreen(
                                reminder = r,
                                onBack = { navController.popBackStack() },
                                onPickLocation = { navController.navigate("mapPicker") },
                                locationLabel = locationLabel,
                                onDelete = {
                                    scope.launch {
                                        reminderRepo.delete(r)
                                        navController.popBackStack()
                                    }
                                },
                                onSave = { updatedTitle, updatedNotes, updatedRadiusText, updatedIsActive ->
                                    if (updatedTitle.isBlank()) return@EditReminderScreen
                                    if (selectedLat == null || selectedLng == null) return@EditReminderScreen

                                    scope.launch {
                                        val radiusInt = updatedRadiusText.toIntOrNull() ?: r.radiusMeters
                                        val newLabel = "${locationLabel}, ${radiusInt}m radius"

                                        reminderRepo.update(
                                            r.copy(
                                                title = updatedTitle.trim(),
                                                notes = updatedNotes.trim(),
                                                radiusMeters = radiusInt,
                                                locationLabel = newLabel,
                                                isActive = updatedIsActive,
                                                latitude = selectedLat,
                                                longitude = selectedLng,
                                                postcode = selectedPostcode
                                            )
                                        )
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
    onGoTriggerTest: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SmartGeo Reminders") },
                actions = {
                    TextButton(onClick = onGoTriggerTest) { Text("Test Trigger") }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateReminderClick) { Text("+") }
        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No reminders yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Create one and we’ll notify you when you enter the area.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onCreateReminderClick) { Text("Create Reminder") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onGoTriggerTest) { Text("Test Trigger") }
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
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

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGoTriggerTest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Trigger Test")
                }
            }
        }
    }
}
