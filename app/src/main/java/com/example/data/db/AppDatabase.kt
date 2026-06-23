package com.example.data.db

import androidx.room.*
import com.example.data.model.Article
import com.example.data.model.SocialQueuePost
import com.example.data.model.AnalyticsMetric
import com.example.data.model.NotificationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishTimestamp DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isPublished = 1 ORDER BY publishTimestamp DESC")
    fun getPublishedArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isTrending = 1 ORDER BY publishTimestamp DESC")
    fun getTrendingArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Int): Article?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article): Long

    @Update
    suspend fun updateArticle(article: Article)

    @Delete
    suspend fun deleteArticle(article: Article)
}

@Dao
interface SocialPostDao {
    @Query("SELECT * FROM social_posts ORDER BY scheduledTime DESC")
    fun getAllSocialPosts(): Flow<List<SocialQueuePost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialPost(post: SocialQueuePost)

    @Update
    suspend fun updateSocialPost(post: SocialQueuePost)

    @Delete
    suspend fun deleteSocialPost(post: SocialQueuePost)
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics ORDER BY timestamp ASC")
    fun getAllMetrics(): Flow<List<AnalyticsMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: AnalyticsMetric)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<AnalyticsMetric>)

    @Query("DELETE FROM analytics")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationRecord>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationRecord)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Database(
    entities = [
        Article::class,
        SocialQueuePost::class,
        AnalyticsMetric::class,
        NotificationRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun socialPostDao(): SocialPostDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dainik_jahan_db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
