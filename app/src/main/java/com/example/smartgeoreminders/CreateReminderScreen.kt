package com.example.smartgeoreminders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    onBack: () -> Unit,
    onSave: (title: String, notes: String, radiusMeters: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("") }

    val titleError = title.isBlank()
    val radiusError = radius.isNotBlank() && radius.toIntOrNull() == null
    val canSave = title.isNotBlank() && !radiusError

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Reminder") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                isError = titleError,
                modifier = Modifier.fillMaxWidth()
            )
            if (titleError) Text("Title is required", color = MaterialTheme.colorScheme.error)

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Radius (meters)") },
                isError = radiusError,
                modifier = Modifier.fillMaxWidth()
            )
            if (radiusError) Text("Radius must be a number", color = MaterialTheme.colorScheme.error)

            Button(
                onClick = { onSave(title.trim(), notes.trim(), radius.trim()) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Reminder") }
        }
    }
}
