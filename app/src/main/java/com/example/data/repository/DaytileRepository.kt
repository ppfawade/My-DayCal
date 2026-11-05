package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DaytileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw FirebaseFirestoreException(
                "User must be signed in with Google before accessing Firestore.",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
    }

    fun observeTasks(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<DayTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DayTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception) ?: Exception(error)
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeJournals(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<DayJournal>> = flow {
        val path = "users/$userId/journals"
        emitAll(
            db.collection("users").document(userId).collection("journals")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DayJournal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception) ?: Exception(error)
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createTask(
        dateKey: String,
        title: String,
        notes: String = "",
        startMinute: Int = 540,
        durationMinutes: Int = 60,
        category: String = "focus",
        colorIndex: Int = 0,
        completed: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val tasksRef = db.collection("users").document(uid).collection("tasks")
        val docRef = tasksRef.document()
        val task = DayTask(
            id = docRef.id,
            userId = uid,
            dateKey = dateKey,
            title = title.trim().take(200),
            notes = notes.trim().take(2000),
            startMinute = startMinute.coerceIn(0, 1439).toLong(),
            durationMinutes = durationMinutes.coerceIn(15, 720).toLong(),
            category = category,
            colorIndex = colorIndex.coerceIn(0, 5).toLong(),
            completed = completed,
            createdAt = null,
            updatedAt = null
        )
        try {
            docRef.set(task).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTask(task: DayTask): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        try {
            val preservedCreatedAt = task.createdAt ?: docRef.get().await().getTimestamp("createdAt")
            requireNotNull(preservedCreatedAt) { "Task createdAt must not be null for update" }
            val updated = task.copy(
                userId = uid,
                title = task.title.trim().take(200),
                notes = task.notes.trim().take(2000),
                startMinute = task.startMinute.coerceIn(0L, 1439L),
                durationMinutes = task.durationMinutes.coerceIn(15L, 720L),
                colorIndex = task.colorIndex.coerceIn(0L, 5L),
                createdAt = preservedCreatedAt,
                updatedAt = null
            )
            docRef.set(updated).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getUserTasks(userId: String = requireUserId()): Result<List<DayTask>> = runCatching {
        val path = "users/$userId/tasks"
        try {
            val snapshot = db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(DayTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getTaskById(ownerUserId: String, taskId: String): Result<DayTask?> = runCatching {
        val docRef = db.collection("users").document(ownerUserId).collection("tasks").document(taskId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(DayTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveJournal(
        dateKey: String,
        headline: String,
        content: String,
        moodScore: Int,
        existingCreatedAt: Timestamp? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("journals").document(dateKey)
        try {
            val resolvedCreatedAt = existingCreatedAt ?: run {
                val snap = docRef.get().await()
                if (snap.exists()) snap.getTimestamp("createdAt") else null
            }
            val journal = DayJournal(
                id = dateKey,
                userId = uid,
                dateKey = dateKey,
                headline = headline.trim().take(200),
                content = content.trim().take(10000),
                moodScore = moodScore.coerceIn(1, 5).toLong(),
                createdAt = resolvedCreatedAt,
                updatedAt = null
            )
            docRef.set(journal).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (existingCreatedAt == null) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, docRef.path)
            throw e
        }
    }
}
