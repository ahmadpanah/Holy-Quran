package com.Hamp.HolyQuran

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // State for managing dark mode
            var isDarkMode by remember { mutableStateOf(false) }

            // Dynamic theme based on dark mode state
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

// Function to load Quran data
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

// Data classes
data class Surah(
    val id: Int,
    val name: String,
    val transliteration: String,
    val type: String,
    val total_verses: Int,
    val verses: List<Verse>
)

data class Verse(
    val id: Int, // Verse number (not displayed in UI)
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val surahs = remember { loadQuranData(context) } // Cache Quran data
    val uthmanTahaFont = remember { FontFamily(Font(context.resources.getIdentifier("taha", "font", context.packageName))) }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Sorting state (true = ascending, false = descending)
    var isAscending by remember { mutableStateOf(true) }

    // Filtered and sorted surahs
    val filteredSurahs = remember(searchQuery, isAscending) {
        val filtered = if (searchQuery.isEmpty()) {
            surahs
        } else {
            surahs.filter { it.name.contains(searchQuery, ignoreCase = true) || it.transliteration.contains(searchQuery, ignoreCase = true) }
        }
        if (isAscending) filtered.sortedBy { it.id } else filtered.sortedByDescending { it.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, // Align items to opposite ends
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icons on the left side
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(onClick = { onThemeChange(!isDarkMode) }) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                    contentDescription = "Toggle Theme"
                                )
                            }
                            IconButton(onClick = { isAscending = !isAscending }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Ascending/Descending"
                                )
                            }
                        }

                        // Title on the right side
                        Text(
                            text = "آوای وحی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Right,
                            fontFamily = uthmanTahaFont,
                            modifier = Modifier.padding(end = 16.dp) // Add padding for spacing
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
            // Search Bar (RTL-aligned and centered)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = {
                    Text(
                        text = "جستجوی نام سوره",
                        textAlign = TextAlign.Right, // Align placeholder text to the right
                        style = TextStyle(fontFamily = uthmanTahaFont),
                        modifier = Modifier.fillMaxWidth() // Ensure full width for RTL alignment
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* Handle search action */ }),
                textStyle = TextStyle(
                    textAlign = TextAlign.Right, // Align input text to the right
                    fontFamily = uthmanTahaFont
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Grid of Surahs
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2-column grid
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center, // Centerize grid items
                verticalArrangement = Arrangement.Top
            ) {
                items(filteredSurahs) { surah ->
                    SurahItem(surah, navController)
                }
            }
        }
    }
}

@Composable
fun SurahItem(surah: Surah, navController: NavController) {
    val context = LocalContext.current
    val uthmanTahaFont = remember { FontFamily(Font(context.resources.getIdentifier("taha", "font", context.packageName))) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Navigate to the verse list screen for the selected surah
                navController.navigate("verse_list/${surah.id}")
            },
        horizontalAlignment = Alignment.CenterHorizontally // Centerize each item
    ) {
        Text(
            text = surah.name,
            fontSize = 16.sp,
            fontFamily = uthmanTahaFont,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, // Centerize text
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = surah.transliteration,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center, // Centerize text
            color = Color.Gray
        )
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
    val surahs = remember { loadQuranData(context) } // Cache Quran data
    val uthmanTahaFont = remember { FontFamily(Font(context.resources.getIdentifier("taha", "font", context.packageName))) }

    // Find the selected surah safely
    val selectedSurah = surahId?.let { id -> surahs.find { it.id == id } }

    // Font size state for verses
    var fontSize by remember { mutableStateOf(18.sp) }

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
                            fontFamily = uthmanTahaFont
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate back to the previous screen
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onThemeChange(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            selectedSurah?.verses?.forEach { verse ->
                item {
                    VerseItem(verse, fontSize, uthmanTahaFont)
                }
            }
        }
    }
}

@Composable
fun VerseItem(verse: Verse, fontSize: TextUnit, fontFamily: FontFamily) {
    Text(
        text = verse.text,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontStyle = FontStyle.Normal,
        lineHeight = fontSize.value * 1.5.sp,
        textAlign = TextAlign.Right, // Align text to the right
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}