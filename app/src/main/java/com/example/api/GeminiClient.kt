package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Article
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Determine if we have a valid configured key
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    private suspend fun apiPost(prompt: String): String? = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            Log.w(TAG, "API Key is not configured. Falling back to local helper simulation.")
            return@withContext null
        }

        try {
            val key = BuildConfig.GEMINI_API_KEY
            val url = "$BASE_URL?key=$key"

            // Construct the exact Gemini JSON payload structure
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from Gemini: Code = ${response.code}, Msg = ${response.message}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val responseJson = JSONObject(bodyStr)
                
                // Extract parts[0].text safely
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                
                return@withContext firstPart?.optString("text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Gemini API request: ${e.message}", e)
            return@withContext null
        }
    }

    // Task 1: Sentiment Analysis on news content
    suspend fun analyzeSentiment(title: String, snippet: String): String {
        val prompt = """
            You are an expert news editor for Dainik Jahan. Analyze the overall tone and sentiment of this news article snippet.
            Respond with ONLY one of these four words of your choice: 'Positive', 'Neutral', 'Negative', 'Critical'.
            Do not write explanation, punctuation, or formatting.
            
            Title: $title
            Content: $snippet
        """.trimIndent()

        val responseText = apiPost(prompt)
        if (responseText != null) {
            val trimmed = responseText.trim().replace(".", "").replace("'", "")
            if (listOf("Positive", "Neutral", "Negative", "Critical").contains(trimmed)) {
                return trimmed
            }
        }
        
        // Logical local fallback simulation
        val textLower = (title + " " + snippet).lowercase()
        return when {
            textLower.contains("রেকর্ড") || textLower.contains("সাফল্য") || textLower.contains("জয়") -> "Positive"
            textLower.contains("দুর্নীতি") || textLower.contains("তদন্ত") || textLower.contains("অভিযোগ") -> "Critical"
            textLower.contains("সংকট") || textLower.contains("ক্ষতি") || textLower.contains("মারা") -> "Negative"
            else -> "Neutral"
        }
    }

    // Task 2: AI Rewrite (Style & Flow optimization)
    suspend fun rewriteArticle(title: String, content: String, style: String): Pair<String, String> {
        val prompt = """
            You are a senior Bengali rewriter at www.dainikjahan.com. Rewrite this Bengali news title and content to optimize it into a '$style' editorial tone (choices: 'Engaging', 'Formal Journalist', 'Dramatic/Urgent', 'SEO-Maximized').
            Keep the core facts, locations, and names unaltered.
            You must output your response in valid JSON format ONLY. Do not write markdown blocks or backticks.
            JSON schema:
            {
              "title": "Optimized headline string",
              "content": "Optimized body content paragraph string"
            }
            
            Original Title: $title
            Original Body: $content
        """.trimIndent()

        val responseText = apiPost(prompt)
        if (responseText != null) {
            try {
                // Clean up potential markdown formatting codeblocks enclosing JSON
                var cleaned = responseText.trim()
                if (cleaned.startsWith("```json")) {
                    cleaned = cleaned.substring(7)
                }
                if (cleaned.startsWith("```")) {
                    cleaned = cleaned.substring(3)
                }
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.substring(0, cleaned.length - 3)
                }
                cleaned = cleaned.trim()

                val obj = JSONObject(cleaned)
                val newTitle = obj.optString("title").trim()
                val newContent = obj.optString("content").trim()
                if (newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                    return Pair(newTitle, newContent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing JSON response from Gemini rewrite: ${e.message}")
            }
        }

        // Logical local fallback simulation
        val headingSuffix = when (style) {
            "Engaging" -> " [বিশদ আলোচনা ও প্রতিবেদন]"
            "Dramatic/Urgent" -> " [জরুরি খবর: বিশেষ বুলেটিন]"
            "SEO-Maximized" -> " - আজকের তাজা খবর"
            else -> " [সম্পাদিত সংস্করণ]"
        }
        val contentPrefix = when (style) {
            "Engaging" -> "বাংলাদেশ ও আন্তর্জাতিক বিশেষজ্ঞদের মতে, "
            "Dramatic/Urgent" -> "বিশেষ সুত্রে প্রাপ্ত শেষ খবর অনুযায়ী, "
            "SEO-Maximized" -> "ঢাকা থেকে আমাদের বিশেষ পাতায় বিস্তারিত, "
            else -> "দৈনিক জাহানের সম্পাদকীয় ডেস্ক জানায়: "
        }
        return Pair(title + headingSuffix, contentPrefix + content)
    }

    // Task 3: SEO Tool - rating and 5 key SEO tags
    suspend fun analyzeSEO(title: String, content: String): Pair<Int, String> {
        val prompt = """
            You are an SEO expert for a high-traffic news website. Analyze the SEO keywords, density, and readability of the following news post.
            Output exactly a JSON object, no other words, no markdown backticks representation.
            JSON structure:
            {
              "score": 85,
              "keywords": "keyword1, keyword2, keyword3, keyword4, keyword5"
            }
            The score must be an integer between 40 and 100 assessing general readability and headline appeal. Keywords should be relevant.
            
            Title: $title
            Content: $content
        """.trimIndent()

        val responseText = apiPost(prompt)
        if (responseText != null) {
            try {
                var cleaned = responseText.trim()
                if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7)
                if (cleaned.startsWith("```")) cleaned = cleaned.substring(3)
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3)
                cleaned = cleaned.trim()

                val obj = JSONObject(cleaned)
                val score = obj.optInt("score", 70)
                val keywords = obj.optString("keywords", "")
                if (keywords.isNotEmpty()) {
                    return Pair(score, keywords)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing JSON for SEO audit: ${e.message}")
            }
        }

        // Logical local fallback simulation
        val score = (50 + (title.length + content.length) % 45)
        val cleanTerms = title.split(" ", "।", ",", "-")
            .filter { it.length > 3 }
            .distinct()
            .take(4)
            .joinToString(", ")
        val defaultKeywords = "$cleanTerms, দৈনিক জাহান খবর, তাজা খবর"
        return Pair(score, defaultKeywords)
    }

    // Task 4: Trend Spotting
    suspend fun spotTrends(): List<TrendItem> {
        val prompt = """
            Identify three major trending topics currently active or emerging in Bangladesh today (topics could be climate change, digital transformation, clothing sector, sports tournaments, food price levels).
            Respond with ONLY a raw JSON array of objects. Do not write markdown blocks or explain.
            JSON Schema:
            [
              {
                "topic": "নামকরণ (যেমন: রাজশাহী বনায়ন বিপ্লব)",
                "volume": "উচ্চ / ভাইরাল / স্বাভাবিক",
                "reportersGuide": "সংক্ষিপ্ত নির্দেশনা সাংবাদিকের সুবিধার জন্য"
              }
            ]
        """.trimIndent()

        val responseText = apiPost(prompt)
        if (responseText != null) {
            try {
                var cleaned = responseText.trim()
                if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7)
                if (cleaned.startsWith("```")) cleaned = cleaned.substring(3)
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3)
                cleaned = cleaned.trim()

                val array = JSONArray(cleaned)
                val list = mutableListOf<TrendItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TrendItem(
                            title = obj.optString("topic", "অজ্ঞাত ট্রেন্ড"),
                            category = obj.optString("volume", "উচ্চ"),
                            recommendation = obj.optString("reportersGuide", "এই বিষয়ে কভারেজ তৈরি করুন")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing JSON for trend spotting: ${e.message}")
            }
        }

        // Dynamic, high-quality local fallback simulation
        return listOf(
            TrendItem("সবুজ নগরায়ন ও জলবায়ু সহনশীলতা", "ভাইরাল", "উপকূলীয় পরিবেশবান্ধব জীবনযাত্রা ও বাঁধ পুনর্নির্মাণে বনায়ন নিয়ে নিবন্ধ প্রকাশ করুন।"),
            TrendItem("ডিজিটাল পেমেন্ট ও অনলাইন কর প্রদান", "উচ্চ", "দেশের নতুন কর রিটার্ন ফাইলিং ও ব্যাংকিং ক্যাশলেস ক্যাম্পেইন নিয়ে নাগরিক মতামত সংগ্রহ করুন।"),
            TrendItem("আন্তর্জাতিক ফুটবল ও সাকিবের নতুন একাডেমি", "স্বাভাবিক", "নতুন উদীয়মান ক্রিকেটার ও ফুটবল খেলোয়াড়দের প্রশিক্ষণ কোর্সের সুযোগ নিয়ে ফিচার লিখুন।")
        )
    }

    // Recommendation engine (matches user interests based on a set of preferred topics)
    fun getPersonalizedRecommendations(articles: List<Article>, preferredCategory: String): List<Article> {
        if (preferredCategory == "All") return articles.shuffled().take(4)
        
        val matched = articles.filter { it.category.equals(preferredCategory, ignoreCase = true) }
        val remaining = articles.filter { !it.category.equals(preferredCategory, ignoreCase = true) }
        
        return (matched + remaining.shuffled()).take(4)
    }
}

data class TrendItem(
    val title: String,
    val category: String,
    val recommendation: String
)
