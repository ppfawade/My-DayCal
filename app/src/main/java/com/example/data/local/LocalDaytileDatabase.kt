package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "local_tasks")
data class LocalTaskEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val title: String,
    val notes: String,
    val startMinute: Long,
    val durationMinutes: Long,
    val category: String,
    val colorIndex: Long,
    val reminderMinutes: Long = -1L,
    val completed: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    fun toDomainModel(): DayTask = DayTask(
        id = id,
        userId = "local_user",
        dateKey = dateKey,
        title = title,
        notes = notes,
        startMinute = startMinute,
        durationMinutes = durationMinutes,
        category = category,
        colorIndex = colorIndex,
        reminderMinutes = reminderMinutes,
        completed = completed,
        createdAt = Timestamp(Date(createdAtEpochMillis)),
        updatedAt = Timestamp(Date(updatedAtEpochMillis))
    )
}

@Entity(tableName = "local_journals")
data class LocalJournalEntity(
    @PrimaryKey val dateKey: String,
    val headline: String,
    val content: String,
    val moodScore: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    fun toDomainModel(): DayJournal = DayJournal(
        id = dateKey,
        userId = "local_user",
        dateKey = dateKey,
        headline = headline,
        content = content,
        moodScore = moodScore,
        createdAt = Timestamp(Date(createdAtEpochMillis)),
        updatedAt = Timestamp(Date(updatedAtEpochMillis))
    )
}

@Dao
interface LocalDaytileDao {
    @Query("SELECT * FROM local_tasks ORDER BY dateKey DESC, startMinute ASC")
    fun observeAllTasks(): Flow<List<LocalTaskEntity>>

    @Query("SELECT * FROM local_journals ORDER BY dateKey DESC")
    fun observeAllJournals(): Flow<List<LocalJournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: LocalTaskEntity)

    @Query("DELETE FROM local_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM local_journals WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getJournalByDate(dateKey: String): LocalJournalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournal(journal: LocalJournalEntity)
}

@Database(
    entities = [LocalTaskEntity::class, LocalJournalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LocalDaytileDatabase : RoomDatabase() {
    abstract fun daytileDao(): LocalDaytileDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDaytileDatabase? = null

        fun getInstance(context: Context): LocalDaytileDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalDaytileDatabase::class.java,
                    "my_daycal_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
