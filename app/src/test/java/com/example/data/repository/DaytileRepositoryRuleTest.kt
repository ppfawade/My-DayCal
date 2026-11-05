package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DaytileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTask_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DaytileRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                dateKey = "2026-11-05",
                title = "Morning Architecture Block",
                notes = "Design timeline tile layout",
                startMinute = 540,
                durationMinutes = 90,
                category = "break",
                colorIndex = 4,
                reminderMinutes = 15
            )
        }
        assertTrue("Expected task creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertTrue(taskId.isNotBlank())

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(aliceUid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(
            emittedTasks.any {
                it.id == taskId &&
                    it.title == "Morning Architecture Block" &&
                    it.category == "break" &&
                    it.reminderMinutes == 15L
            }
        )
    }

    @Test
    fun updateAndDeleteTask_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DaytileRepository(firestore, auth)

        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                dateKey = "2026-11-06",
                title = "Initial Focus Session",
                startMinute = 600,
                durationMinutes = 60
            ).getOrThrow()
        }

        val createdTask = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(aliceUid).first { list -> list.any { it.id == taskId } }
        }.first { it.id == taskId }

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTask(createdTask.copy(title = "Updated Focus Session", completed = true))
        }
        assertTrue("Expected task update to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteTask(taskId)
        }
        assertTrue("Expected task delete to succeed: ${deleteResult.exceptionOrNull()}", deleteResult.isSuccess)
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DaytileRepository(firestore, auth)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(
                dateKey = "2026-11-05",
                title = "Private Task ${UUID.randomUUID()}",
                startMinute = 600,
                durationMinutes = 45
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = DaytileRepository(firestore, auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getTaskById(aliceUid, taskId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException but got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = DaytileRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks("unauthenticated_user").first()
            }
            fail("Expected PERMISSION_DENIED when observing tasks while signed out")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@daytile.test"
        const val BOB_EMAIL = "bob@daytile.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
