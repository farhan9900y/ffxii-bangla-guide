package com.example

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.service.FFXIIJiniService
import com.example.ui.theme.*
import com.example.assistant.BanglaVoiceAssistant
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private var voiceAssistant: BanglaVoiceAssistant? = null
    private val triggerVoiceFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Colloquial Bangla Voice assistant
        voiceAssistant = BanglaVoiceAssistant(this) { success ->
            Log.d("MainActivity", "Bangla Voice Assistant ready: $success")
        }

        setContent {
            MyApplicationTheme {
                val triggerVoice by triggerVoiceFlow.collectAsState()

                FFXIIApp(
                    voiceAssistant = voiceAssistant,
                    triggerNextVoice = triggerVoice,
                    onVoiceTriggerHandled = { triggerVoiceFlow.value = false },
                    onToggleOverlayService = { enable -> toggleOverlayService(enable) }
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("TRIGGER_VOICE_OUT_NEXT", false) == true) {
            triggerVoiceFlow.value = true
        }
    }

    private fun toggleOverlayService(enable: Boolean) {
        if (enable) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "ভাসমান বাবল চালুর জন্য ওভারলে অনুমতি দিন দোস্ত!", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                startService(Intent(this, FFXIIJiniService::class.java))
                Toast.makeText(this, "সিস্টেম-ওয়াইড জাদুকর বাবল চালু হইসে!", Toast.LENGTH_SHORT).show()
            }
        } else {
            stopService(Intent(this, FFXIIJiniService::class.java))
            Toast.makeText(this, "জাদুকর বাবল বন্ধ হইসে!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceAssistant?.shutdown()
    }
}

@Composable
fun FFXIIApp(
    voiceAssistant: BanglaVoiceAssistant?,
    triggerNextVoice: Boolean,
    onVoiceTriggerHandled: () -> Unit,
    onToggleOverlayService: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Guides, 1 = Database, 2 = Jini Smart Search

    // Dynamic State Holders for Inner Details views
    var selectedStoryChapter by remember { mutableStateOf<StoryChapter?>(null) }
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    var selectedHunt by remember { mutableStateOf<Hunt?>(null) }
    var selectedEsper by remember { mutableStateOf<Esper?>(null) }
    var selectedCharacter by remember { mutableStateOf<GameCharacter?>(null) }

    // Bento Sub Tab Tracks
    var guideSubTab by remember { mutableIntStateOf(0) } // 0 = Bento Dashboard, 1 = Story, 2 = Missions, 3 = Hunts, 4 = Espers
    var databaseSubTab by remember { mutableIntStateOf(0) } // 0 = Character, 1 = Equipments, 2 = Monsters

    // Jini Chat state
    var queryText by remember { mutableStateOf("") }
    var jiniResponse by remember { mutableStateOf("আরে জোয়ান দোস্ত! ফাইনাল ফ্যান্টাসি ১২ এর দুনিয়ায় তোমারে স্বাগতম। ভ্যালেন্টাইন বা রাজকুমারী অ্যাশ কীভাবে মিশন খেলবা? জটপট জিগাও আমারে, পানির মতো সোজা কইরা বুঝায় দিমু!") }

    // Floating Bubble Settings
    var isSystemOverlayEnabled by remember { mutableStateOf(false) }
    var isInAppBubbleEnabled by remember { mutableStateOf(true) }
    var bubbleOffset by remember { mutableStateOf(IntOffset(120, 320)) }

    // Watcher for external intent activation
    LaunchedEffect(triggerNextVoice) {
        if (triggerNextVoice) {
            selectedTab = 2 // Switch automatically to Smart Jini Tab
            val nextGuide = FFXIIDataSource.searchJini("next")
            jiniResponse = nextGuide
            voiceAssistant?.speak(nextGuide)
            onVoiceTriggerHandled()
        }
    }

    // Main Outer Container allowing custom overlay layouts with dual radial gaming gradients from the Bento design
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090118))
            .drawBehind {
                // Top-right radial spotlight (#3B0764)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B0764), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        radius = size.width * 1.2f
                    )
                )
                // Bottom-left radial spotlight (#1E3A8A)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, size.height),
                        radius = size.width * 1.2f
                    )
                )
            }
    ) {

        // Scaffold styled with dynamic custom dark gaming colors
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0x99090118),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.MenuBook, contentDescription = "গাইডবুক") },
                        label = { Text("গাইডবুক", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrystalGold,
                            selectedTextColor = CrystalGold,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = CosmicPurp
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.Storage, contentDescription = "ডেটাবেস") },
                        label = { Text("ডেটাবেস", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrystalGold,
                            selectedTextColor = CrystalGold,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = CosmicPurp
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "জাদুকর জিনি") },
                        label = { Text("জাদুকর জিনি", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlowBlue,
                            selectedTextColor = GlowBlue,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = CosmicPurp
                        )
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Hero Banner Visualizer with parallax or gorgeous image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1782207631000),
                        contentDescription = "Bhujerba floating Sky City",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF090118)),
                                    startY = 60f
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_1782207615553),
                            contentDescription = "Jini Icon Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CrystalGold, RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ফাইনাল ফ্যান্টাসি XII বাংলা জিনি",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                style = LocalTextStyle.current.copy(shadow = androidx.compose.ui.graphics.Shadow(color = GlowBlue, blurRadius = 4f))
                            )
                            Text(
                                text = "অফলাইন বুক ও ভয়েস জাদুকর",
                                color = GlowBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Main Navigation Screen routing
                Crossfade(targetState = selectedTab, label = "TabNavigation") { tab ->
                    when (tab) {
                        0 -> GuidesSection(
                            selectedStory = selectedStoryChapter,
                            selectedMission = selectedMission,
                            selectedHunt = selectedHunt,
                            selectedEsper = selectedEsper,
                            onStorySelect = { selectedStoryChapter = it },
                            onMissionSelect = { selectedMission = it },
                            onHuntSelect = { selectedHunt = it },
                            onEsperSelect = { selectedEsper = it },
                            onNavigateToDatabase = { sub ->
                                databaseSubTab = sub
                                selectedTab = 1
                            },
                            subTab = guideSubTab,
                            onSubTabChange = { guideSubTab = it }
                        )
                        1 -> DatabaseSection(
                            selectedCharacter = selectedCharacter,
                            onCharacterSelect = { selectedCharacter = it },
                            subTab = databaseSubTab,
                            onSubTabChange = { databaseSubTab = it }
                        )
                        2 -> JiniSection(
                            queryText = queryText,
                            onQueryChange = { queryText = it },
                            jiniResponse = jiniResponse,
                            onSearchSubmit = { q ->
                                val res = FFXIIDataSource.searchJini(q)
                                jiniResponse = res
                                voiceAssistant?.speak(res)
                            },
                            voiceAssistant = voiceAssistant,
                            isSystemOverlayEnabled = isSystemOverlayEnabled,
                            onToggleSystemOverlay = {
                                isSystemOverlayEnabled = it
                                onToggleOverlayService(it)
                            },
                            isInAppBubbleEnabled = isInAppBubbleEnabled,
                            onToggleInAppBubble = { isInAppBubbleEnabled = it }
                        )
                    }
                }
            }
        }

        // Draggable In-App Crystal Jini Floating Bubble
        if (isInAppBubbleEnabled) {
            Box(
                modifier = Modifier
                    .offset { bubbleOffset }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            bubbleOffset = IntOffset(
                                x = (bubbleOffset.x + dragAmount.x).toInt(),
                                y = (bubbleOffset.y + dragAmount.y).toInt()
                            )
                        }
                    }
                    .size(72.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPurp, CosmicPurp)
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, CrystalGold, shape = CircleShape)
                    .clickable {
                        // Speaks the next objective and navigates directly to the voice assistant!
                        val nextMissionSpeech = FFXIIDataSource.searchJini("next")
                        jiniResponse = nextMissionSpeech
                        voiceAssistant?.speak(nextMissionSpeech)
                        selectedTab = 2
                        Toast
                            .makeText(context, "আরে দোস্ত! জিনি নিয়ে আসলাম স্ক্রিনে!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .testTag("in_app_jini_bubble"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon_1782207615553),
                    contentDescription = "FFXII Genie Magic Crystal",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, GlowBlue, CircleShape)
                )
            }
        }
    }
}

// ------------------- GUIDES TAB (STORY, MISSIONS, HUNTS, ESPERS) -------------------
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x13FFFFFF) // Transparent bento glass style
        ),
        border = BorderStroke(1.dp, Color(0x16FFFFFF))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

@Composable
fun GuidesSection(
    selectedStory: StoryChapter?,
    selectedMission: Mission?,
    selectedHunt: Hunt?,
    selectedEsper: Esper?,
    onStorySelect: (StoryChapter?) -> Unit,
    onMissionSelect: (Mission?) -> Unit,
    onHuntSelect: (Hunt?) -> Unit,
    onEsperSelect: (Esper?) -> Unit,
    onNavigateToDatabase: (Int) -> Unit,
    subTab: Int,
    onSubTabChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Scrollable Sub Tabs bar for beautiful grid options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0x33000000))
                .padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            val subTabs = listOf("ড্যাশবোর্ড (Home)", "কাহিনী (Story)", "মিশন গাইড", "হান্ট লিজেন্ড", "এস্পার দানব")
            subTabs.forEachIndexed { index, title ->
                Button(
                    onClick = {
                        onSubTabChange(index)
                        // Reset nested expand views
                        onStorySelect(null)
                        onMissionSelect(null)
                        onHuntSelect(null)
                        onEsperSelect(null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subTab == index) CosmicPurp else Color.Transparent,
                        contentColor = if (subTab == index) CrystalGold else Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("guide_sub_tab_$index")
                ) {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = Color(0x16FFFFFF), thickness = 1.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(12.dp)
        ) {
            when (subTab) {
                0 -> {
                    // Bento Grid Dashboard View (New Theme integration)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Active Mission (col-span-4)
                        val activeMission = FFXIIDataSource.missions.firstOrNull()
                        activeMission?.let { m ->
                            BentoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                onClick = {
                                    onMissionSelect(m)
                                    onSubTabChange(2) // Jump to Mission sub-tab
                                }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Decorative backing radial light indicator
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-10).dp)
                                            .size(80.dp)
                                            .background(Color(0x132563EB), CircleShape)
                                            .border(1.dp, Color(0x222563EB), CircleShape)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "চলমান মিশন",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color(0xFF2563EB), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = m.title,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "পরবর্তী গন্তব্য: ${m.location}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Story, Esper & Hunt Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Story Card
                            BentoCard(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight(),
                                onClick = { onSubTabChange(1) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF7C3AED).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📖", fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(
                                            text = "কাহিনী (Story)",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "অধ্যায় ভিত্তিক বাংলা অনুবাদ",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }

                            // Right side stack: Espers & Hunts
                            Column(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Espers Card
                                BentoCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSubTabChange(4) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("🔥", fontSize = 18.sp)
                                        Text(
                                            text = "এসপার গাইড",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Hunts Card
                                BentoCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSubTabChange(3) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("⚔️", fontSize = 18.sp)
                                        Text(
                                            text = "হান্ট বোর্ড",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Characters, Equipment & Monsters Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Heroes/Characters Card
                            BentoCard(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight(),
                                onClick = { onNavigateToDatabase(0) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        listOf("V", "A", "B").forEachIndexed { idx, name ->
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = (idx * -6).dp)
                                                    .size(24.dp)
                                                    .background(Color(0xFF1E293B), CircleShape)
                                                    .border(1.dp, Color(0xFF7C3AED), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "চরিত্রসমূহ",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "ভ্যান থেকে প্যানেলোর দল",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }

                            // Right Column (Weapons & Monsters)
                            Column(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Equipment
                                BentoCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToDatabase(1) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "অস্ত্র ও বর্ম",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("🛡️", fontSize = 16.sp)
                                    }
                                }

                                // Monsters
                                BentoCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToDatabase(2) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "দানব ড্যাটা",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("👾", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Story List Selector
                    if (selectedStory == null) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(FFXIIDataSource.chapters) { ch ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStorySelect(ch) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                    border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Book,
                                            contentDescription = "Chapter",
                                            tint = CrystalGold,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ch.number,
                                                color = GlowBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = ch.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = ch.englishTitle,
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Open", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    } else {
                        // Expanded Story View
                        StoryReaderDetail(chapter = selectedStory, onBack = { onStorySelect(null) })
                    }
                }
                2 -> {
                    // Mission List Selector
                    if (selectedMission == null) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(FFXIIDataSource.missions) { m ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onMissionSelect(m) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                    border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.DirectionsRun,
                                                contentDescription = "Mission",
                                                tint = GlowBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = m.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "মেইন লক্ষ্য: " + m.objective,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "লোকেশন: " + m.location,
                                            color = CrystalGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Expanded Mission View File
                        MissionReaderDetail(mission = selectedMission, onBack = { onMissionSelect(null) })
                    }
                }
                3 -> {
                    // Hunt Section Selector
                    if (selectedHunt == null) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(FFXIIDataSource.hunts) { hunt ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onHuntSelect(hunt) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                    border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = hunt.rank,
                                                    color = CrystalGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(
                                                            CosmicPurp,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = hunt.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "স্থান: " + hunt.location,
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Hunt Trophy",
                                            tint = GlowBlue
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        HuntDetail(hunt = selectedHunt, onBack = { onHuntSelect(null) })
                    }
                }
                4 -> {
                    // Esper Summon Section Selector
                    if (selectedEsper == null) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(FFXIIDataSource.espers) { esp ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEsperSelect(esp) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                    border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.FlashOn,
                                            contentDescription = "Summon Spark",
                                            tint = CrystalGold,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = esp.name,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "রাশিচক্র: " + esp.constellation,
                                                color = GlowBlue,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "See Summon Specs", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    } else {
                        EsperDetail(esper = selectedEsper, onBack = { onEsperSelect(null) })
                    }
                }
            }
        }
    }
}

// Sub Detailed Book Layout elements
@Composable
fun StoryReaderDetail(chapter: StoryChapter, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button_story")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GlowBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ফিরে যান", color = GlowBlue, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(chapter.number, color = CrystalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(chapter.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(chapter.englishTitle, color = Color.Gray, fontSize = 12.sp)
        Divider(color = CosmicPurp, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
        
        Card(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CosmicPurp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMana)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = chapter.summary,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Composable
fun MissionReaderDetail(mission: Mission, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button_mission")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GlowBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ফিরে যান", color = GlowBlue, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(mission.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("অবস্থান: ${mission.location}", color = GlowBlue, fontSize = 12.sp)
        
        Divider(color = CosmicPurp, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
        
        // Objective Section
        Text("মেইন অবজেক্টিভ বা বর্তমান লক্ষ্য:", color = CrystalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMana),
            border = BorderStroke(1.dp, CosmicPurp)
        ) {
            Text(mission.objective, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        }

        // Steps mapping
        Spacer(modifier = Modifier.height(10.dp))
        Text("কী কী করতে হবে (ধাপসমূহ):", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        mission.steps.forEachIndexed { idx, step ->
            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp)
                        .background(GlowBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text((idx + 1).toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(step, color = Color.LightGray, fontSize = 12.sp)
            }
        }

        // Boss specs
        Spacer(modifier = Modifier.height(14.dp))
        Text("চেম্বারের মেইন বস লড়াই:", color = CrystalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMana),
            border = BorderStroke(1.dp, GlowBlue)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("বস নাম: ${mission.boss}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("ফাইট কৌশল: ${mission.bossStrategy}", color = Color.LightGray, fontSize = 12.sp)
            }
        }

        // Friends guide
        Spacer(modifier = Modifier.height(8.dp))
        Text("জিনি দোস্তের পরামর্শ:", color = GlowBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "💬 \"${mission.guideText}\"",
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .background(SurfaceMana, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}

@Composable
fun HuntDetail(hunt: Hunt, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GlowBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ফিরে যান", color = GlowBlue, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(hunt.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(hunt.rank, color = CrystalGold, fontSize = 12.sp)
        
        Divider(color = CosmicPurp, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("হানিং বায়ার (Petitioner):", color = GlowBlue, fontSize = 11.sp)
                Text(hunt.petitioner, color = Color.White, fontSize = 13.sp)
            }
            Column {
                Text("লোকেশন (Location):", color = GlowBlue, fontSize = 11.sp)
                Text(hunt.location, color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("হান্টিং রিকোয়ারমেন্টস (শর্তাবলি):", color = CrystalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(hunt.requirements, color = Color.LightGray, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(10.dp))
        Text("বিজয়ী রিওয়ার্ড (পুরস্কার):", color = GlowBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(hunt.reward, color = Color.White, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(12.dp))
        Text("মারার গোপন পদ্ধতি (Bangla Strategy):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            text = hunt.strategy,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .background(SurfaceMana, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}

@Composable
fun EsperDetail(esper: Esper, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GlowBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ফিরে যান", color = GlowBlue, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(esper.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("রাশিচক্র: " + esper.constellation, color = CrystalGold, fontSize = 12.sp)

        Divider(color = CosmicPurp, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

        Text("কোথায় অবস্থান করে:", color = GlowBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(esper.location, color = Color.White, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(10.dp))
        Text("কীভাবে আনলক করবা:", color = CrystalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(esper.unlockMethod, color = Color.LightGray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))
        Text("যুদ্ধ জয়ের জন্য জাদুকরী টিপস:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            text = esper.battleTips,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .background(SurfaceMana, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}

// ------------------- DATABASE TAB (CHARACTERS, EQUIPMENTS, MONSTERS) -------------------
@Composable
fun DatabaseSection(
    selectedCharacter: GameCharacter?,
    onCharacterSelect: (GameCharacter?) -> Unit,
    subTab: Int,
    onSubTabChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color(0x33000000),
            contentColor = CrystalGold
        ) {
            Tab(selected = subTab == 0, onClick = { onSubTabChange(0); onCharacterSelect(null) }) {
                Text("চরিত্র (Heroes)", fontSize = 12.sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = subTab == 1, onClick = { onSubTabChange(1); onCharacterSelect(null) }) {
                Text("অস্ত্র ও বর্ম", fontSize = 12.sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = subTab == 2, onClick = { onSubTabChange(2); onCharacterSelect(null) }) {
                Text("দানবকুল", fontSize = 12.sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            when (subTab) {
                0 -> {
                    if (selectedCharacter == null) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(FFXIIDataSource.characters) { char ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCharacterSelect(char) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                    border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(CosmicPurp, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char.name.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(char.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(char.role, color = GlowBlue, fontSize = 11.sp)
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Details", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    } else {
                        CharacterDetail(character = selectedCharacter, onBack = { onCharacterSelect(null) })
                    }
                }
                1 -> {
                    // Equipments Database Lists
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(FFXIIDataSource.equipments) { eq ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = eq.category,
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(CrystalGold, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = eq.type,
                                            color = GlowBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(eq.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("পরিসংখ্যান: ${eq.stats}", color = Color.LightGray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("দাম: ${eq.cost}", color = Color.Gray, fontSize = 11.sp)
                                    Text("লোকেশন বা প্রাপ্তি: ${eq.location}", color = CrystalGold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(FFXIIDataSource.monsters) { m ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                                border = BorderStroke(1.dp, Color(0x16FFFFFF)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(m.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("এইচপি: " + m.hp, color = CrystalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("লোকেশন: ${m.location}", color = Color.LightGray, fontSize = 11.sp)
                                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("দুর্বলতা বা ফাটল: ", color = GlowBlue, fontSize = 11.sp)
                                        Text(
                                            text = m.weakness,
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color(0xFFE63946), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text("মারলে কী লুট পাওয়া যায়: " + m.loot, color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterDetail(character: GameCharacter, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GlowBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ফিরে যান", color = GlowBlue, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(character.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(character.role, color = CrystalGold, fontSize = 12.sp)

        Divider(color = CosmicPurp, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

        Text("পছন্দের সেরা অস্ত্র:", color = GlowBlue, fontSize = 11.sp)
        Text(character.weapon, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(10.dp))
        Text("চরিত্রের ইতিহাস ও গল্প:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(character.story, color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)

        Spacer(modifier = Modifier.height(12.dp))
        Text("সেরা বিল্ড ও গেমিং পরামর্শ বা জ্যাম্বিট কৌশল:", color = CrystalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            text = character.tips,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .background(SurfaceMana, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}

// ------------------- JINI VOICE ASSISTANT TAB -------------------
@Composable
fun JiniSection(
    queryText: String,
    onQueryChange: (String) -> Unit,
    jiniResponse: String,
    onSearchSubmit: (String) -> Unit,
    voiceAssistant: BanglaVoiceAssistant?,
    isSystemOverlayEnabled: Boolean,
    onToggleSystemOverlay: (Boolean) -> Unit,
    isInAppBubbleEnabled: Boolean,
    onToggleInAppBubble: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Glowing Avatar for the Genie
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPurp, Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon_1782207615553),
                    contentDescription = "Glow Genie",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(2.dp, CrystalGold, CircleShape)
                )
            }
        }

        // Speech dialogue bubble card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMana),
            border = BorderStroke(1.dp, GlowBlue)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ChatBubble,
                        contentDescription = "Speech Bubble",
                        tint = GlowBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "জিনি দোস্ত বলছে:",
                        color = GlowBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = jiniResponse,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { voiceAssistant?.speak(jiniResponse) },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPurp),
                    modifier = Modifier.align(Alignment.End).testTag("speak_jini_response_button")
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "শুনাও", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("অনুবাদে আওয়াজ তুলুন", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Smart Search input
        Text("অফলাইন সার্চ ও সমাধান (টাইপ করো):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).testTag("smart_search_input"),
                placeholder = { Text("যেমন: ভোজরবার পর কি মিশন?", fontSize = 12.sp, color = Color.LightGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlowBlue,
                    unfocusedBorderColor = CosmicPurp,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { onSearchSubmit(queryText) },
                colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp).width(72.dp).testTag("smart_search_submit_button")
            ) {
                Icon(Icons.Filled.Search, contentDescription = "খুঁজুন", tint = Color.Black)
            }
        }

        // Direct Quick-questions buttons row
        Text("দ্রুত দোস্ত সাহায্য ও প্রশ্নোত্তর:", color = Color.LightGray, fontSize = 11.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp)
        ) {
            val quickQuestions = listOf(
                "পরের মিশন?" to "next",
                "জাজ বারগান কেমনে মারবো?" to "bergan",
                "বেলিয়াস কোথায় পাবো?" to "belias",
                "ভুজরবার পর কোন জায়গা?" to "bhujerba",
                "রাজকুমারী অ্যাশ কেমন?" to "ashe",
                "ভ্যান চরিত্রটা কী?" to "vaan"
            )
            quickQuestions.forEachIndexed { index, pair ->
                Button(
                    onClick = {
                        onQueryChange(pair.first)
                        onSearchSubmit(pair.second)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceMana),
                    border = BorderStroke(1.dp, CosmicPurp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("quick_question_$index")
                ) {
                    Text(pair.first, fontSize = 11.sp, color = CrystalGold)
                }
            }
        }

        Divider(color = CosmicPurp, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // Floating Bubble toggles options
        Text("জাদুকরের বাবল উইজেট সেটিংস:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // In app bubble toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceMana, shape = RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ইন-অ্যাপ মোবাইল বাবল (In-App Bubble)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("অ্যাপের ভেতরে ক্রিস্টাল বাবল ভাসবে যাকে আঙুল দিয়ে টেনে যেকোনো দিকে সরানো যায়।", color = Color.LightGray, fontSize = 10.sp)
            }
            Switch(
                checked = isInAppBubbleEnabled,
                onCheckedChange = onToggleInAppBubble,
                colors = SwitchDefaults.colors(checkedThumbColor = CrystalGold, checkedTrackColor = CosmicPurp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // System overlay bubble toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceMana, shape = RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("সিস্টেম-ওয়াইড মোবাইল বাবল (System-Wide)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("অনুরোধ: গেমটি বা অন্য অ্যাপ রান করার সময়ও এটি ভাসবে। ক্লিক করলে সাহায্য করবে।", color = Color.LightGray, fontSize = 10.sp)
            }
            Switch(
                checked = isSystemOverlayEnabled,
                onCheckedChange = onToggleSystemOverlay,
                colors = SwitchDefaults.colors(checkedThumbColor = CrystalGold, checkedTrackColor = CosmicPurp)
            )
        }
    }
}
