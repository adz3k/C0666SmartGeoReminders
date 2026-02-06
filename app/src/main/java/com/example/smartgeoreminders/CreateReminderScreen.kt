package com.example.smartgeoreminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    onBack: () -> Unit,
    onPickLocation: () -> Unit,
    locationLabel: String,
    onSave: (String, String, String) -> Unit
) {
    // ✅ survives navigation away/back
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var radiusText by rememberSaveable { mutableStateOf("100") }

    // ✅ validation errors
    var titleError by rememberSaveable { mutableStateOf<String?>(null) }
    var radiusError by rememberSaveable { mutableStateOf<String?>(null) }
    var locationError by rememberSaveable { mutableStateOf<String?>(null) }

    val radiusInt = radiusText.toIntOrNull()
    val hasLocation = locationLabel.isNotBlank() && locationLabel != "Location not set"

    fun validateAll(): Boolean {
        val t = title.trim()

        titleError = if (t.isEmpty()) "Title is required." else null

        radiusError = when {
            radiusText.trim().isEmpty() -> "Radius is required."
            radiusInt == null -> "Radius must be a number."
            radiusInt <= 0 -> "Radius must be greater than 0."
            else -> null
        }

        locationError = if (!hasLocation) "Please pick a location." else null

        return titleError == null && radiusError == null && locationError == null
    }

    val canSave = title.trim().isNotEmpty() && radiusInt != null && radiusInt > 0 && hasLocation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Reminder") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
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
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                isError = titleError != null,
                supportingText = { titleError?.let { Text(it) } }
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
                onValueChange = {
                    // optional numeric-only filter:
                    radiusText = it.filter { ch -> ch.isDigit() }
                    if (radiusError != null) radiusError = null
                },
                label = { Text("Radius (meters)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = radiusError != null,
                supportingText = { radiusError?.let { Text(it) } }
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Location", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(locationLabel)

                    locationError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            // clear location error if they are about to pick
                            locationError = null
                            onPickLocation()
                        }
                    ) {
                        Text("Pick location on map")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (validateAll()) {
                        onSave(title.trim(), notes.trim(), radiusText)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text("Save Reminder")
            }
        }
    }
}
