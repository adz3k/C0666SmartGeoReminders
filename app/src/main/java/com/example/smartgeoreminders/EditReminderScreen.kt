package com.example.smartgeoreminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    reminder: ReminderEntity,
    onBack: () -> Unit,
    onPickLocation: () -> Unit,
    locationLabel: String,
    onDelete: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var title by remember(reminder.id) { mutableStateOf(reminder.title) }
    var notes by remember(reminder.id) { mutableStateOf(reminder.notes) }
    var radiusText by remember(reminder.id) { mutableStateOf(reminder.radiusMeters.toString()) }
    var isActive by remember(reminder.id) { mutableStateOf(reminder.isActive) }

    var error by remember { mutableStateOf<String?>(null) }

    val radiusInt = radiusText.toIntOrNull()
    val hasLocation = locationLabel.isNotBlank()
    val canSave =
        title.trim().isNotEmpty() &&
                (radiusInt != null && radiusInt in 10..5000) &&
                hasLocation

    fun validate(): Boolean {
        val t = title.trim()
        val r = radiusText.toIntOrNull()

        error = when {
            t.isEmpty() -> "Title is required."
            r == null -> "Radius must be a number."
            r !in 10..5000 -> "Radius should be between 10 and 5000 meters."
            !hasLocation -> "Pick a location on the map."
            else -> null
        }
        return error == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Reminder") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = onDelete) { Text("Delete") } }
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
            error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = null },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = radiusText,
                onValueChange = { radiusText = it; error = null },
                label = { Text("Radius (meters) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Active")
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Location *", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(locationLabel)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onPickLocation) {
                        Text("Change location on map")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (validate()) onSave(title, notes, radiusText, isActive)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text("Save Changes")
            }
        }
    }
}
