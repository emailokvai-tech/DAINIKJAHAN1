package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NewsRepository(private val database: AppDatabase) {

    val articles: Flow<List<Article>> = database.articleDao().getAllArticles()
    val publishedArticles: Flow<List<Article>> = database.articleDao().getPublishedArticles()
    val trendingArticles: Flow<List<Article>> = database.articleDao().getTrendingArticles()
    val socialPosts: Flow<List<SocialQueuePost>> = database.socialPostDao().getAllSocialPosts()
    val analyticsMetrics: Flow<List<AnalyticsMetric>> = database.analyticsDao().getAllMetrics()
    val notifications: Flow<List<NotificationRecord>> = database.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = database.notificationDao().getUnreadCount()

    suspend fun getArticleById(id: Int): Article? = database.articleDao().getArticleById(id)

    suspend fun saveArticle(article: Article): Long = database.articleDao().insertArticle(article)

    suspend fun updateArticle(article: Article) = database.articleDao().updateArticle(article)

    suspend fun deleteArticle(article: Article) = database.articleDao().deleteArticle(article)

    suspend fun scheduleSocialPost(post: SocialQueuePost) = database.socialPostDao().insertSocialPost(post)

    suspend fun updateSocialPost(post: SocialQueuePost) = database.socialPostDao().updateSocialPost(post)

    suspend fun deleteSocialPost(post: SocialQueuePost) = database.socialPostDao().deleteSocialPost(post)

    suspend fun insertMetric(metric: AnalyticsMetric) = database.analyticsDao().insertMetric(metric)

    suspend fun insertNotification(title: String, body: String) {
        database.notificationDao().insertNotification(
            NotificationRecord(title = title, body = body)
        )
    }

    suspend fun markNotificationsRead() = database.notificationDao().markAllAsRead()

    suspend fun clearNotifications() = database.notificationDao().clearAllNotifications()

    suspend fun populateInitialDataIfEmpty() {
        // If no articles exist, populate initial high-fidelity mock data for Dainik Jahan
        val existingArticles = articles.first()
        if (existingArticles.isEmpty()) {
            val defaultArticles = listOf(
                Article(
                    title = "শতবর্ষে পা রাখল ঢাকা রিপোর্টার্স ইউনিটি, দেশের সংকট কাটিয়ে এগিয়ে যাওয়ার অঙ্গীকার",
                    content = "জাতীয় স্বার্থ ও গণতন্ত্র সমুন্নত রাখতে সাংবাদিকদের লেখনীর তাগিদ দিয়েছেন জ্যেষ্ঠ সাংবাদিকরা। আজ এক সেমিনারে সাংবাদিক কল্যাণ ও পেশাদারিত্বে সমন্বয়ের আহ্বান জানানো হয়। ঢাকা রিপোর্টার্স ইউনিটি (ডিআরইউ) তাদের শতবর্ষী দীর্ঘ ইতিহাস স্মরণ করে এক গৌরবময় উৎসবের মধ্য দিয়ে দিনটি অতিবাহিত করে। এতে বাংলাদেশ ও বিশ্বের সাম্প্রতিক রাজনৈতিক পরিস্থিতি নিয়ে দীর্ঘ আলোচনা হয়।",
                    author = "আবদুল মান্নান",
                    category = "Bangladesh",
                    imageUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&q=80&w=400",
                    sentiment = "Positive",
                    seoKeywords = "ডিআরইউ, সাংবাদিকতা, শতবর্ষ উৎসব, ঢাকা রিপোর্টার্স ইউনিটি",
                    seoScore = 84,
                    isPublished = true,
                    isTrending = true,
                    views = 4580,
                    likes = 342,
                    shares = 189
                ),
                Article(
                    title = "রেকর্ড গড়ল তৈরি পোশাক রপ্তানি, নতুন বাজার অনুসন্ধানে উদ্যোক্তাদের জোর তাগিদ",
                    content = "চলতি অর্থবছরে ইউরোপ এবং মধ্য প্রাচ্যের অপ্রচলিত বাজারে রেকর্ড রপ্তানি করেছে বাংলাদেশের তৈরি পোশাক শিল্প প্রতিষ্ঠানগুলো। বিশেষ করে নতুন পরিবেশবান্ধব গ্রিন ফ্যাক্টরি স্থাপনের মাধ্যমে পরিবেশ সচেতন ক্রেতাদের আকর্ষণ করা সম্ভব হয়েছে। বিজিএমইএ সভাপতি বলেন, গুণগত মান এবং সময়মতো পণ্য সরবরাহ করায় বিশ্বের ক্রেতারা এখনও বাংলাদেশি পণ্যকে প্রাধান্য দিচ্ছেন। তবে সংকট এড়াতে কাঁচামাল আমদানিতে স্থানীয় উৎপাদন উন্নত করার তাগিদ রয়েছে।",
                    author = "তাসনিম জাহান",
                    category = "Economy",
                    imageUrl = "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&q=80&w=400",
                    sentiment = "Positive",
                    seoKeywords = "তৈরি পোশাক, বিজিএমইএ, বাংলাদেশ পোশাক শিল্প, রপ্তানি রেকর্ড",
                    seoScore = 92,
                    isPublished = true,
                    isTrending = true,
                    views = 8240,
                    likes = 721,
                    shares = 312
                ),
                Article(
                    title = "গ্রিনসিটি হিসেবে গড়ে উঠছে রাজশাহী, পরিবেশবান্ধব উদ্যোগের প্রশংসায় বিশ্ব সম্প্রদায়",
                    content = "রাজশাহী সিটি কর্পোরেশনের দীর্ঘমেয়াদী বনায়ন এবং শব্দ দূষণ প্রতিরোধমূলক কাজের সুফল পেতে শুরু করেছে নগরবাসী। শহরের বুক চিরে বয়ে যাওয়া পদ্মা নদীর তীর রক্ষা প্রকল্প এবং বৃক্ষরোপণের ফলেই এটি বাংলাদেশের অন্যতম বাসযোগ্য সবুজ নগরে পরিণত হয়েছে। জাতিসংঘের পরিবেশ বিষয়ক প্রতিনিধি দল সম্প্রতি রাজশাহী সফর করে এই ইতিবাচক পরিবর্তনকে অনুকরণীয় সবুজ মডেল হিসেবে আখ্যায়িত করেন।",
                    author = "ফারহান আহমেদ",
                    category = "Bangladesh",
                    imageUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&q=80&w=400",
                    sentiment = "Positive",
                    seoKeywords = "রাজশাহী গ্রিনসিটি, পরিবেশবান্ধব নগর, পদ্মা নদী প্রকল্প, ক্লিন সিটি",
                    seoScore = 78,
                    isPublished = true,
                    isTrending = false,
                    views = 2890,
                    likes = 455,
                    shares = 98
                ),
                Article(
                    title = "আন্তর্জাতিক ক্রিকেট মঞ্চে বাংলাদেশের গৌরবোজ্জ্বল জয়, শেষ ওভারের রোমাঞ্চ",
                    content = "চিরপ্রতিদ্বন্দ্বী দলের বিপক্ষে শেষ বলের স্কয়ার কাটে বাউন্ডারি মেরে রোমাঞ্চকর জয় উপহার দিল বাংলাদেশ জাতীয় ক্রিকেট দল। অসাধারণ বোলিং স্পেলে শুরুতে ম্যাচের নিয়ন্ত্রণ ধরে রেখে এবং শেষ দিকে ঠান্ডা মাথার ব্যাটিংয়ে ঐতিহাসিক এই সিরিজ নিজেদের করে নিলো তারা। অলরাউন্ডার সাকিবের অনবদ্য ১২৫ রান ও তিন উইকেটে তিনি ম্যাচ সেরার পুরস্কার অর্জন করেছেন। দর্শক গ্যালারিতে লাল-সবুজের উল্লাস আজ আকাশছোঁয়া।",
                    author = "রকিব হাসান",
                    category = "Sports",
                    imageUrl = "https://images.unsplash.com/photo-1540747737956-378724044592?auto=format&fit=crop&q=80&w=400",
                    sentiment = "Positive",
                    seoKeywords = "ক্রিকেট জয়, সাকিব আল হাসান, বাংলাদেশ অলরাউন্ডার, লাল-সবুজ ক্রিকেট",
                    seoScore = 88,
                    isPublished = true,
                    isTrending = true,
                    views = 12050,
                    likes = 2390,
                    shares = 1450
                ),
                Article(
                    title = "জলবায়ু ফান্ডের সঠিক বাস্তবায়নে স্বচ্ছতার নির্দেশ প্রধানমন্ত্রীর",
                    content = "বিশ্বব্যাপী উষ্ণতার কারণে ঝুঁকিপূর্ণ বাংলাদেশের উপকূলবর্তী জেলাগুলোর সুরক্ষায় বরাদ্দ করা জলবায়ু ফান্ড ব্যবহারে বিন্দুমাত্র অনিয়ম বরদাশত করা হবে না বলে কড়া নির্দেশনা দেওয়া হয়েছে। বাঁধ নির্মাণ ও বন্যা আশ্রয় কেন্দ্র তৈরিতে প্রতিটি প্রকল্পের ব্যয় নিবিড়ভাবে নিরীক্ষণ করার ঘোষণা দিয়েছেন পরিবেশ মন্ত্রী। দুর্নীতি দমন বিভাগকে কাজের অগ্রগতির সার্বক্ষণিক স্বচ্ছ ডিজিটাল অডিট বজায় রাখার নির্দেশ দেওয়া হয়েছে।",
                    author = "শামসুল আলম",
                    category = "Politics",
                    imageUrl = "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?auto=format&fit=crop&q=80&w=400",
                    sentiment = "Neutral",
                    seoKeywords = "জলবায়ু ফান্ড, স্বচ্ছতা নির্দেশ, পরিবেশ মন্ত্রণালয়, উপকূল বাঁধ প্রকল্প",
                    seoScore = 75,
                    isPublished = true,
                    isTrending = false,
                    views = 1900,
                    likes = 120,
                    shares = 45
                ),
                Article(
                    title = "কৃত্রিম বুদ্ধিমত্তা শিক্ষায় বিপ্লব ঘটাবে: ঢাকা আন্তর্জাতিক সেমিনারে বিজ্ঞানীদের মতামত",
                    content = "কৃত্রিম বুদ্ধিমত্তা শিক্ষাক্ষেত্রে অসমতা দূরীকরণে বিশাল ভূমিকা রাখবে বলে জানিয়েছেন দেশ-বিদেশের শীর্ষ গবেষকরা। ঢাকা বিশ্ববিদ্যালয়ের কার্জন হলে অনুষ্ঠিত এআই সায়েন্স সামিটে প্রযুক্তি শিক্ষাকে প্রত্যন্ত অঞ্চলে ছড়িয়ে দেওয়ার প্রস্তাব করা হয়। তবে এআই-এর প্রযুক্তির ব্যবহার নিয়ে নৈতিক সীমানা ও কপিরাইট নিয়ন্ত্রণের সঠিক ফ্রেমওয়ার্ক থাকার পক্ষেও জোর দেন বক্তারা। বিশেষ অতিথি হিসেবে তথ্যপ্রযুক্তি প্রতিমন্ত্রী বাংলায় এআই মডিউল উন্নয়ন জোরদার করার আশ্বাস দেন।",
                    author = "ডা. মেহরাব হোসেন",
                    category = "Tech",
                    imageUrl = "https://images.unsplash.com/photo-1677442136019-21780efad99a?auto=format&fit=crop&q=80&w=400",
                    sentiment = "Positive",
                    seoKeywords = "কৃত্রিম বুদ্ধিমত্তা শিক্ষা, কার্জন হল এআই সেমিনার, ঢাকা প্রযুক্তি সামিট",
                    seoScore = 91,
                    isPublished = false,
                    isTrending = false,
                    views = 0,
                    likes = 0,
                    shares = 0
                )
            )

            for (art in defaultArticles) {
                database.articleDao().insertArticle(art)
            }
        }

        // Populate initial historical statistics to make the analytics page highly illustrative
        val existingMetrics = analyticsMetrics.first()
        if (existingMetrics.isEmpty()) {
            val defaultMetrics = listOf(
                // Daily traffic (views)
                AnalyticsMetric(metricCategory = "Daily traffic", label = "মে ২৭", value = 42000f),
                AnalyticsMetric(metricCategory = "Daily traffic", label = "মে ২৮", value = 48000f),
                AnalyticsMetric(metricCategory = "Daily traffic", label = "মে ২৯", value = 45000f),
                AnalyticsMetric(metricCategory = "Daily traffic", label = "মে ৩০", value = 59000f),
                AnalyticsMetric(metricCategory = "Daily traffic", label = "মে ৩১", value = 64000f),
                AnalyticsMetric(metricCategory = "Daily traffic", label = "জুন ০১", value = 71000f),
                AnalyticsMetric(metricCategory = "Daily traffic", label = "জুন ০২", value = 85400f),

                // CTR growth (%)
                AnalyticsMetric(metricCategory = "CTR growth", label = "মে ২৭", value = 2.8f),
                AnalyticsMetric(metricCategory = "CTR growth", label = "মে ২৮", value = 3.2f),
                AnalyticsMetric(metricCategory = "CTR growth", label = "মে ২৯", value = 3.1f),
                AnalyticsMetric(metricCategory = "CTR growth", label = "মে ৩০", value = 4.0f),
                AnalyticsMetric(metricCategory = "CTR growth", label = "মে ৩১", value = 3.9f),
                AnalyticsMetric(metricCategory = "CTR growth", label = "জুন ০১", value = 4.8f),
                AnalyticsMetric(metricCategory = "CTR growth", label = "জুন ০২", value = 5.4f),

                // Social engagements
                AnalyticsMetric(metricCategory = "Social engagements", label = "মে ২৭", value = 800f),
                AnalyticsMetric(metricCategory = "Social engagements", label = "মে ২৮", value = 1100f),
                AnalyticsMetric(metricCategory = "Social engagements", label = "মে ২৯", value = 950f),
                AnalyticsMetric(metricCategory = "Social engagements", label = "মে ৩০", value = 1800f),
                AnalyticsMetric(metricCategory = "Social engagements", label = "মে ৩১", value = 2200f),
                AnalyticsMetric(metricCategory = "Social engagements", label = "জুন ০১", value = 3100f),
                AnalyticsMetric(metricCategory = "Social engagements", label = "জুন ০২", value = 4320f)
            )
            database.analyticsDao().insertMetrics(defaultMetrics)
        }

        // Populate initial notification logs
        val existingNotifications = notifications.first()
        if (existingNotifications.isEmpty()) {
            database.notificationDao().insertNotification(
                NotificationRecord(
                    title = "দৈনিক জাহান নিউজ পোর্টালে স্বাগতম",
                    body = "সাংবাদিক ওয়ার্কফ্লো, এআই রাইটার, এসইও অ্যানালিটিক্স এবং রিয়েল-টাইম সোশাল প্রচারণার ড্যাশবোর্ডে আপনাকে অভিনন্দন!"
                )
            )
            database.notificationDao().insertNotification(
                NotificationRecord(
                    title = "🚨 ব্রেকিং নিউজ নোটিফিকেশন এলার্ট",
                    body = "ক্রিকেট সিরিজে শেষ বলের রোমাঞ্চকর জয়ের খবরটি ১-ক্লিকে ফেইসবুক ও টুইটারে ভাইরাল করা হয়েছে।"
                )
            )
        }
    }
}
