package com.example.smartgeoreminders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    reminder: ReminderEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    var title by remember { mutableStateOf(reminder.title) }
    var notes by remember { mutableStateOf(reminder.notes) }
    var radius by remember { mutableStateOf(reminder.radiusMeters.toString()) }
    var isActive by remember { mutableStateOf(reminder.isActive) }

    val titleError = title.isBlank()
    val radiusError = radius.isNotBlank() && radius.toIntOrNull() == null
    val canSave = title.isNotBlank() && !radiusError

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Reminder") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                actions = {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
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
            Text("Location", style = MaterialTheme.typography.labelLarge)
            Text(reminder.locationLabel, style = MaterialTheme.typography.bodyMedium)

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

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = isActive, onCheckedChange = { isActive = it })
                Spacer(Modifier.width(8.dp))
                Text(if (isActive) "Active" else "Off")
            }

            Button(
                onClick = {
                    val newRadius = radius.toIntOrNull() ?: reminder.radiusMeters
                    val newLabel = "Location not set, ${newRadius}m radius"
                    onSave(
                        reminder.copy(
                            title = title.trim(),
                            notes = notes.trim(),
                            radiusMeters = newRadius,
                            locationLabel = newLabel,
                            isActive = isActive
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Changes") }
        }
    }
}
