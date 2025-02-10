package com.Hamp.HolyQuran

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.InputStreamReader

// API related data classes
data class TranslatorResponse(
    val code: Int,
    val status: String,
    val data: List<Translator>
)

data class Translator(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String
)

data class TranslationResponse(
    val code: Int,
    val status: String,
    val data: TranslationData
)

data class TranslationData(
    val number: Int,
    val name: String,
    val englishName: String,
    val ayahs: List<TranslatedVerse>
)

data class TranslatedVerse(
    val number: Int,
    val text: String
)

// API Service interface
interface QuranTranslationService {
    @GET("edition")
    suspend fun getTranslators(
        @Query("format") format: String = "text",
        @Query("language") language: String = "fa"
    ): TranslatorResponse

    @GET("surah/{surahNumber}/{translator}")
    suspend fun getTranslation(
        @Path("surahNumber") surahNumber: Int,
        @Path("translator") translator: String
    ): TranslationResponse
}

// Retrofit client
object RetrofitClient {
    private const val BASE_URL = "https://api.alquran.cloud/v1/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val translationService: QuranTranslationService =
        retrofit.create(QuranTranslationService::class.java)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            val dynamicTheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

            MaterialTheme(
                colorScheme = dynamicTheme,
                typography = Typography(),
                content = {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "surah_list") {
                        composable(
                            route = "surah_list",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
                        ) {
                            SurahListScreen(navController, isDarkMode) { isDarkMode = it }
                        }
                        composable(
                            route = "verse_list/{surahId}",
                            enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                        ) { backStackEntry ->
                            val surahId = backStackEntry.arguments?.getString("surahId")?.toIntOrNull()
                            VerseListScreen(navController, surahId, isDarkMode) { isDarkMode = it }
                        }
                    }
                }
            )
        }
    }
}

// Data classes for Quran content
data class Surah(
    val id: Int,
    val name: String,
    val transliteration: String,
    val type: String,
    val total_verses: Int,
    val verses: List<Verse>
)

data class Verse(
    val id: Int,
    val text: String
)

data class Reciter(
    val id: Int,
    val name: String
)

// Function to load Quran data from assets
fun loadQuranData(context: Context): List<Surah> {
    return try {
        val inputStream = context.assets.open("quran.json")
        val reader = InputStreamReader(inputStream, "UTF-8")
        Gson().fromJson(reader, Array<Surah>::class.java).toList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val surahs = remember { loadQuranData(context) }
    val uthmanTahaFont = remember { FontFamily(Font(context.resources.getIdentifier("taha", "font", context.packageName))) }
    val vazirFont = remember { FontFamily(Font(context.resources.getIdentifier("vazir", "font", context.packageName))) }

    var searchQuery by remember { mutableStateOf("") }
    var isAscending by remember { mutableStateOf(true) }

    val filteredSurahs = remember(searchQuery, isAscending) {
        val filtered = if (searchQuery.isEmpty()) {
            surahs
        } else {
            surahs.filter { it.name.contains(searchQuery, ignoreCase = true) ||
                    it.transliteration.contains(searchQuery, ignoreCase = true) }
        }
        if (isAscending) filtered.sortedBy { it.id } else filtered.sortedByDescending { it.id }
    }

    val textColor = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(onClick = { onThemeChange(!isDarkMode) }) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.Brightness7
                                    else Icons.Default.Brightness4,
                                    contentDescription = "Toggle Theme"
                                )
                            }
                            IconButton(onClick = { isAscending = !isAscending }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort"
                                )
                            }
                        }
                        Text(
                            text = "آوای وحی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Right,
                            fontFamily = vazirFont,
                            color = textColor,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = {
                    Text(
                        text = "جستجوی نام سوره",
                        textAlign = TextAlign.Right,
                        style = TextStyle(fontFamily = vazirFont, color = textColor,textDirection = TextDirection.Content),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                textStyle = TextStyle(
                    textAlign = TextAlign.Right,
                    fontFamily = vazirFont,
                    textDirection = TextDirection.Content,
                    color = textColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Top
            ) {
                items(filteredSurahs) { surah ->
                    SurahItem(surah, navController, textColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseListScreen(
    navController: NavController,
    surahId: Int?,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val surahs = remember { loadQuranData(context) }
    val uthmanTahaFont = remember { FontFamily(Font(context.resources.getIdentifier("taha", "font", context.packageName))) }
    val vazirFont = remember { FontFamily(Font(context.resources.getIdentifier("vazir", "font", context.packageName))) }


    val selectedSurah = surahId?.let { id -> surahs.find { it.id == id } }
    var fontSize by remember { mutableStateOf(18.sp) }

    // Translation states
    var translators by remember { mutableStateOf<List<Translator>>(emptyList()) }
    var selectedTranslator by remember { mutableStateOf<Translator?>(null) }
    var translations by remember { mutableStateOf<List<TranslatedVerse>>(emptyList()) }
    var isTranslatorDropdownExpanded by remember { mutableStateOf(false) }

    // Audio states
    var isPlaying by remember { mutableStateOf(false) }
    var currentVerseIndex by remember { mutableStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val reciters = listOf(
        Reciter(1, "مشاري بن راشد العفاسي"),
        Reciter(2, "أبو بكر الشاطري"),
        Reciter(3, "ناصر القطامي")
    )
    var selectedReciter by remember { mutableStateOf(reciters.first()) }
    var isReciterDropdownExpanded by remember { mutableStateOf(false) }

    val textColor = if (isDarkMode) Color.White else Color.Black

    // Fetch translators
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.translationService.getTranslators()
            translators = response.data
            selectedTranslator = translators.firstOrNull()
        } catch (e: Exception) {
            // Handle error
        }
    }

    // Fetch translations when translator changes
    LaunchedEffect(selectedTranslator, surahId) {
        selectedTranslator?.let { translator ->
            try {
                val response = RetrofitClient.translationService.getTranslation(
                    surahId ?: 1,
                    translator.identifier
                )
                translations = response.data.ayahs
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Audio playback effect
    LaunchedEffect(isPlaying, currentVerseIndex, selectedReciter) {
        if (isPlaying) {
            val verse = selectedSurah?.verses?.getOrNull(currentVerseIndex) ?: return@LaunchedEffect
            val audioUrl = "https://quranaudio.pages.dev/${selectedReciter.id}/${selectedSurah.id}_${verse.id}.mp3"

            mediaPlayer?.stop()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    if (currentVerseIndex < (selectedSurah.verses.size - 1)) {
                        currentVerseIndex++
                    } else {
                        isPlaying = false
                    }
                }
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedSurah?.name ?: "Unknown Surah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = uthmanTahaFont,
                            color = textColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onThemeChange(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.Brightness7
                            else Icons.Default.Brightness4,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { fontSize = (fontSize.value - 2).sp }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size")
                        }
                        IconButton(onClick = { fontSize = (fontSize.value + 2).sp }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Font Size")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Translation selector
                ExposedDropdownMenuBox(
                    expanded = isTranslatorDropdownExpanded,
                    onExpandedChange = { isTranslatorDropdownExpanded = !isTranslatorDropdownExpanded }
                ) {
                    TextField(
                        value = selectedTranslator?.name ?: "انتخاب ترجمه",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTranslatorDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isTranslatorDropdownExpanded,
                        onDismissRequest = { isTranslatorDropdownExpanded = false }
                    ) {
                        translators.forEach { translator ->
                            DropdownMenuItem(
                                text = { Text(translator.name) },
                                onClick = {
                                    selectedTranslator = translator
                                    isTranslatorDropdownExpanded = false
                                    scope.launch {
                                        try {
                                            val response = RetrofitClient.translationService.getTranslation(
                                                surahId ?: 1,
                                                translator.identifier
                                            )
                                            translations = response.data.ayahs
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Audio controls
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reciter selection dropdown
                        ExposedDropdownMenuBox(
                            expanded = isReciterDropdownExpanded,
                            onExpandedChange = { isReciterDropdownExpanded = !isReciterDropdownExpanded }
                        ) {
                            Text(
                                text = selectedReciter.name,
                                modifier = Modifier
                                    .menuAnchor()
                                    .clickable { isReciterDropdownExpanded = true },
                                color = textColor
                            )
                            DropdownMenu(
                                expanded = isReciterDropdownExpanded,
                                onDismissRequest = { isReciterDropdownExpanded = false }
                            ) {
                                reciters.forEach { reciter ->
                                    DropdownMenuItem(
                                        text = { Text(text = reciter.name) },
                                        onClick = {
                                            selectedReciter = reciter
                                            isReciterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Play/Pause button
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    isPlaying = false
                                } else {
                                    isPlaying = true
                                    currentVerseIndex = 0
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            selectedSurah?.verses?.forEachIndexed { index, verse ->
                item {
                    VerseItemWithTranslation(
                        verse = verse,
                        translation = translations.getOrNull(index)?.text,
                        fontSize = fontSize,
                        fontFamily = uthmanTahaFont,
                        isCurrentVerse = currentVerseIndex == index,
                        textColor = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun SurahItem(surah: Surah, navController: NavController, textColor: Color) {
    val context = LocalContext.current
    val uthmanTahaFont = remember { FontFamily(Font(context.resources.getIdentifier("taha", "font", context.packageName))) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate("verse_list/${surah.id}")
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = surah.name,
            fontSize = 16.sp,
            fontFamily = uthmanTahaFont,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = textColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = surah.transliteration,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun VerseItemWithTranslation(
    verse: Verse,
    translation: String?,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    isCurrentVerse: Boolean,
    textColor: Color
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Share Icon
        IconButton(
            onClick = {
                val shareText = buildString {
                    append(verse.text)
                    translation?.let { translatedText ->
                        append("\n\n")
                        append(translatedText)
                    }
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                context.startActivity(Intent.createChooser(shareIntent, "اشتراک گذاری آیه"))
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share verse",
                tint = textColor.copy(alpha = 0.4f)
            )
        }

        // Verse Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            // Arabic verse
            Text(
                text = verse.text,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontStyle = FontStyle.Normal,
                lineHeight = fontSize.value * 1.5.sp,
                textAlign = TextAlign.Right,
                style = TextStyle(textDirection = TextDirection.Content),
                color = if (isCurrentVerse) Color.Blue else textColor,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation
            translation?.let { translatedText ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translatedText,
                    fontSize = fontSize * 0.8f,
                    fontStyle = FontStyle.Normal,
                    lineHeight = fontSize.value * 1.2.sp,
                    textAlign = TextAlign.Right,
                    style = TextStyle(textDirection = TextDirection.Content),
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}