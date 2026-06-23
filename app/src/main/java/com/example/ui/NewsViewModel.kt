package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.TrendItem
import com.example.api.GmailClient
import com.example.api.GmailMailDetail
import com.example.data.db.AppDatabase
import com.example.data.model.Article
import com.example.data.model.SocialQueuePost
import com.example.data.model.AnalyticsMetric
import com.example.data.model.NotificationRecord
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NewsRepository
    
    // States from local SQLite database (observed reactively)
    val articles: StateFlow<List<Article>>
    val publishedArticles: StateFlow<List<Article>>
    val trendingArticles: StateFlow<List<Article>>
    val socialPosts: StateFlow<List<SocialQueuePost>>
    val analyticsMetrics: StateFlow<List<AnalyticsMetric>>
    val notifications: StateFlow<List<NotificationRecord>>
    val unreadNotificationsCount: StateFlow<Int>

    // Editor Draft state
    var draftTitle = MutableStateFlow("")
    var draftContent = MutableStateFlow("")
    var draftCategory = MutableStateFlow("Bangladesh")
    var draftAuthor = MutableStateFlow("স্টাফ রিপোর্টার")
    
    var seoKeywordsOutput = MutableStateFlow("")
    var seoScoreOutput = MutableStateFlow(0)
    var sentimentOutput = MutableStateFlow("")

    // Interactive states
    var selectedArticle = MutableStateFlow<Article?>(null)
    var selectedCategoryTab = MutableStateFlow("All")
    var userPreferredCategory = MutableStateFlow("Bangladesh") // For personalized recommendation anchor
    
    // Gmail Integration states
    val gmailAccessToken = MutableStateFlow("")
    val isGmailConnected = MutableStateFlow(false)
    val isFetchingGmail = MutableStateFlow(false)
    val gmailMails = MutableStateFlow<List<GmailMailDetail>>(emptyList())

    // API Call Loaders
    var isRefreshing = MutableStateFlow(false)
    var aiRewriting = MutableStateFlow(false)
    var sentimentLoading = MutableStateFlow(false)
    var seoLoading = MutableStateFlow(false)
    var trendsLoading = MutableStateFlow(false)
    
    // Live Gemini Results
    private val _activeTrends = MutableStateFlow<List<TrendItem>>(emptyList())
    val activeTrends: StateFlow<List<TrendItem>> = _activeTrends.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NewsRepository(database)

        articles = repository.articles
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        publishedArticles = repository.publishedArticles
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        trendingArticles = repository.trendingArticles
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        socialPosts = repository.socialPosts
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        analyticsMetrics = repository.analyticsMetrics
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        notifications = repository.notifications
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        unreadNotificationsCount = repository.unreadNotificationsCount
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)

        // Seed data and fetch initial hot trends
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
            fetchHotTrends()
        }
    }

    fun setDraftSubject(title: String, content: String) {
        draftTitle.value = title
        draftContent.value = content
    }

    // Refresh everything
    fun refreshData() {
        viewModelScope.launch {
            isRefreshing.value = true
            fetchHotTrends()
            isRefreshing.value = false
        }
    }

    // Load trends from Gemini
    fun fetchHotTrends() {
        viewModelScope.launch {
            trendsLoading.value = true
            try {
                val trends = GeminiClient.spotTrends()
                _activeTrends.value = trends
            } catch (e: Exception) {
                // fallbacks handled inside client
            } finally {
                trendsLoading.value = false
            }
        }
    }

    // Publish New Article (from Journalist workspace)
    fun publishDraftArticle() {
        viewModelScope.launch {
            val title = draftTitle.value.trim()
            val text = draftContent.value.trim()
            if (title.isEmpty() || text.isEmpty()) return@launch

            val newArticle = Article(
                title = title,
                content = text,
                author = draftAuthor.value.trim().ifEmpty { "স্টাফ রিপোর্টার" },
                category = draftCategory.value,
                imageUrl = getRandomNewsImageUrl(draftCategory.value),
                sentiment = sentimentOutput.value.ifEmpty { "Neutral" },
                seoKeywords = seoKeywordsOutput.value,
                seoScore = if (seoScoreOutput.value == 0) 55 else seoScoreOutput.value,
                isPublished = true,
                views = kotlin.random.Random.nextInt(100, 401),
                likes = kotlin.random.Random.nextInt(10, 41),
                shares = kotlin.random.Random.nextInt(2, 13)
            )

            repository.saveArticle(newArticle)
            repository.insertNotification(
                "নিবন্ধ প্রকাশিত হয়েছে!",
                "সাংবাদিক ${newArticle.author} সাফল্যের সাথে '${newArticle.title}' প্রকাশ করেছেন।"
            )

            // Clear inputs
            draftTitle.value = ""
            draftContent.value = ""
            seoKeywordsOutput.value = ""
            seoScoreOutput.value = 0
            sentimentOutput.value = ""
        }
    }

    // Save News as a Private Pending review draft
    fun saveDraftArticleOnly() {
        viewModelScope.launch {
            val title = draftTitle.value.trim()
            val text = draftContent.value.trim()
            if (title.isEmpty() || text.isEmpty()) return@launch

            val newArticle = Article(
                title = title,
                content = text,
                author = draftAuthor.value.trim().ifEmpty { "স্টাফ রিপোর্টার" },
                category = draftCategory.value,
                imageUrl = getRandomNewsImageUrl(draftCategory.value),
                sentiment = sentimentOutput.value.ifEmpty { "Neutral" },
                seoKeywords = seoKeywordsOutput.value,
                isPublished = false,
                seoScore = if (seoScoreOutput.value == 0) 50 else seoScoreOutput.value
            )

            repository.saveArticle(newArticle)
            repository.insertNotification(
                "খসড়া সংরক্ষিত হয়েছে",
                "নতুন প্রতিবেদন '${newArticle.title}' খসড়া হিসেবে যুক্ত করা হয়েছে।"
            )
            
            draftTitle.value = ""
            draftContent.value = ""
            seoKeywordsOutput.value = ""
            seoScoreOutput.value = 0
            sentimentOutput.value = ""
        }
    }

    // Gemini Feature: Style Rewrite optimization
    fun triggerAiRewrite(styleName: String) {
        viewModelScope.launch {
            val title = draftTitle.value
            val content = draftContent.value
            if (title.isEmpty() || content.isEmpty()) return@launch

            aiRewriting.value = true
            try {
                val optimized = GeminiClient.rewriteArticle(title, content, styleName)
                draftTitle.value = optimized.first
                draftContent.value = optimized.second
                // automatically review and recalculate SEO parameters on optimization changes
                triggerSeoAuditOffline()
            } catch (e: Exception) {
                // error handling
            } finally {
                aiRewriting.value = false
            }
        }
    }

    // Gemini Feature: Sentiment Classification audit
    fun triggerSentimentCheck() {
        viewModelScope.launch {
            val title = draftTitle.value
            val content = draftContent.value
            if (title.isEmpty() || content.isEmpty()) return@launch

            sentimentLoading.value = true
            try {
                val outcome = GeminiClient.analyzeSentiment(title, content.take(300))
                sentimentOutput.value = outcome
            } catch (e: Exception) {
                sentimentOutput.value = "Neutral"
            } finally {
                sentimentLoading.value = false
            }
        }
    }

    // Gemini Feature: SEO Analysis Check & Keyword synthesis
    fun triggerSeoAuditOffline() {
        viewModelScope.launch {
            val title = draftTitle.value
            val content = draftContent.value
            if (title.isEmpty() || content.isEmpty()) return@launch

            seoLoading.value = true
            try {
                val outcome = GeminiClient.analyzeSEO(title, content)
                seoScoreOutput.value = outcome.first
                seoKeywordsOutput.value = outcome.second
            } catch (e: Exception) {
                seoScoreOutput.value = 60
                seoKeywordsOutput.value = "আঞ্চলিক খবর, দৈনিক জাহান"
            } finally {
                seoLoading.value = false
            }
        }
    }

    // Engagement helper: increments views, likes, shares locally
    fun handleArticleInteraction(article: Article, type: String) {
        viewModelScope.launch {
            val updated = when (type) {
                "like" -> article.copy(likes = article.likes + 1)
                "share" -> article.copy(shares = article.shares + 1)
                else -> article.copy(views = article.views + 1)
            }
            repository.saveArticle(updated)
            
            // Add a dynamic performance metric bump when users interact with the app, generating live graphs!
            if (type == "share") {
                val currentHourStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                repository.insertMetric(
                    AnalyticsMetric(metricCategory = "Social engagements", label = currentHourStr, value = 4320f + (10..50).random())
                )
            }
        }
    }

    // Auto-Scheduler Social queuing flow (cross-platform 1-click execution)
    fun scheduleSocialSharingQueue(article: Article, fb: Boolean, tw: Boolean, li: Boolean) {
        viewModelScope.launch {
            val selectedPlatforms = mutableListOf<String>()
            if (fb) selectedPlatforms.add("Facebook")
            if (tw) selectedPlatforms.add("Twitter")
            if (li) selectedPlatforms.add("LinkedIn")

            if (selectedPlatforms.isEmpty()) return@launch

            val platformsList = selectedPlatforms.joinToString(", ")
            val hashtags = article.seoKeywords.split(",")
                .filter { it.isNotBlank() }
                .take(3)
                .joinToString(" ") { "#" + it.trim().replace(" ", "") }

            val optimizedSocialCopy = "【তাজা খবর • দৈনিক জাহান】\n\n${article.title}\n\nপড়ুন বিস্তারিত এখানে: www.dainikjahan.com/article/${article.id}\n\n$hashtags #DainikJahan"

            val schedulePost = SocialQueuePost(
                articleId = article.id,
                articleTitle = article.title,
                platforms = platformsList,
                optimizedContent = optimizedSocialCopy,
                status = "Pending"
            )

            repository.scheduleSocialPost(schedulePost)
            repository.insertNotification(
                "সামাজিক প্রচারণায় যুক্ত হয়েছে",
                "নিবন্ধ '${article.title}' $platformsList এর প্রকাশনা শিডিউলে যুক্ত হয়েছে।"
            )
        }
    }

    // Publish post instant
    fun publishSocialPostNow(post: SocialQueuePost) {
        viewModelScope.launch {
            val updated = post.copy(status = "Published")
            repository.updateSocialPost(updated)
            
            // Log analytics increase
            val labelStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            repository.insertMetric(
                AnalyticsMetric(
                    metricCategory = "Social engagements",
                    label = labelStr,
                    value = kotlin.random.Random.nextDouble(3500.0, 5000.0).toFloat()
                )
            )

            repository.insertNotification(
                "সোশ্যাল প্রচারণা সফল!",
                "পোস্টটি সফলভাবে ${post.platforms} প্ল্যাটফর্মে প্রকাশ করা হয়েছে।"
            )
        }
    }

    fun removeScheduledPost(post: SocialQueuePost) {
        viewModelScope.launch {
            repository.deleteSocialPost(post)
        }
    }

    // Simulated journalist dynamic emails update & notification dispatcher
    fun simulateRealtimeEditorialBroadcast() {
        viewModelScope.launch {
            val titles = listOf(
                "সাভারে নতুন ট্যানারি বর্জ্য শোধনাগার প্রকল্প অনুমোদন দিল পরিবেশ অধিদপ্তর",
                "আইটি খাতে যুব সমাজকে দক্ষ করতে জাতীয় সেমিনারের উদ্বোধন ঢাকার সফটওয়্যার পার্কে",
                "খাদ্য উৎপাদনে স্বয়ংসম্পূর্ণতা বজায় রাখতে কৃষকদের ১০ কোটি টাকা বিশেষ প্রণোদনা ঘোষণা",
                "বঙ্গোপসাগরে সৃষ্ট মৃদু লঘুচাপের কারণে ৪ নম্বর স্থানীয় সতর্ক সংকেত জারি করেছে আবহাওয়া অফিস",
                "রংপুরে তীব্র শৈত্যপ্রবাহে বিপন্ন ছিন্নমূল মানুষের জন্য উষ্ণ ও শুকনো খাবার সহায়তা"
            )
            val authors = listOf("ফারুক পাঠান", "রুমা খাতুন", "জাহিদ হাসান", "আমানুল্লাহ খান", "নাদিয়া চৌধুরী")
            val categories = listOf("Politics", "Tech", "Economy", "International", "Bangladesh")
            val contents = listOf(
                "সাভার শিল্পাঞ্চলের চামড়া কারখানার বিষাক্ত তরল বর্জ্য শোধনে সরকারি অর্থায়নে বসানো হবে পরিবেশবান্ধব রোবোটিক ফিল্টার প্রকল্প। এতে স্থানীয় নদীগুলোর দূষণ ক্রমান্বয়ে শূন্যে নেমে আসবে বলে নিশ্চিত করেছেন পরিবেশ কমিশনার।",
                "তথ্যপ্রযুক্তি ক্ষেত্রে আগামী ৩ বছরে দেশের ১ লাখ তরুণ-তরুণীকে এডভান্সড মেশিন লার্নিং সামর্থ্য গড়ে তুলতে আন্তর্জাতিক কনসোর্টিয়াম সই হয়েছে। এতে প্রযুক্তি শিক্ষার অবকাঠামো বিস্তৃতি পাবে।",
                "কৃষি মন্ত্রণালয়ের বিশেষ প্রজ্ঞাপন অনুসারে প্রান্তিক কৃষকদের বিনা সুদে ও ভুর্তকি মূল্যে সার এবং উচ্চ ফলনশীল বীজ সরবরাহ করবে শস্য ফান্ড। এতে চালের ও গমের উৎপাদন আরো বৃদ্ধি পাওয়ার সম্ভাবনা দেখা দিয়েছে।",
                "আসন্ন মৌসুমী ঝড়ের পূর্বাভাস দিয়ে আবহাওয়া অধিদপ্তর জানিয়েছে, দক্ষিণ-পূর্ব বঙ্গোপসাগরের গভীর নিম্নচাপটি শক্তিশালী সাইক্লোনে রূপ নিতে পারে। উপকূলীয় জেলেদের দ্রুত নিরাপদ আশ্রয়ে ফেরার নির্দেশ দেওয়া হয়েছে।",
                "উত্তরবঙ্গের সীমান্ত জেলাগুলোতে হিমশীতল বাতাসের কারণে বিপর্যস্ত জনজীবন। স্বেচ্ছাসেবীদের উদ্যোগে আজ সকালে শীতার্তদের মাঝে বিনামূল্যে ৫ হাজার কম্বল ও গরম সুপ বিতরণ ক্যাম্পেইন শুরু হয়।"
            )

            val randomIndex = kotlin.random.Random.nextInt(0, 5)
            val simulatedArticle = Article(
                title = titles[randomIndex],
                content = contents[randomIndex],
                author = authors[randomIndex],
                category = categories[randomIndex],
                imageUrl = getRandomNewsImageUrl(categories[randomIndex]),
                sentiment = "Neutral",
                isPublished = false, // starts in draft for journalists workspace review
                seoScore = kotlin.random.Random.nextInt(50, 81)
            )

            // Save as draft
            val draftId = repository.saveArticle(simulatedArticle)
            
            // Add notification alert
            repository.insertNotification(
                "📬 নতুন সাংবাদিক আপডেট প্রাপ্তি",
                "সাংবাদিক ${simulatedArticle.author} থেকে খসড়া নিবন্ধ '${simulatedArticle.title}' পর্যালোচনার জন্য জমা পড়েছে।"
            )
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    // Dynamic metrics builder for simulation
    fun triggerDynamicEngagementGrowth() {
        viewModelScope.launch {
            val labelStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            
            // Add a bump to page views
            repository.insertMetric(
                AnalyticsMetric(
                    metricCategory = "Daily traffic",
                    label = labelStr,
                    value = kotlin.random.Random.nextDouble(75000.0, 95000.0).toFloat()
                )
            )

            // Add a bump to CTR
            repository.insertMetric(
                AnalyticsMetric(
                    metricCategory = "CTR growth",
                    label = labelStr,
                    value = kotlin.random.Random.nextDouble(5.5, 7.2).toFloat()
                )
            )

            repository.insertNotification(
                "📈 ক্যাম্পেইনের অর্গানিক বৃদ্ধি চিহ্নিত",
                "এআই এডিটরের উন্নত এবং কীওয়ার্ড-অপ্টিমাইজড আর্টিকেলের মাধ্যমে দৈনিক ট্রাফিক রেট ৮৫,০০০ ছাড়িয়ে নতুন মাইলফলক ছুঁয়েছে!"
            )
        }
    }

    // Gmail connection functions
    fun connectGmail(token: String) {
        viewModelScope.launch {
            gmailAccessToken.value = token
            isGmailConnected.value = true
            fetchGmailEmails()
        }
    }

    fun disconnectGmail() {
        gmailAccessToken.value = ""
        isGmailConnected.value = false
        gmailMails.value = emptyList()
    }

    fun fetchGmailEmails() {
        viewModelScope.launch {
            isFetchingGmail.value = true
            try {
                val mails = GmailClient.fetchLastEmails(gmailAccessToken.value)
                gmailMails.value = mails
            } catch (e: Exception) {
                gmailMails.value = emptyList()
            } finally {
                isFetchingGmail.value = false
            }
        }
    }

    fun importGmailAsReviewDraft(mail: GmailMailDetail) {
        viewModelScope.launch {
            val title = mail.subject.trim()
            val text = mail.textBody.trim()
            if (title.isEmpty() || text.isEmpty()) return@launch

            // Auto-detect category or keep Bangladesh as default
            val detectedCategory = when {
                title.lowercase().contains("tech") || title.contains("প্রযুক্তি") || title.contains("আইটি") -> "Tech"
                title.lowercase().contains("sports") || title.contains("ক্রীড়া") || title.contains("খেলার") || title.contains("জয়") -> "Sports"
                title.lowercase().contains("economy") || title.contains("অর্থনীতি") || title.contains("বাজেট") || title.contains("টাকা") || title.contains("রপ্তানি") -> "Economy"
                title.lowercase().contains("politics") || title.contains("পরিবেশ") || title.contains("প্রজ্ঞাপন") -> "Politics"
                else -> "Bangladesh"
            }

            val cleaningAuthor = mail.from.replace("<", "[").replace(">", "]")

            val newArticle = Article(
                title = title,
                content = text,
                author = cleaningAuthor,
                category = detectedCategory,
                imageUrl = getRandomNewsImageUrl(detectedCategory),
                sentiment = "Neutral",
                seoKeywords = "জিমেইল আমদানি, দৈনিক জাহান, খবর",
                isPublished = false, // starts in draft for journalists review
                seoScore = 50,
                views = 0,
                likes = 0,
                shares = 0
            )

            repository.saveArticle(newArticle)
            repository.insertNotification(
                "📬 জিমেইল খসড়া আমদানি সফল",
                "ইমেইল '${mail.subject}' জিমেইল একাউন্ট dainikjahan@gmail.com থেকে খসড়া হিসেবে সফলভাবে আমদানি করা হয়েছে।"
            )

            // Auto load subject & body into inputs for immediate AI assistance editing
            draftTitle.value = title
            draftContent.value = text
            draftCategory.value = detectedCategory
            draftAuthor.value = cleaningAuthor
            
            // Auto run SEO Audit so they have live SEO recommendations instantly
            triggerSeoAuditOffline()
        }
    }

    // Helpers
    private fun getRandomNewsImageUrl(category: String): String {
        return when (category) {
            "Politics" -> "https://images.unsplash.com/photo-1529107386315-e1a2ed48a620?auto=format&fit=crop&q=80&w=400"
            "Economy" -> "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?auto=format&fit=crop&q=80&w=400"
            "Sports" -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&q=80&w=400"
            "Tech" -> "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&q=80&w=400"
            "International" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=400"
            else -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&q=80&w=400"
        }
    }
}
