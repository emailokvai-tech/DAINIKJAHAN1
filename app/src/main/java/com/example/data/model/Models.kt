package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val author: String,
    val category: String, // "Bangladesh", "International", "Politics", "Sports", "Economy", "Tech"
    val imageUrl: String,
    val publishTimestamp: Long = System.currentTimeMillis(),
    val sentiment: String = "Neutral", // "Positive", "Neutral", "Negative", "Critical"
    val seoKeywords: String = "",
    val seoScore: Int = 50,
    val isPublished: Boolean = false,
    val isTrending: Boolean = false,
    val views: Int = 0,
    val likes: Int = 0,
    val shares: Int = 0
)

@Entity(tableName = "social_posts")
data class SocialQueuePost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val articleId: Int,
    val articleTitle: String,
    val platforms: String, // Comma separated e.g., "Facebook, Twitter, LinkedIn"
    val optimizedContent: String,
    val scheduledTime: Long = System.currentTimeMillis(),
    val status: String = "Pending" // "Pending", "Scheduled", "Published"
)

@Entity(tableName = "analytics")
data class AnalyticsMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val metricCategory: String, // "Daily traffic", "CTR growth", "Social engagements"
    val label: String,          // e.g. "Jun 1", "Jun 2"
    val value: Float,           // e.g. 1500f
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
