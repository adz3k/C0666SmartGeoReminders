package com.example.smartgeoreminders

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {
    val reminders: Flow<List<ReminderEntity>> = dao.getAllFlow()

    suspend fun insert(reminder: ReminderEntity) = dao.insert(reminder)
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)
    suspend fun getById(id: Long) = dao.getById(id)
}
