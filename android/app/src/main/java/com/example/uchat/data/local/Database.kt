package com.example.uchat.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey val id: Long,
    val roomId: String,
    val type: String, // "user", "system", "invite"
    val username: String = "",
    val msg: String = "",
    val time: String = "",
    val accountId: String = "",
    val isOwn: Boolean = false,
    val fromAccount: String = "",
    val inviteRoomName: String = "",
    val inviteRoomId: String = "",
    val needPassword: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM cached_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<CachedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("DELETE FROM cached_messages WHERE roomId = :roomId")
    suspend fun clearRoom(roomId: String)

    @Query("DELETE FROM cached_messages")
    suspend fun clearAll()
}

@Database(entities = [CachedMessage::class], version = 1, exportSchema = false)
abstract class UChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: UChatDatabase? = null

        fun getInstance(context: android.content.Context): UChatDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UChatDatabase::class.java,
                    "uchat_db"
                ).build().also { INSTANCE = it }
            }
    }
}
