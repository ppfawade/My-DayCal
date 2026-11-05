package com.example.data.repository

import com.example.data.local.LocalDaytileDao
import com.example.data.local.LocalTaskEntity
import com.example.data.model.DayTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface PlannerDataSource {
    val isCloudBacked: Boolean
    fun observeTasks(): Flow<List<DayTask>>
    suspend fun createTask(
        dateKey: String,
        title: String,
        notes: String = "",
        startMinute: Int = 540,
        durationMinutes: Int = 60,
        category: String = "focus",
        colorIndex: Int = 0,
        reminderMinutes: Int = -1,
        completed: Boolean = false
    ): Result<String>

    suspend fun updateTask(task: DayTask): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
}

class CloudPlannerDataSource(
    private val repository: DaytileRepository,
    private val userId: String
) : PlannerDataSource {
    override val isCloudBacked: Boolean = true

    override fun observeTasks(): Flow<List<DayTask>> =
        repository.observeTasks(userId)

    override suspend fun createTask(
        dateKey: String,
        title: String,
        notes: String,
        startMinute: Int,
        durationMinutes: Int,
        category: String,
        colorIndex: Int,
        reminderMinutes: Int,
        completed: Boolean
    ): Result<String> = repository.createTask(
        dateKey = dateKey,
        title = title,
        notes = notes,
        startMinute = startMinute,
        durationMinutes = durationMinutes,
        category = category,
        colorIndex = colorIndex,
        reminderMinutes = reminderMinutes,
        completed = completed
    )

    override suspend fun updateTask(task: DayTask): Result<Unit> =
        repository.updateTask(task)

    override suspend fun deleteTask(taskId: String): Result<Unit> =
        repository.deleteTask(taskId)
}

class LocalPlannerDataSource(
    private val dao: LocalDaytileDao
) : PlannerDataSource {
    override val isCloudBacked: Boolean = false

    override fun observeTasks(): Flow<List<DayTask>> =
        dao.observeAllTasks().map { list -> list.map { it.toDomainModel() } }

    override suspend fun createTask(
        dateKey: String,
        title: String,
        notes: String,
        startMinute: Int,
        durationMinutes: Int,
        category: String,
        colorIndex: Int,
        reminderMinutes: Int,
        completed: Boolean
    ): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val entity = LocalTaskEntity(
            id = id,
            dateKey = dateKey,
            title = title.trim().take(200),
            notes = notes.trim().take(2000),
            startMinute = startMinute.coerceIn(0, 1439).toLong(),
            durationMinutes = durationMinutes.coerceIn(15, 720).toLong(),
            category = category,
            colorIndex = colorIndex.coerceIn(0, 5).toLong(),
            reminderMinutes = reminderMinutes.coerceIn(-1, 1440).toLong(),
            completed = completed,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        dao.upsertTask(entity)
        id
    }

    override suspend fun updateTask(task: DayTask): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val createdMillis = task.createdAt?.toDate()?.time ?: now
        val entity = LocalTaskEntity(
            id = task.id.ifBlank { UUID.randomUUID().toString() },
            dateKey = task.dateKey,
            title = task.title.trim().take(200),
            notes = task.notes.trim().take(2000),
            startMinute = task.startMinute.coerceIn(0L, 1439L),
            durationMinutes = task.durationMinutes.coerceIn(15L, 720L),
            category = task.category,
            colorIndex = task.colorIndex.coerceIn(0L, 5L),
            reminderMinutes = task.reminderMinutes.coerceIn(-1L, 1440L),
            completed = task.completed,
            createdAtEpochMillis = createdMillis,
            updatedAtEpochMillis = now
        )
        dao.upsertTask(entity)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        dao.deleteTaskById(taskId)
    }
}
