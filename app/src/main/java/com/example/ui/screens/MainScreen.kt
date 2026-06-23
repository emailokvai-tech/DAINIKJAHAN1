package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.api.TrendItem
import com.example.api.GeminiClient
import com.example.data.model.Article
import androidx.compose.ui.text.TextStyle
import com.example.data.model.SocialQueuePost
import com.example.data.model.AnalyticsMetric
import com.example.data.model.NotificationRecord
import com.example.ui.NewsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun BoxStroke(width: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: NewsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Collecting states from the ViewModel
    val articles by viewModel.articles.collectAsStateWithLifecycle(emptyList())
    val publishedArticles by viewModel.publishedArticles.collectAsStateWithLifecycle(emptyList())
    val socialPosts by viewModel.socialPosts.collectAsStateWithLifecycle(emptyList())
    val analyticsMetrics by viewModel.analyticsMetrics.collectAsStateWithLifecycle(emptyList())
    val notifications by viewModel.notifications.collectAsStateWithLifecycle(emptyList())
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle(0)
    val activeTrends by viewModel.activeTrends.collectAsStateWithLifecycle(emptyList())
    
    // UI Local State
    var currentTab by remember { mutableStateOf("portal") } // "portal", "editor", "social", "dashboard"
    val selectedArticle by viewModel.selectedArticle.collectAsStateWithLifecycle()
    val selectedCategoryTab by viewModel.selectedCategoryTab.collectAsStateWithLifecycle()
    
    var showNotificationBottomSheet by remember { mutableStateOf(false) }
    
    // Color schemes matching Emerald-Cream dynamic palette
    val headerBg = Brush.linearGradient(
        colors = listOf(Color(0xFF003024), Color(0xFF004d3d))
    )
    val cardGrad = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFCFDF9), Color(0xFFF7F9F5))
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF005FAF), shape = RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "D",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Dainik Jahan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B1B1F),
                                        letterSpacing = 0.5.sp
                                    ),
                                    modifier = Modifier.testTag("app_identity_title")
                                )
                                Text(
                                    "Journalist Portal".uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF565E71),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        // Simulates Incoming Journalist updates
                        IconButton(
                            onClick = {
                                viewModel.simulateRealtimeEditorialBroadcast()
                                Toast.makeText(context, "সাংবাদিক থেকে ইমেল আপডেট জমা পড়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(36.dp)
                                .background(Color(0xFFEDF1FF), shape = CircleShape)
                                .testTag("incoming_simulate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = "Simulate Dispatch",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Alert Center
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .background(Color(0xFFEDF1FF), shape = CircleShape)
                                .clickable { showNotificationBottomSheet = true }
                                .testTag("notification_center_bell"),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(containerColor = Color(0xFFBA1A1A)) {
                                            Text(unreadNotificationsCount.toString(), color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Alerts",
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                HorizontalDivider(color = Color(0xFFDDE2F0), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0xFFDDE2F0), thickness = 1.dp)
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentTab == "portal",
                        onClick = {
                            currentTab = "portal"
                            viewModel.selectedArticle.value = null
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "News Portal") },
                        label = { Text("পোর্টাল ফিড", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF005FAF),
                            unselectedIconColor = Color(0xFF44474F),
                            selectedTextColor = Color(0xFF005FAF),
                            unselectedTextColor = Color(0xFF44474F),
                            indicatorColor = Color(0xFFD3E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_portal")
                    )
                    NavigationBarItem(
                        selected = currentTab == "editor",
                        onClick = { currentTab = "editor" },
                        icon = { Icon(Icons.Filled.Edit, contentDescription = "AI Editor") },
                        label = { Text("সাংбавить ডেস্ক", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF005FAF),
                            unselectedIconColor = Color(0xFF44474F),
                            selectedTextColor = Color(0xFF005FAF),
                            unselectedTextColor = Color(0xFF44474F),
                            indicatorColor = Color(0xFFD3E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_editor")
                    )
                    NavigationBarItem(
                        selected = currentTab == "social",
                        onClick = { currentTab = "social" },
                        icon = { Icon(Icons.Filled.Share, contentDescription = "Social Queue") },
                        label = { Text("সামাজিক প্রচারণা", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF005FAF),
                            unselectedIconColor = Color(0xFF44474F),
                            selectedTextColor = Color(0xFF005FAF),
                            unselectedTextColor = Color(0xFF44474F),
                            indicatorColor = Color(0xFFD3E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_social")
                    )
                    NavigationBarItem(
                        selected = currentTab == "dashboard",
                        onClick = { currentTab = "dashboard" },
                        icon = { Icon(Icons.Filled.Menu, contentDescription = "Dashboard Analytics") },
                        label = { Text("পারফরম্যান্স", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF005FAF),
                            unselectedIconColor = Color(0xFF44474F),
                            selectedTextColor = Color(0xFF005FAF),
                            unselectedTextColor = Color(0xFF44474F),
                            indicatorColor = Color(0xFFD3E4FF)
                        ),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                }
            }
        }
    ) { innerPadding ->
        
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFFF7F9FF)
        ) {
            when (currentTab) {
                "portal" -> {
                    if (selectedArticle != null) {
                        ArticleDetailsScreen(
                            article = selectedArticle!!,
                            onBack = { viewModel.selectedArticle.value = null },
                            viewModel = viewModel
                        )
                    } else {
                        NewsPortalFeedScreen(
                            publishedArticles = publishedArticles,
                            selectedCategory = selectedCategoryTab,
                            onCategorySelect = { viewModel.selectedCategoryTab.value = it },
                            onArticleClick = {
                                viewModel.selectedArticle.value = it
                                viewModel.handleArticleInteraction(it, "view")
                            },
                            viewModel = viewModel
                        )
                    }
                }
                "editor" -> {
                    JournalistWorkspaceScreen(viewModel = viewModel)
                }
                "social" -> {
                    SocialQueueScreen(socialPosts = socialPosts, viewModel = viewModel)
                }
                "dashboard" -> {
                    CampaignDashboardScreen(
                        metrics = analyticsMetrics,
                        articles = articles,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Real-time alerts Bottom Sheet simulation
        if (showNotificationBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showNotificationBottomSheet = false
                    viewModel.markNotificationsRead()
                },
                containerColor = Color.White
            ) {
                NotificationCenterPanel(
                    notifications = notifications,
                    onClearAll = { viewModel.clearAllNotifications() },
                    onDismiss = {
                        viewModel.markNotificationsRead()
                        showNotificationBottomSheet = false
                    }
                )
            }
        }
    }
}

// ---------------- TAB 1: NEWS PORTAL HOME FEED ----------------

@Composable
fun NewsPortalFeedScreen(
    publishedArticles: List<Article>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onArticleClick: (Article) -> Unit,
    viewModel: NewsViewModel
) {
    val categories = listOf("All", "Bangladesh", "Economy", "Politics", "Sports", "Tech", "International")
    val filteredArticles = if (selectedCategory == "All") {
        publishedArticles
    } else {
        publishedArticles.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val preferredCategory by viewModel.userPreferredCategory.collectAsStateWithLifecycle()
    val recommendedList: List<Article> = remember(publishedArticles, preferredCategory) {
        GeminiClient.getPersonalizedRecommendations(publishedArticles, preferredCategory)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portal_feed_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // News ticker / banner title
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF1FF)),
                border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "তাজা কন্টেন্ট: রিয়েল-টাইম এআই রিরাইটার এবং সোশাল ডিস্ট্রিবিউশন কন্টেন্ট ডিস্ট্রিবিউশন সক্রিয়।",
                        fontSize = 12.sp,
                        color = Color(0xFF005FAF),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Personalization preferences anchor header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF005FAF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Gemini এআই রিকমেন্ডেশন ভিত্তিক পছন্দ",
                        color = Color(0xFFEDF1FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "আপনার পছন্দের সংবাদ ক্যাটাগরি সেট করুন, এআই রিকমিণ্ডেশন সিস্টেম সেই অনুযায়ী সংবাদ সাজাবে:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Bangladesh", "Economy", "Sports", "Tech").forEach { cat ->
                            val active = preferredCategory == cat
                            SuggestionChip(
                                onClick = { viewModel.userPreferredCategory.value = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (active) Color.White else Color.White.copy(alpha = 0.15f),
                                    labelColor = if (active) Color(0xFF005FAF) else Color.White
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Category Filter
        item {
            Text(
                "ক্যাটাগরি ভিত্তিক সংবাদ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1B1B1F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val active = selectedCategory == cat
                    Button(
                        onClick = { onCategorySelect(cat) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) Color(0xFF005FAF) else Color.White
                        ),
                        border = BoxStroke(1.dp, if (active) Color.Transparent else Color(0xFFDDE2F0)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (cat == "All") "সকল খবর" else cat,
                            color = if (active) Color.White else Color(0xFF44474F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // News articles feed
        if (filteredArticles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Empty Feed",
                            tint = Color(0xFFB0C5C0),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "এই ক্যাটাগরিতে কোনো নিবন্ধ নেই",
                            color = Color(0xFF6B8A83),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "সাংবাদিক ডেস্ক থেকে নতুন নিবন্ধ সাবমিট করুন।",
                            color = Color(0xFF8BAAA1),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredArticles) { article ->
                NewsArticleRowCard(
                    article = article,
                    onClick = { onArticleClick(article) }
                )
            }
        }

        // Personalized Articles Section
        if (recommendedList.isNotEmpty()) {
            item {
                HorizontalDivider(color = Color(0xFFDDE2F0), modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Gemini এআই ব্যক্তিগতকৃত পছন্দ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF1B1B1F)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "আপনার পছন্দের ক্যাটাগরি '$preferredCategory' এর উপর ভিত্তি করে স্বয়ংক্রিয় টিউনিং:",
                    color = Color(0xFF565E71),
                    fontSize = 12.sp
                )
            }

            items(recommendedList) { article ->
                Card(
                    onClick = { onArticleClick(article) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                    border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.title,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    article.category.uppercase(),
                                    color = Color(0xFF005FAF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Badge(containerColor = Color(0xFFEADDFF), contentColor = Color(0xFF21005D)) {
                                    Text("এআই সাজেশন্স", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                article.title,
                                color = Color(0xFF1B1B1F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "লেখক: ${article.author}",
                                color = Color(0xFF565E71),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleRowCard(article: Article, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("article_row_card_${article.id}")
    ) {
        Column {
            // Unsplash loaded news image
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            // Content body preview
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.category.uppercase(),
                        color = Color(0xFF005FAF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Published",
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "সংবাদ পাতা",
                            color = Color(0xFF005FAF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B1B1F),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Content snippet
                Text(
                    text = article.content,
                    fontSize = 13.sp,
                    color = Color(0xFF44474F),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE0E2EC))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "লিখেছেন: ${article.author}",
                        fontSize = 11.sp,
                        color = Color(0xFF565E71),
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Star, contentDescription = "Views", modifier = Modifier.size(15.dp), tint = Color(0xFF565E71))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(article.views.toString(), fontSize = 11.sp, color = Color(0xFF565E71))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ThumbUp, contentDescription = "Likes", modifier = Modifier.size(15.dp), tint = Color(0xFF565E71))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(article.likes.toString(), fontSize = 11.sp, color = Color(0xFF565E71))
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 1 SUBPANEL: ARTICLE DETAILS VIEW ----------------

@Composable
fun ArticleDetailsScreen(
    article: Article,
    onBack: () -> Unit,
    viewModel: NewsViewModel
) {
    val context = LocalContext.current
    var fbChecked by remember { mutableStateOf(true) }
    var twChecked by remember { mutableStateOf(true) }
    var liChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("article_details_pane")
    ) {
        // Back toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("details_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1B1B1F))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "নিবন্ধ বিস্তারিত",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF1B1B1F)
            )
        }

        AsyncImage(
            model = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF1FF)),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = article.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FAF)
                    )
                }

                // AI Tone & Sentiment metadata indicator
                val toneColor = when (article.sentiment) {
                    "Positive" -> Color(0xFF005FAF)
                    "Critical" -> Color(0xFFBA1A1A)
                    "Negative" -> Color(0xFFBA1A1A)
                    else -> Color(0xFF565E71)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = toneColor.copy(alpha = 0.1f)),
                    border = BoxStroke(1.dp, toneColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = toneColor, modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "এআই সেন্টিমেন্ট: ${article.sentiment}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = toneColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = article.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp,
                color = Color(0xFF1B1B1F)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF565E71))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "প্রতিবেদক: ${article.author}  •  তাজা প্রকাশনা ডেস্ক",
                    fontSize = 12.sp,
                    color = Color(0xFF565E71),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = article.content,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = Color(0xFF44474F)
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (article.seoKeywords.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                    border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF005FAF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "অভিযোজিত এসইও কীওয়ার্ডস (SEO Optimizer)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Badge(containerColor = Color(0xFFEADDFF), contentColor = Color(0xFF21005D)) {
                                Text("স্কোর: ${article.seoScore}/100", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = article.seoKeywords,
                            fontSize = 12.sp,
                            color = Color(0xFF44474F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE0E2EC))
            Spacer(modifier = Modifier.height(16.dp))

            // Social Automation distribution controller
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "১-ক্লিক সোশ্যাল অটোমেশন প্রচারণা scheduler",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B1B1F),
                        fontSize = 14.sp
                    )
                    Text(
                        "জমা হওয়া এই নিউজ নিবন্ধটিকে এআই জেনারেটেড কপি এবং হ্যাশট্যাগ সহ এক ক্লিকে বিভিন্ন সমাজমাধ্যমে শিডিউল করুন:",
                        fontSize = 12.sp,
                        color = Color(0xFF565E71)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = fbChecked, onCheckedChange = { fbChecked = it })
                            Text("Facebook", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = twChecked, onCheckedChange = { twChecked = it })
                            Text("Twitter (X)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = liChecked, onCheckedChange = { liChecked = it })
                            Text("LinkedIn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.scheduleSocialSharingQueue(article, fbChecked, twChecked, liChecked)
                            Toast.makeText(context, "প্রচারণা কিউতে যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_distribution_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পোস্ট প্রচার শিডিউল করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ---------------- TAB 2: JOURNALIST WORKROOM WITH GEMINI API ----------------

@Composable
fun JournalistWorkspaceScreen(viewModel: NewsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val draftTitle by viewModel.draftTitle.collectAsStateWithLifecycle()
    val draftContent by viewModel.draftContent.collectAsStateWithLifecycle()
    val draftCategory by viewModel.draftCategory.collectAsStateWithLifecycle()
    val draftAuthor by viewModel.draftAuthor.collectAsStateWithLifecycle()
    
    val seoKeywords by viewModel.seoKeywordsOutput.collectAsStateWithLifecycle()
    val seoScore by viewModel.seoScoreOutput.collectAsStateWithLifecycle()
    val sentiment by viewModel.sentimentOutput.collectAsStateWithLifecycle()

    val aiRewriting by viewModel.aiRewriting.collectAsStateWithLifecycle()
    val sentimentLoading by viewModel.sentimentLoading.collectAsStateWithLifecycle()
    val seoLoading by viewModel.seoLoading.collectAsStateWithLifecycle()
    val trendsLoading by viewModel.trendsLoading.collectAsStateWithLifecycle()
    
    val activeTrends by viewModel.activeTrends.collectAsStateWithLifecycle()

    // Gmail state observation
    val isGmailConnected by viewModel.isGmailConnected.collectAsStateWithLifecycle()
    val isFetchingGmail by viewModel.isFetchingGmail.collectAsStateWithLifecycle()
    val gmailMails by viewModel.gmailMails.collectAsStateWithLifecycle()
    val gmailAccessToken by viewModel.gmailAccessToken.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("journalist_workspace_scroll")
    ) {
        
        // Header
        Text(
            "সাংবাদিক অটোমেশন ওয়ার্কফ্লো",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF1B1B1F)
        )
        Text(
            "এআই চালিত নিউজ অপ্টিমাইজেশন, ট্রেন্ডিং এনালাইসিস এবং এসইও চেকআউট টুলস।",
            color = Color(0xFF565E71),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gmail connection Panel (dainikjahan@gmail.com Integration)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("gmail_connector_panel")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Gmail Connector",
                        tint = Color(0xFFBA1A1A),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "জিমেইল অটোমেশন ফিড (dainikjahan@gmail.com)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "dainikjahan@gmail.com ঠিকানায় প্রেরিত তাজা রিপোর্টিং তথ্য ইমেইলগুলো ১-ক্লিকে পর্যালোচনার খসড়া হিসেবে পোর্টাল কন্টেন্ট ইনপুটে আমদানি করুন।",
                    fontSize = 11.sp,
                    color = Color(0xFF565E71)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (!isGmailConnected) {
                    Column {
                        var tempToken by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = tempToken,
                            onValueChange = { tempToken = it },
                            label = { Text("গুগল ওউথ অ্যাক্সেস টোকেন (OAuth Access Token)") },
                            placeholder = { Text("টোকেন লিখুন বা সিমুলেটেড কানেক্ট করুন") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gmail_token_input"),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val token = tempToken.trim().ifEmpty { "simulated_oauth_dainikjahan_token" }
                                    viewModel.connectGmail(token)
                                    Toast.makeText(context, "dainikjahan@gmail.com সংযুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("gmail_connect_btn")
                            ) {
                                Text("সংযুক্ত করুন (Connect)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    viewModel.connectGmail("simulated_oauth_dainikjahan_token")
                                    Toast.makeText(context, "টেস্ট ওয়্যারলেস নোড সক্রিয়!", Toast.LENGTH_SHORT).show()
                                },
                                border = BoxStroke(1.dp, Color(0xFF005FAF)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF005FAF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gmail_fast_sim_btn")
                            ) {
                                Text("ডিমো লোড", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF2E7D32), shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "সংযুক্ত: dainikjahan@gmail.com",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.fetchGmailEmails() },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFF3F4F9), shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync",
                                        tint = Color(0xFF1B1B1F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        viewModel.disconnectGmail()
                                        Toast.makeText(context, "জিমেইল সংযোগ বিচ্ছিন্ন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
                                    border = BoxStroke(1.dp, Color(0xFFBA1A1A)),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("gmail_disconnect_btn")
                                ) {
                                    Text("বিচ্ছিন্ন করুন", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "সাংবারিকদের পাঠানো ইনবক্সের মেইলসমূহ (অপঠিত):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (isFetchingGmail) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFBA1A1A), modifier = Modifier.size(24.dp))
                            }
                        } else if (gmailMails.isEmpty()) {
                            Text(
                                "সব খসড়া সুচারুভাবে আমদানি করা হয়েছে। কোনো নতুন নোটিশ বা অপঠিত সংবাদ ইমেল নেই।",
                                fontSize = 11.sp,
                                color = Color(0xFF565E71),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            gmailMails.forEach { email ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FD)),
                                    border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "প্রেরক: ${email.from}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF005FAF),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = email.date,
                                                fontSize = 9.sp,
                                                color = Color(0xFF565E71)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = email.subject,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1B1B1F)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = email.snippet,
                                            fontSize = 11.sp,
                                            color = Color(0xFF565E71),
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Button(
                                            onClick = {
                                                viewModel.importGmailAsReviewDraft(email)
                                                Toast.makeText(context, "খসড়া কার্যাবলীতে সফলভাবে ইমপোর্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .testTag("import_gmail_mail_${email.id}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("১-ক্লিক এডিটিং বোর্ডে আমদানি করুন", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Editor inputs
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "নতুন প্রতিবেদন খসড়া",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1B1B1F)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { viewModel.draftTitle.value = it },
                    label = { Text("প্রতিবেদনের শিরোনাম (Headline)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_draft_title"),
                    maxLines = 2,
                    textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = draftContent,
                    onValueChange = { viewModel.draftContent.value = it },
                    label = { Text("সংবাদ কন্টেন্ট বডি (Bengali content)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("input_draft_content"),
                    maxLines = 8
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = draftAuthor,
                        onValueChange = { viewModel.draftAuthor.value = it },
                        label = { Text("প্রতিবেদক / লেখক") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .testTag("input_draft_author")
                    )

                    // Category Selector
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)) {
                        OutlinedTextField(
                            value = draftCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ক্যাটাগরি") },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_category_dropdown")
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("Bangladesh", "Economy", "Politics", "Sports", "Tech", "International").forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        viewModel.draftCategory.value = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gemini AI Assistants Pane
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Face,
                            contentDescription = "Gemini AI",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gemini-3.5 এআই সম্পাদকীয় সহকারী",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B1B1F),
                            fontSize = 14.sp
                        )
                    }
                    Badge(containerColor = Color(0xFFEADDFF), contentColor = Color(0xFF21005D)) {
                        Text("AI ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                
                Text(
                    "Gemini API-র মাধ্যমে আপনার শিরোনাম এবং সংবাদ লেখার ঢং ও সার্চ র‍্যাংকিং ১-ক্লিকে অপ্টিমাইজ করুন:",
                    fontSize = 11.sp,
                    color = Color(0xFF565E71),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Row 1: Rewrite style selection
                Text("এআই রিরাইটের স্টাইল চয়েসুন:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                var styleExpanded by remember { mutableStateOf(false) }
                var selectedStyle by remember { mutableStateOf("Engaging") }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1.3f)) {
                        Button(
                            onClick = { styleExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("style_dropdown_selector")
                        ) {
                            Text(selectedStyle, color = Color(0xFF1B1B1F), fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF565E71))
                        }
                        DropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                            listOf("Engaging", "Formal Journalist", "Dramatic/Urgent", "SEO-Maximized").forEach { styleOpt ->
                                DropdownMenuItem(
                                    text = { Text(styleOpt) },
                                    onClick = {
                                        selectedStyle = styleOpt
                                        styleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (draftTitle.isEmpty() || draftContent.isEmpty()) {
                                Toast.makeText(context, "শিরোনাম এবং বিষয়বস্তু আগে পূরণ করুন!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerAiRewrite(selectedStyle)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_rewrite_btn")
                    ) {
                        if (aiRewriting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("এআই রিরাইট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE0E2EC))
                Spacer(modifier = Modifier.height(10.dp))

                // Audit buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    
                    // Sentiment audit button
                    Button(
                        onClick = {
                            if (draftTitle.isEmpty() || draftContent.isEmpty()) {
                                Toast.makeText(context, "তথ্য পূরণ করুন!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerSentimentCheck()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEDF1FF),
                            contentColor = Color(0xFF005FAF)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_sentiment_audit_btn")
                    ) {
                        if (sentimentLoading) {
                            CircularProgressIndicator(color = Color(0xFF005FAF), modifier = Modifier.size(14.dp))
                        } else {
                            Text("সেন্টিমেন্ট চেক", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FAF))
                        }
                    }

                    // SEO Audit button
                    Button(
                        onClick = {
                            if (draftTitle.isEmpty() || draftContent.isEmpty()) {
                                Toast.makeText(context, "তথ্য পূরণ করুন!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerSeoAuditOffline()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEDF1FF),
                            contentColor = Color(0xFF005FAF)
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("ai_seo_audit_btn")
                    ) {
                        if (seoLoading) {
                            CircularProgressIndicator(color = Color(0xFF005FAF), modifier = Modifier.size(14.dp))
                        } else {
                            Text("এসইও কীওয়ার্ড চেক", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FAF))
                        }
                    }
                }

                // AI Outputs scorecard displays
                if (sentiment.isNotEmpty() || seoKeywords.isNotEmpty() || seoScore > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                        border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("এআই অডিট ফলাফল:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FAF))
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            if (sentiment.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("নিউজ টোন অনুধাবন: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1F))
                                    Badge(containerColor = Color(0xFFEADDFF), contentColor = Color(0xFF21005D)) {
                                        Text(sentiment, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            if (seoScore > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("এসইও নির্ভরযোগ্য সূচক: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1F))
                                    Text("$seoScore / 100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005FAF))
                                }
                            }

                            if (seoKeywords.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("প্রস্তাবিত মেটা কীওয়ার্ডস: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF565E71))
                                Text(seoKeywords, fontSize = 12.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hot Topics suggestion by AI
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF005FAF))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Gemini রিয়েল-টাইম হট ট্রেন্ড স্পটার",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF005FAF),
                            fontSize = 13.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.fetchHotTrends() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Trends", tint = Color(0xFF005FAF))
                    }
                }

                Text(
                    "বাংলাদেশী সমাজমাধ্যম ও অনলাইন পোর্টাল বিশ্লেষণ করে সংগৃহীত ৩টি হট ট্রেন্ডিং নিউজ টপিক:",
                    fontSize = 11.sp,
                    color = Color(0xFF565E71)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (trendsLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF005FAF))
                    }
                } else {
                    activeTrends.forEachIndexed { idx, item ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("${idx + 1}. ", fontWeight = FontWeight.Bold, color = Color(0xFF005FAF), fontSize = 12.sp)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B1B1F))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = Color(0xFFEDF1FF), contentColor = Color(0xFF005FAF)) {
                                        Text(item.category, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                                Text("গাইড: ${item.recommendation}", fontSize = 11.sp, color = Color(0xFF565E71))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submission Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (draftTitle.isEmpty() || draftContent.isEmpty()) {
                        Toast.makeText(context, "শিরোনাম এবং বিষয়বস্তু আগে লিখুন!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveDraftArticleOnly()
                        Toast.makeText(context, "খসড়া সফলভাবে সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                },
                border = BoxStroke(1.dp, Color(0xFF005FAF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF005FAF)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_draft_only_btn")
            ) {
                Text("খসড়া সংরক্ষণ", fontWeight = FontWeight.Bold, color = Color(0xFF005FAF))
            }

            Button(
                onClick = {
                    if (draftTitle.isEmpty() || draftContent.isEmpty()) {
                        Toast.makeText(context, "খবর পূরণ করুন!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.publishDraftArticle()
                        Toast.makeText(context, "অভিনন্দন, সংবাদ সফলভাবে প্রকাশিত এবং নোটিফাই সম্পন্ন!", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("journalist_publish_article_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("তাত্ক্ষণিক প্রকাশ করুন", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------- TAB 3: AUTO SOCIAL SCHEDULING QUEUE ----------------

@Composable
fun SocialQueueScreen(
    socialPosts: List<SocialQueuePost>,
    viewModel: NewsViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "সোশ্যাল অটোমেশন প্রচার শিডিউলার",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF1B1B1F)
        )
        Text(
            "এখানে পূর্বে শিডিউল করা বা পেন্ডিং সমাজিক প্রচারণা পোস্ট সমূহের কিউ তালিকা বিদ্যমান।",
            color = Color(0xFF565E71),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (socialPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = Color(0xFFDDE2F0),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "কোনো নির্ধারিত সোশ্যাল বা সামাজিক পোস্ট নেই",
                        color = Color(0xFF1B1B1F),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "পোর্টালের যেকোনো আর্টিকেলের '১-ক্লিক অটোমেশন' সেকশন থেকে শিডিউল করুন।",
                        color = Color(0xFF565E71),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("social_queue_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(socialPosts) { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFF005FAF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "প্ল্যাটফর্ম: ${post.platforms}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005FAF)
                                    )
                                }
                                
                                val statusBg = if (post.status == "Published") Color(0xFFEDF1FF) else Color(0xFFFFF1EB)
                                val statusTxt = if (post.status == "Published") Color(0xFF005FAF) else Color(0xFFB06000)
                                Card(colors = CardDefaults.cardColors(containerColor = statusBg)) {
                                    Text(
                                        text = if (post.status == "Published") "প্রচারিত" else "শিডিউলড",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTxt
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "রিলেটেড খবর: ${post.articleTitle}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF565E71)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = post.optimizedContent,
                                fontSize = 12.sp,
                                color = Color(0xFF1B1B1F),
                                modifier = Modifier
                                    .background(Color(0xFFF3F4F9), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.removeScheduledPost(post) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                                ) {
                                    Text("বাতিল করুন", fontSize = 12.sp)
                                }

                                if (post.status != "Published") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.publishSocialPostNow(post)
                                            Toast.makeText(context, "সাফল্যের সাথে সমাজমাধ্যমে প্রচার করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                                    ) {
                                        Text("এখনই প্রচার করুন (১-ক্লিক)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 4: PERFORMANCE ANALYTICS & VISUAL GRAPH ----------------

@Composable
fun CampaignDashboardScreen(
    metrics: List<AnalyticsMetric>,
    articles: List<Article>,
    viewModel: NewsViewModel
) {
    val context = LocalContext.current
    val totalViews = articles.sumOf { it.views }
    val averageSeo = if (articles.isNotEmpty()) articles.map { it.seoScore }.average().toInt() else 0

    // Filter metrics by category
    val trafficMetrics = metrics.filter { it.metricCategory == "Daily traffic" }
    val ctrMetrics = metrics.filter { it.metricCategory == "CTR growth" }
    val engagementMetrics = metrics.filter { it.metricCategory == "Social engagements" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "ক্যাম্পেইন পারফরম্যান্স অ্যানালিটিক্স",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF1B1B1F)
        )
        Text(
            "আর্টিকেল ভিউ বৃদ্ধি, এসইও স্কোর এবং ১-ক্লিক সোশাল শেয়ারিং অর্গানিক গ্রোথ রিপোর্ট।",
            color = Color(0xFF565E71),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // High level KPI Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("মোট নিউজ ট্রাফিক", fontSize = 11.sp, color = Color(0xFF565E71), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalViews Views", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF005FAF))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("এভারেজ এসইও", fontSize = 11.sp, color = Color(0xFF565E71), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$averageSeo% SEO Index", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF005FAF))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Growth simulation trigger
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
            border = BoxStroke(1.dp, Color(0xFFDDE2F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("তাত্ক্ষণিক ট্রাফিক গ্রোথ ড্রাইভ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B1B1F))
                    Text("উন্নত এআই কীওয়ার্ড ও এসইও নিবন্ধ প্রচারের মাধ্যমে ট্রাফিক বাড়ান ও রিয়েল টাইমে ট্র্যাক করুন:", fontSize = 11.sp, color = Color(0xFF565E71))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        viewModel.triggerDynamicEngagementGrowth()
                        Toast.makeText(context, "ট্রাফিক সার্জ ও সামাজিক প্রচারণা গতি বেড়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                    modifier = Modifier.testTag("trigger_surge_btn")
                ) {
                    Text("সার্জ ড্রাইভ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Chart 1: Daily Traffic Views (Line Graph)
        Text(
            "📈 দৈনিক ট্রাফিক ট্রেন্ড (পৃষ্ঠা ভিউ বৃদ্ধি)",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = Color(0xFF1B1B1F)
        )
        Spacer(modifier = Modifier.height(6.dp))
        NewsMetricsChart(metrics = trafficMetrics, strokeColor = Color(0xFF005FAF), isPercentage = false)

        Spacer(modifier = Modifier.height(20.dp))

        // Chart 2: Campaign Click-Through-Rate (Bar Graph)
        Text(
            "📊 বিজ্ঞাপন ও আর্টিকেলের CTR অবিরত বৃদ্ধি (%)",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = Color(0xFF1B1B1F)
        )
        Spacer(modifier = Modifier.height(6.dp))
        NewsMetricsChart(metrics = ctrMetrics, strokeColor = Color(0xFF005FAF), isPercentage = true)

        Spacer(modifier = Modifier.height(20.dp))

        // Chart 3: Social engagements (Shares + Comments)
        Text(
            "↗️ সামগ্রিক সামাজিক মিথস্ক্রিয়া ও ভাইরাল সূচক",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = Color(0xFF1B1B1F)
        )
        Spacer(modifier = Modifier.height(6.dp))
        NewsMetricsChart(metrics = engagementMetrics, strokeColor = Color(0xFFBA1A1A), isPercentage = false)

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ---------------- HIGH FIDELITY CUSTOM DRAWN CANVAS CHARTS ----------------

@Composable
fun NewsMetricsChart(
    metrics: List<AnalyticsMetric>,
    strokeColor: Color,
    isPercentage: Boolean
) {
    if (metrics.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("গ্রাফ ডেটা লোড হচ্ছে...", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    val labels = metrics.map { it.label }
    val values = metrics.map { it.value }
    val maxValue = values.maxOrNull() ?: 100f
    val minValue = values.minOrNull() ?: 0f
    val valueRange = if (maxValue - minValue == 0f) 100f else (maxValue - minValue)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BoxStroke(1.dp, Color(0xFFE0E2EC)),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "সর্বোচ্চ: ${if (isPercentage) "%.1f".format(maxValue) + "%" else "%,.0f".format(maxValue)}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "স্ন্যাপশট: সক্রিয়",
                    fontSize = 10.sp,
                    color = Color(0xFF005FAF),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas drawing for premium chart curves
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (metrics.size - 1).coerceAtLeast(1)

                val points = metrics.mapIndexed { idx, it ->
                    val ratio = (it.value - minValue) / valueRange
                    // Margin padding inside canvas to prevent bounds cut
                    val padding = 10f
                    val y = height - (ratio * (height - 2 * padding)) - padding
                    val x = idx * stepX
                    Offset(x, y)
                }

                // Draw background horizontal gridline
                for (i in 1..3) {
                    val gy = height * (i / 4f)
                    drawLine(
                        color = Color(0xFFECEFF1),
                        start = Offset(0f, gy),
                        end = Offset(width, gy),
                        strokeWidth = 2f
                    )
                }

                // Create fill path for gorgeous gradient aura below lines
                val fillPath = Path().apply {
                    moveTo(0f, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(width, height)
                    close()
                }

                // Draw shiny translucent area
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(strokeColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw the main curve line
                val curvePath = Path().apply {
                    points.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = curvePath,
                    color = strokeColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw point node circles
                points.forEach { pt ->
                    drawCircle(
                        color = strokeColor,
                        radius = 8f,
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = pt
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ---------------- NOTIFICATION ALERTS BOTTOM PANEL ----------------

@Composable
fun NotificationCenterPanel(
    notifications: List<NotificationRecord>,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔔 রিয়েল-টাইম নোটিফিকেশন লগ",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color(0xFF004D3C)
            )

            TextButton(
                onClick = onClearAll,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828)),
                modifier = Modifier.testTag("clear_alerts_btn")
            ) {
                Text("সব মুছুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো নতুন নোটিফিকেশন এলার্ট নেই।",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("notifications_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F3)),
                        border = BoxStroke(1.dp, Color(0xFFD6ECE6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF003D30)
                                )
                                Text(
                                    text = "সক্রিয় এলার্ট",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.body,
                                fontSize = 12.sp,
                                color = Color(0xFF4C5E5A)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D3C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("বন্ধ করুন", fontWeight = FontWeight.Bold)
        }
    }
}
