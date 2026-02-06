package com.example.smartgeoreminders.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.smartgeoreminders.network.NetworkModule
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onSave: (Double, Double, String?) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var postcode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var pickedLatLng by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(51.5074, -0.1278), 10f) // London default
    }

    // Move camera when postcode lookup returns a location
    LaunchedEffect(pickedLatLng) {
        pickedLatLng?.let { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 14f),
                durationMs = 600
            )
        }
    }

    val canSave = pickedLatLng != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Text("Enter a UK postcode", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = postcode,
                onValueChange = {
                    postcode = it
                    errorMsg = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. MK9 2DN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                supportingText = {
                    errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val cleaned = postcode.trim()

                        if (cleaned.isEmpty()) {
                            errorMsg = "Please enter a postcode."
                            return@Button
                        }

                        isLoading = true
                        errorMsg = null

                        scope.launch {
                            try {
                                val response = NetworkModule.postcodeApi.lookup(cleaned)
                                val lat = response.result?.latitude
                                val lng = response.result?.longitude

                                if (lat == null || lng == null) {
                                    errorMsg = "Postcode not found."
                                    pickedLatLng = null
                                } else {
                                    pickedLatLng = LatLng(lat, lng)
                                }
                            } catch (e: Exception) {
                                errorMsg = "Network error. Check internet and try again."
                                pickedLatLng = null
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Searching..." else "Find on map")
                }

                OutlinedButton(
                    onClick = {
                        postcode = ""
                        pickedLatLng = null
                        errorMsg = null
                    },
                    enabled = !isLoading
                ) {
                    Text("Clear")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Map
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    pickedLatLng?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = postcode.trim().ifEmpty { "Selected location" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val latLng = pickedLatLng ?: return@Button
                    onSave(latLng.latitude, latLng.longitude, postcode.trim().ifEmpty { null })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text("Save location")
            }
        }
    }
}
