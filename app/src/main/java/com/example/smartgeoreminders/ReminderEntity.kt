package com.example.smartgeoreminders

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String,
    val radiusMeters: Int,
    val locationLabel: String,
    val isActive: Boolean
)
