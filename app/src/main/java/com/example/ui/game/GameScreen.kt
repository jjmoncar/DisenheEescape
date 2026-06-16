package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.LevelProgress
import com.example.game.*
import com.example.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- Custom Handdrawn theme colors adapted to "Immersive UI" Neo-Brutalist Theme ---
val SketchCream = Color(0xFFF4F1EA)      // Immersive background warm greyish-cream page
val SketchCoal = Color(0xFF2D2D2D)       // Immersive slate charcoal dark gray
val SketchBlueLine = Color(0xFFC0D2F0)   // Light notebook paper ruler line
val SketchMarginRed = Color(0xFFF99D9D)  // Vertical left margin red pencil line
val SketchGreenSpring = Color(0xFF10B981)// Emerald green bouncer
val SketchRedSpike = Color(0xFFFF6B6B)   // Coral red hazard / brush
val SketchActiveBlue = Color(0xFF4F46E5) // Indigo blue indicator crayon
val SketchCardInks = Color(0xFFFFFFFF)   // Plain high contrast white cards

/**
 * Conceptual "Sketchbook CSS Stylesheet" for Jetpack Compose.
 * Maps traditional CSS formatting rules (fonts, margins, shadows, grid layouts)
 * into typed Material 3 Compose objects and style properties.
 */
object SketchbookCSS {
    // --- CSS Fonts & Typography Configurations ---
    val fontCursive = FontFamily.Cursive        // Handcopied beautiful cursive script (headings, Victory overlays)
    val fontTypewriter = FontFamily.Monospace   // Block draftsman mono lettering (technical coordinates, level indicators)
    val fontPrint = FontFamily.Serif            // Clean educational prints / book layout serif (secondary info)
    
    // --- CSS Borders & Styling Attributes ---
    val borderThickness = 2.dp                  // Thick graphite-inked frame border
    val borderColor = SketchCoal
    val cardCornerRadius = 12.dp                // Smooth card geometry rounded corner radius
    
    // --- CSS Shadows (Neo-Brutalist 3D style) ---
    val shadowOffset = 3.dp
    val shadowColor = SketchCoal
}

// Custom Neo-Brutalist shadow modifier
fun Modifier.neoShadow(
    color: Color = SketchCoal,
    offset: Float = 4f, // in dp
    cornerRadius: Float = 16f // in dp
) = this.drawBehind {
    val offsetPx = offset.dp.toPx()
    val radiusPx = cornerRadius.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(offsetPx, offsetPx),
        size = this.size,
        cornerRadius = CornerRadius(radiusPx, radiusPx)
    )
}

@Composable
fun GameApp(viewModel: GameViewModel) {
    val levelProgress by viewModel.levelProgressList.collectAsState()

    // Single ticker for simulation game loop
    LaunchedEffect(viewModel.isSimulating) {
        var lastTime = System.nanoTime()
        while (viewModel.isSimulating) {
            val now = System.nanoTime()
            val dt = (now - lastTime).toFloat() / 1_000_000_000f
            lastTime = now

            // Bound dt to prevent huge jumps from system lag
            val boundedDt = dt.coerceIn(0.001f, 0.033f)
            viewModel.tick(boundedDt)
            delay(15) // Approx 60fps simulation pacing
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SketchCream
    ) {
        Crossfade(targetState = viewModel.gameState, label = "ScreenTransition") { state ->
            when (state) {
                GameState.MainMenu -> MainMenuScreen(
                    onStartClicked = { viewModel.navigateToSelector() },
                    onResetProgressClicked = { viewModel.resetAllProgress() },
                    onHelpClicked = { viewModel.showTutorial() }
                )
                GameState.LevelSelector -> LevelSelectorScreen(
                    progressList = levelProgress,
                    onBackClicked = { viewModel.navigateToMainMenu() },
                    onLevelSelected = { lvl -> viewModel.startPlayingLevel(lvl) },
                    onHelpClicked = { viewModel.showTutorial() }
                )
                is GameState.Playing -> PlayingScreen(
                    viewModel = viewModel,
                    level = state.level,
                    onBackToSelectorClicked = { viewModel.navigateToSelector() }
                )
            }
        }
    }

    if (viewModel.showTutorialDialog) {
        TutorialDialog(
            onDismiss = { viewModel.dismissTutorial() }
        )
    }
}

@Composable
fun MainMenuScreen(
    onStartClicked: () -> Unit,
    onResetProgressClicked: () -> Unit,
    onHelpClicked: () -> Unit
) {
    var showConfirmReset by remember { mutableStateOf(false) }
    var showPrivacyTermsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw some background guidelines just for sketchbook depth
        NotebookLinedBackground()

        // Top right helper question mark button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .padding(end = 4.dp, bottom = 4.dp)
        ) {
            IconButton(
                onClick = onHelpClicked,
                modifier = Modifier
                    .size(40.dp)
                    .neoShadow(cornerRadius = 10f, offset = 3f)
                    .background(SketchCardInks, RoundedCornerShape(10.dp))
                    .border(2.dp, SketchCoal, RoundedCornerShape(10.dp))
                    .testTag("menu_help_button")
            ) {
                Text(
                    text = "?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SketchbookCSS.fontTypewriter,
                    color = SketchCoal
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Sketched Title Shield adapted to modern Neo-Brutalist style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .padding(end = 6.dp, bottom = 6.dp) // space for shadow
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SketchCardInks),
                    border = BorderStroke(2.dp, SketchCoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .rotate(-2f)
                        .neoShadow(cornerRadius = 16f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = SketchCoal,
                            textAlign = TextAlign.Center,
                            fontFamily = SketchbookCSS.fontCursive
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SketchCoal.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            fontFamily = SketchbookCSS.fontTypewriter
                        )
                    }
                }
            }

            // Central Animated Character Illustration
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "CharacterIdle")
                val floatOffsetY by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "idleFloat"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * 0.35f
                    val center = Offset(size.width / 2f, size.height / 2f + floatOffsetY)

                    // Sketched circle
                    drawCircle(
                        color = SketchCoal,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Pencil shading hatch lines inside character
                    for (i in -4..4) {
                        val offset = i * 6f
                        drawLine(
                            color = SketchCoal.copy(alpha = 0.3f),
                            start = Offset(center.x - radius + 5f + offset, center.y + offset),
                            end = Offset(center.x + offset, center.y - radius + 5f + offset),
                            strokeWidth = 2f
                        )
                    }

                    // Face: huge happy round eyes
                    drawCircle(
                        color = SketchCoal,
                        radius = 6f,
                        center = Offset(center.x - 12f, center.y - 10f)
                    )
                    drawCircle(
                        color = SketchCoal,
                        radius = 6f,
                        center = Offset(center.x + 12f, center.y - 10f)
                    )
                    // Smile
                    drawArc(
                        color = SketchCoal,
                        startAngle = 10f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(center.x - 12f, center.y),
                        size = Size(24f, 16f),
                        style = Stroke(width = 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main CTA (Play / JUGAR)
            SketchButton(
                text = stringResource(R.string.start_adventure),
                icon = Icons.Filled.PlayArrow,
                onClick = onStartClicked,
                testTag = "start_adventure_button",
                colorAccent = SketchActiveBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Records Button
            TextButton(
                onClick = { showConfirmReset = true },
                modifier = Modifier.testTag("reset_all_progress_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.clear_all),
                    tint = SketchCoal.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.clear_ink_history),
                    color = SketchCoal.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Privacy and Terms Button
            TextButton(
                onClick = { showPrivacyTermsDialog = true },
                modifier = Modifier.testTag("privacy_terms_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.privacy_terms),
                    tint = SketchCoal.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.privacy_terms),
                    color = SketchCoal.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    if (showConfirmReset) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showConfirmReset = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .padding(end = 4.dp, bottom = 4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SketchCardInks),
                    border = BorderStroke(SketchbookCSS.borderThickness, SketchCoal),
                    shape = RoundedCornerShape(SketchbookCSS.cardCornerRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(cornerRadius = 12f, offset = 5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = stringResource(R.string.reset_progress_title).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = SketchbookCSS.fontCursive,
                            color = SketchCoal,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Icon to emphasize caution / sketch pencil style
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.reset_progress_title),
                            tint = SketchRedSpike,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Text description
                        Text(
                            text = stringResource(R.string.reset_progress_text),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SketchbookCSS.fontPrint,
                            color = SketchCoal.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // CANCEL (Primary UX to keep progress, easy to hit)
                            Button(
                                onClick = { showConfirmReset = false },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("reset_cancel_button")
                                    .neoShadow(cornerRadius = 8f, offset = 2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SketchCream,
                                    contentColor = SketchCoal
                                ),
                                border = BorderStroke(2.dp, SketchCoal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SketchbookCSS.fontTypewriter,
                                    fontSize = 13.sp
                                )
                            }
                            
                            // CONFIRM DELETE
                            Button(
                                onClick = {
                                    showConfirmReset = false
                                    onResetProgressClicked()
                                },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("reset_confirm_button")
                                    .neoShadow(cornerRadius = 8f, offset = 2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SketchRedSpike,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(2.dp, SketchCoal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.delete_all),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SketchbookCSS.fontTypewriter,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyTermsDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyTermsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.privacy_terms),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    fontFamily = SketchbookCSS.fontCursive,
                    color = SketchCoal
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SketchCoal,
                        fontFamily = SketchbookCSS.fontTypewriter
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.privacy_block_1),
                        fontSize = 12.sp,
                        color = SketchCoal.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_block_2),
                        fontSize = 12.sp,
                        color = SketchCoal.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_block_3),
                        fontSize = 12.sp,
                        color = SketchCoal.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_block_4),
                        fontSize = 12.sp,
                        color = SketchCoal.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_block_5),
                        fontSize = 12.sp,
                        color = SketchCoal.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyTermsDialog = false }
                ) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.Bold, color = SketchActiveBlue)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectorScreen(
    progressList: List<LevelProgress>,
    onBackClicked: () -> Unit,
    onLevelSelected: (Level) -> Unit,
    onHelpClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.select_level),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = SketchbookCSS.fontCursive,
                        color = SketchCoal,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            onClick = onBackClicked,
                            modifier = Modifier
                                .size(36.dp)
                                .neoShadow(cornerRadius = 8f, offset = 2f)
                                .background(SketchCardInks, RoundedCornerShape(8.dp))
                                .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                .testTag("selector_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = SketchCoal,
                                modifier = Modifier.size(20.dp)
                              )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            onClick = onHelpClicked,
                            modifier = Modifier
                                .size(36.dp)
                                .neoShadow(cornerRadius = 8f, offset = 2f)
                                .background(SketchCardInks, RoundedCornerShape(8.dp))
                                .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                .testTag("selector_help_button")
                        ) {
                            Text(
                                text = "?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = SketchbookCSS.fontTypewriter,
                                color = SketchCoal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SketchCream),
                modifier = Modifier.border(width = 1.dp, color = SketchCoal.copy(alpha = 0.1f))
            )
        },
        containerColor = SketchCream
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NotebookLinedBackground()

            // If empty, prompt initializer
            if (progressList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SketchCoal)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 75.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(LevelManager.levels) { level ->
                        val progress = progressList.getOrNull(level.id - 1)
                        val isUnlocked = progress?.unlocked == true

                        LevelItemNode(
                            level = level,
                            progress = progress,
                            isUnlocked = isUnlocked,
                            onClick = {
                                if (isUnlocked) {
                                    onLevelSelected(level)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelItemNode(
    level: Level,
    progress: LevelProgress?,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val containerCol = if (isUnlocked) {
        if ((progress?.stars ?: 0) == 3) Color(0xFFFFD700) else SketchCardInks
    } else {
        SketchCream.copy(alpha = 0.5f)
    }
    val borderCol = if (isUnlocked) SketchCoal else SketchCoal.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = isUnlocked, onClick = onClick)
                .then(if (isUnlocked) Modifier.neoShadow(cornerRadius = 12f) else Modifier)
                .testTag("level_node_${level.id}"),
            colors = CardDefaults.cardColors(containerColor = containerCol),
            border = BorderStroke(2.dp, borderCol),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!isUnlocked) {
                    // Locked lock outline
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.locked),
                        tint = SketchCoal.copy(alpha = 0.25f),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 6.dp, top = 8.dp)
                    ) {
                        // Level Number
                        Text(
                            text = "${level.id}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = SketchCoal,
                            fontFamily = FontFamily.Monospace
                        )

                        // Draw hand sketches stars indicators
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val starsCount = progress?.stars ?: 0
                            for (i in 1..3) {
                                val active = i <= starsCount
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.star),
                                    tint = if (active) SketchCoal else SketchCoal.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .size(13.dp)
                                        .padding(horizontal = 0.5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper extension function to extract Activity from Context safely
private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun PlayingScreen(
    viewModel: GameViewModel,
    level: Level,
    onBackToSelectorClicked: () -> Unit
) {
    val context = LocalContext.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        canvasSize = Size(widthPx, heightPx)

        // Game arena with pointer inputs for drawing drawing lines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(level.id, viewModel.showVictoryScreen, viewModel.showFailureScreen) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val normalizedX = (offset.x / widthPx) * 100f
                            val normalizedY = (offset.y / heightPx) * 100f
                            viewModel.onDrawStart(Offset(normalizedX, normalizedY))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val normalizedX = (change.position.x / widthPx) * 100f
                            val normalizedY = (change.position.y / heightPx) * 100f
                            viewModel.onDrawMove(Offset(normalizedX, normalizedY))
                        },
                        onDragEnd = {
                            viewModel.onDrawEnd()
                        }
                    )
                }
                .testTag("game_drawing_canvas")
        ) {
            // Render beautiful hand-sketched board
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background ruled lines
                drawRuledNotebook()

                // Static objects (AABB shelves)
                for (obj in level.objects) {
                    drawGameObject(obj)
                }

                // Player drawings (crayon lines)
                for (line in viewModel.drawnLines) {
                    drawSketchyLine(line, SketchCoal)
                }

                // Currently active drawing
                if (viewModel.currentLine.isNotEmpty()) {
                    drawSketchyLine(viewModel.currentLine.toList(), SketchActiveBlue)
                }

                // Exit Portal Door
                drawExitPortal(Offset(level.doorX, level.doorY))

                // The Protagonist Doodled ball with expressions based on engine physics states
                drawProtagonist(
                    character = viewModel.character,
                    hint = level.hint
                )
            }

            // HUD overlays & Action indicators
            PlayingHUD(
                viewModel = viewModel,
                level = level,
                onBackClicked = onBackToSelectorClicked
            )
        }

        // Overlays
        AnimatedVisibility(
            visible = viewModel.showVictoryScreen,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            VictoryOverlay(
                levelNum = level.id,
                levelName = level.localizedName(),
                stars = viewModel.calculatedStars,
                timeSec = viewModel.finalTimeSec,
                inkLeftRatio = viewModel.remainingInk / viewModel.maxInkForLevel,
                onReplayClicked = {
                    viewModel.fullResetLevel()
                },
                onNextClicked = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        com.example.AdMobManager.showAdIfReady(activity) {
                            viewModel.playNextLevel()
                        }
                    } else {
                        viewModel.playNextLevel()
                    }
                },
                onHomeClicked = onBackToSelectorClicked
            )
        }

        AnimatedVisibility(
            visible = viewModel.showFailureScreen,
            enter = fadeIn(animationSpec = tween(400)) + scaleIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            FailureOverlay(
                levelName = level.localizedName(),
                onRetryClicked = {
                    // RETRY ONLY THE BALL, KEEP LINES AS IS! Extremely good puzzle feature!
                    viewModel.retryCharacterOnly()
                },
                onWipeResetClicked = {
                    // COMPLETE FULL LEVEL WIPE RESET
                    viewModel.fullResetLevel()
                },
                onHomeClicked = onBackToSelectorClicked
            )
        }
    }
}

@Composable
fun PlayingHUD(
    viewModel: GameViewModel,
    level: Level,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // TOP HUD BAR with Neo-Brutalist Shadow and crisp border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .padding(end = 4.dp, bottom = 4.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(cornerRadius = 12f)
                    .testTag("playing_hud_top_bar"),
                colors = CardDefaults.cardColors(containerColor = SketchCream),
                border = BorderStroke(2.dp, SketchCoal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back and level name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp, bottom = 4.dp)
                        ) {
                            IconButton(
                                onClick = onBackClicked,
                                modifier = Modifier
                                    .size(36.dp)
                                    .neoShadow(cornerRadius = 8f, offset = 2f)
                                    .background(SketchCardInks, RoundedCornerShape(8.dp))
                                    .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                    .testTag("in_game_exit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = SketchCoal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "${stringResource(R.string.level_info).substringBefore("%1").trim()} ${level.id}",
                                fontWeight = FontWeight.Black,
                                fontFamily = SketchbookCSS.fontTypewriter,
                                fontSize = 11.sp,
                                color = SketchCoal.copy(alpha = 0.6f)
                            )
                            Text(
                                text = level.localizedName(),
                                fontWeight = FontWeight.Black,
                                fontFamily = SketchbookCSS.fontCursive,
                                fontSize = 18.sp,
                                color = SketchCoal
                            )
                        }
                    }

                    // Crayon Ink bottle indicator and Helper question mark button side-by-side
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            val inkRatio = viewModel.remainingInk / viewModel.maxInkForLevel
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.ink_remaining),
                                tint = if (inkRatio > 0.3f) SketchCoal else SketchRedSpike,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            // Simulated sliding crayon shape
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "${stringResource(R.string.ink_remaining).substringBefore(":")} ${(inkRatio * 100f).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SketchCoal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(75.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SketchCoal.copy(alpha = 0.12f))
                                        .border(1.dp, SketchCoal, RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(inkRatio)
                                            .background(SketchActiveBlue)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Help question mark button inside the level!
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp, bottom = 4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.showTutorial() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .neoShadow(cornerRadius = 8f, offset = 2f)
                                    .background(SketchCardInks, RoundedCornerShape(8.dp))
                                    .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                    .testTag("in_game_help_button")
                            ) {
                                Text(
                                    text = "?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SketchbookCSS.fontTypewriter,
                                    color = SketchCoal
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // BOTTOM MENU BUTTONS FOR LEVEL CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hint Paper Slip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
                    .padding(end = 4.dp, bottom = 4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SketchCardInks),
                    border = BorderStroke(2.dp, SketchCoal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(cornerRadius = 8f, offset = 3f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.idea),
                            tint = SketchCoal.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = level.localizedHint(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SketchCoal,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Quick Control Circular icons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset ONLY standard ball (highly requested feature!)
                ControlIconButton(
                    icon = Icons.Default.Refresh,
                    tooltipPath = stringResource(R.string.reintentar_caida_tooltip),
                    testTag = "retry_character_only_button",
                    onClick = { viewModel.retryCharacterOnly() }
                )

                // Wipe complete level (wipe ink drawings + reset ball)
                ControlIconButton(
                    icon = Icons.Default.Delete,
                    tooltipPath = stringResource(R.string.borrar_dibujo_tooltip),
                    testTag = "clear_drawing_button",
                    onClick = { viewModel.fullResetLevel() }
                )
            }
        }
    }
}

@Composable
fun ControlIconButton(
    icon: ImageVector,
    tooltipPath: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
    ) {
        IconButton(
            modifier = Modifier
                .size(44.dp)
                .neoShadow(cornerRadius = 8f, offset = 3f)
                .background(SketchCream, RoundedCornerShape(8.dp))
                .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                .testTag(testTag),
            onClick = onClick
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltipPath,
                tint = SketchCoal
            )
        }
    }
}

@Composable
fun VictoryOverlay(
    levelNum: Int,
    levelName: String,
    stars: Int,
    timeSec: Float,
    inkLeftRatio: Float,
    onReplayClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onHomeClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SketchCoal.copy(alpha = 0.5f))
            .clickable(enabled = false) {}, // Intercept click tunneling
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp)
                .padding(end = 8.dp, bottom = 8.dp) // space for shadow
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SketchCream),
                border = BorderStroke(2.dp, SketchCoal),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(1.5f)
                    .neoShadow(cornerRadius = 20f, offset = 8f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.escaped),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = SketchbookCSS.fontCursive,
                        color = SketchCoal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.level_info, levelNum, levelName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SketchCoal.copy(alpha = 0.6f),
                        fontFamily = SketchbookCSS.fontTypewriter,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Render sketchy big stars
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..3) {
                            val active = i <= stars
                            val scale = if (active) 1.2f else 1.0f
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = stringResource(R.string.star),
                                tint = if (active) Color(0xFFFFD700) else SketchCoal.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(scale)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // Ink efficiency and timing info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SketchCardInks),
                        border = BorderStroke(2.dp, SketchCoal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.ink_remaining), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SketchCoal)
                                Text(text = "${(inkLeftRatio * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = SketchCoal)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.time), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SketchCoal)
                                Text(text = String.format("%.2fs", timeSec), fontSize = 13.sp, fontWeight = FontWeight.Black, color = SketchCoal)
                            }
                        }
                    }

                    // Action controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Next Level Button (Primary) with Neo style
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp, bottom = 4.dp)
                        ) {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .neoShadow(cornerRadius = 8f, offset = 4f, color = SketchCoal)
                                    .testTag("victory_next_level_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SketchActiveBlue),
                                border = BorderStroke(2.dp, SketchCoal),
                                shape = RoundedCornerShape(8.dp),
                                onClick = onNextClicked
                            ) {
                                Text(
                                    text = if (levelNum < 50) stringResource(R.string.next_sketch) else stringResource(R.string.main_menu),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Replay
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 3.dp, bottom = 3.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .neoShadow(cornerRadius = 8f, offset = 3f)
                                        .testTag("victory_replay_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SketchCoal,
                                        containerColor = SketchCream
                                    ),
                                    border = BorderStroke(2.dp, SketchCoal),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = onReplayClicked
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.restart), tint = SketchCoal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.replay), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            // Home
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 3.dp, bottom = 3.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .neoShadow(cornerRadius = 8f, offset = 3f)
                                        .testTag("victory_home_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SketchCoal,
                                        containerColor = SketchCream
                                    ),
                                    border = BorderStroke(2.dp, SketchCoal),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = onHomeClicked
                                ) {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.main_menu), tint = SketchCoal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.selector), fontSize = 12.sp, fontWeight = FontWeight.Black)
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
fun FailureOverlay(
    levelName: String,
    onRetryClicked: () -> Unit,
    onWipeResetClicked: () -> Unit,
    onHomeClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SketchRedSpike.copy(alpha = 0.20f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp)
                .padding(end = 8.dp, bottom = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SketchCream),
                border = BorderStroke(2.dp, SketchRedSpike),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(-1f)
                    .neoShadow(cornerRadius = 20f, offset = 8f, color = SketchRedSpike)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.sheet_torn),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = SketchbookCSS.fontCursive,
                        color = SketchRedSpike,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = levelName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SketchCoal.copy(alpha = 0.5f),
                        fontFamily = SketchbookCSS.fontTypewriter,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Angry doodle illustration
                    Canvas(modifier = Modifier.size(70.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        // Draw sketchy cracked circles
                        drawCircle(SketchRedSpike, radius = 30f, center = Offset(cx, cy), style = Stroke(width = 3.dp.toPx()))
                        // Crack line
                        drawLine(SketchRedSpike, start = Offset(cx - 15f, cy - 30f), end = Offset(cx + 5f, cy + 5f), strokeWidth = 3.dp.toPx())
                        drawLine(SketchRedSpike, start = Offset(cx + 5f, cy + 5f), end = Offset(cx - 10f, cy + 30f), strokeWidth = 3.dp.toPx())

                        // Dead eyes (XX)
                        drawLine(SketchRedSpike, start = Offset(cx - 18f, cy - 15f), end = Offset(cx - 8f, cy - 5f), strokeWidth = 3f)
                        drawLine(SketchRedSpike, start = Offset(cx - 8f, cy - 15f), end = Offset(cx - 18f, cy - 5f), strokeWidth = 3f)

                        drawLine(SketchRedSpike, start = Offset(cx + 8f, cy - 15f), end = Offset(cx + 18f, cy - 5f), strokeWidth = 3f)
                        drawLine(SketchRedSpike, start = Offset(cx + 18f, cy - 15f), end = Offset(cx + 8f, cy - 5f), strokeWidth = 3f)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Retry Ball ONLY (Primary action - extremely fast loop)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp, bottom = 4.dp)
                        ) {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .neoShadow(cornerRadius = 8f, offset = 4f, color = SketchCoal)
                                    .testTag("failure_intent_retry_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SketchCoal),
                                border = BorderStroke(2.dp, SketchCoal),
                                shape = RoundedCornerShape(8.dp),
                                onClick = onRetryClicked
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.restart), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.retry_fall_keep_ink),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Wipe & Clear everything
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 3.dp, bottom = 3.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .neoShadow(cornerRadius = 8f, offset = 3f)
                                        .testTag("failure_wipe_reset_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SketchCoal,
                                        containerColor = SketchCream
                                    ),
                                    border = BorderStroke(2.dp, SketchCoal),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = onWipeResetClicked
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.borrar_dibujo_tooltip), tint = SketchCoal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.clear_all), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            // Home Selector
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 3.dp, bottom = 3.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .neoShadow(cornerRadius = 8f, offset = 3f)
                                        .testTag("failure_home_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SketchCoal,
                                        containerColor = SketchCream
                                    ),
                                    border = BorderStroke(2.dp, SketchCoal),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = onHomeClicked
                                ) {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.main_menu), tint = SketchCoal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.selector), fontSize = 12.sp, fontWeight = FontWeight.Black)
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
fun SketchButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    colorAccent: Color = SketchCoal
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(containerColor = colorAccent),
        border = BorderStroke(3.dp, SketchCoal),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@Composable
fun NotebookLinedBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRuledNotebook()
    }
}

// --- Extended DrawScope extensions for Sketch aesthetic rendering ---

fun DrawScope.drawRuledNotebook() {
    // 1. Fill entire sheet with Cream paper color
    drawRect(color = SketchCream)

    val w = size.width
    val h = size.height

    // 2. Parallel blue ruled horizontal and vertical lines (Math Grid / Graph Paper look)
    val gridStep = 32.dp.toPx()
    
    // Vertical grid lines (subtle blueprint grid)
    var cx = gridStep
    while (cx < w) {
        drawLine(
            color = SketchBlueLine.copy(alpha = 0.35f),
            start = Offset(cx, 0f),
            end = Offset(cx, h),
            strokeWidth = 1.dp.toPx()
        )
        cx += gridStep
    }

    // Horizontal grid lines
    var cy = gridStep
    while (cy < h) {
        drawLine(
            color = SketchBlueLine,
            start = Offset(0f, cy),
            end = Offset(w, cy),
            strokeWidth = 1.5.dp.toPx()
        )
        cy += gridStep
    }

    // 3. Binder margin red pencil vertical rule
    val redMarginLabelX = 54.dp.toPx()
    drawLine(
        color = SketchMarginRed,
        start = Offset(redMarginLabelX, 0f),
        end = Offset(redMarginLabelX, h),
        strokeWidth = 2.dp.toPx()
    )

    // 4. DETERMINISTIC RECYCLED PAPER TEXTURIZATION SHADER
    // An LCG (Linear Congruential Generator) with fixed seed allows drawing organic details
    // that remain 100% static across all recompositions (no screen static flickering!)
    var seed = 777666555L
    fun rand(): Float {
        seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL
        return seed.toFloat() / 2147483647f
    }

    // A. Corner vignette shadows (aged yellowing paper border bleed)
    // Draw 4 radial dark cream gradients or soft large overlays at borders
    val cornerRadiusBg = 150.dp.toPx()
    drawCircle(color = Color(0xFFECE6D9), radius = cornerRadiusBg, center = Offset(0f, 0f), alpha = 0.35f)
    drawCircle(color = Color(0xFFECE6D9), radius = cornerRadiusBg, center = Offset(w, 0f), alpha = 0.35f)
    drawCircle(color = Color(0xFFECE6D9), radius = cornerRadiusBg, center = Offset(0f, h), alpha = 0.35f)
    drawCircle(color = Color(0xFFECE6D9), radius = cornerRadiusBg, center = Offset(w, h), alpha = 0.35f)

    // B. Coffee / Tea Spill Ring Watermark
    // Adds incredible authentic high-fidelity "sketchbook workspace" realism!
    val ringX = redMarginLabelX + 110.dp.toPx()
    val ringY = h - 160.dp.toPx()
    val ringR = 55.dp.toPx()
    drawCircle(
        color = Color(0xFFDCD2BE),
        radius = ringR,
        center = Offset(ringX, ringY),
        alpha = 0.18f,
        style = Stroke(width = 3.dp.toPx())
    )
    drawCircle(
        color = Color(0xFFD1C5AD),
        radius = ringR - 3.dp.toPx(),
        center = Offset(ringX - 2.dp.toPx(), ringY + 1.dp.toPx()),
        alpha = 0.10f,
        style = Stroke(width = 1.dp.toPx())
    )

    // C. Microscopic Graphite Pulp Specks
    for (i in 0 until 50) {
        val px = rand() * w
        val py = rand() * h
        val radius = 0.4.dp.toPx() + rand() * 0.8.dp.toPx()
        drawCircle(
            color = SketchCoal,
            radius = radius,
            center = Offset(px, py),
            alpha = 0.08f + rand() * 0.1f
        )
    }

    // D. Recycled Paper Cotton/Pulp Curved Fibers
    for (i in 0 until 40) {
        val px = rand() * w
        val py = rand() * h
        val length = 3.dp.toPx() + rand() * 6.dp.toPx()
        val angle = rand() * 3.14159f * 2f
        val dx = cos(angle) * length
        val dy = sin(angle) * length
        drawLine(
            color = SketchCoal,
            start = Offset(px, py),
            end = Offset(px + dx, py + dy),
            strokeWidth = 0.5.dp.toPx(),
            alpha = 0.06f + rand() * 0.08f
        )
    }

    // E. Carbon/Lead Pencil Smudges
    for (i in 0 until 12) {
        val px = rand() * w
        val py = rand() * h
        val radius = 10.dp.toPx() + rand() * 30.dp.toPx()
        drawCircle(
            color = SketchCoal,
            radius = radius,
            center = Offset(px, py),
            alpha = 0.015f
        )
    }

    // 5. Notebook ring binding spirals on the left
    val ringSpacing = 40.dp.toPx()
    var ry = 20.dp.toPx()
    while (ry < h) {
        drawArc(
            color = SketchCoal.copy(alpha = 0.28f),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-12.dp.toPx(), ry - 8.dp.toPx()),
            size = Size(20.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )
        ry += ringSpacing
    }
}

fun DrawScope.drawGameObject(obj: GameObject) {
    val pixelX = (obj.x / 100f) * size.width
    val pixelY = (obj.y / 100f) * size.height
    val pixelW = (obj.width / 100f) * size.width
    val pixelH = (obj.height / 100f) * size.height

    when (obj.type) {
        ObjectType.BOX_PLATFORM -> {
            // Sketched rect container with rounded paper look
            drawRoundRect(
                color = SketchCoal,
                topLeft = Offset(pixelX, pixelY),
                size = Size(pixelW, pixelH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Cross-hatch pencil fill inside platform
            var hx = pixelX + 12f
            while (hx < pixelX + pixelW) {
                drawLine(
                    color = SketchCoal.copy(alpha = 0.15f),
                    start = Offset(hx, pixelY),
                    end = Offset(hx - 12f, pixelY + pixelH),
                    strokeWidth = 1.5f
                )
                hx += 16f
            }
        }
        ObjectType.BOUNCER -> {
            // Lime bouncer coil
            drawRoundRect(
                color = SketchGreenSpring,
                topLeft = Offset(pixelX, pixelY),
                size = Size(pixelW, pixelH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = SketchCoal,
                topLeft = Offset(pixelX, pixelY),
                size = Size(pixelW, pixelH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            // Springer coils
            val midX = pixelX + pixelW / 2f
            drawLine(
                color = SketchCoal,
                start = Offset(midX, pixelY + pixelH),
                end = Offset(midX, pixelY + 2f),
                strokeWidth = 3f
            )
        }
        ObjectType.SPIKE_HAZARD -> {
            // Dangerous red spikes
            val path = Path()
            val spikeCount = maxOf(3, (pixelW / 14f).toInt())
            val stepW = pixelW / spikeCount

            path.moveTo(pixelX, pixelY + pixelH)
            for (i in 0 until spikeCount) {
                val sx = pixelX + i * stepW
                val mx = sx + stepW / 2f
                val ex = sx + stepW
                path.lineTo(mx, pixelY)
                path.lineTo(ex, pixelY + pixelH)
            }
            path.close()

            // Fill & border red
            drawPath(path = path, color = SketchRedSpike.copy(alpha = 0.2f))
            drawPath(path = path, color = SketchRedSpike, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        ObjectType.SPINNER_HAZARD -> {
            val radius = pixelW / 2f
            val cx = pixelX + radius
            val cy = pixelY + radius

            // Draw spinning sharp gear circle
            drawCircle(
                color = SketchRedSpike,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            // Center axle
            drawCircle(
                color = SketchCoal,
                radius = 6f,
                center = Offset(cx, cy)
            )

            // Spikes on the outer wheel
            val segments = 8
            for (i in 0 until segments) {
                val angle = (i * (360f / segments)).toDouble() * Math.PI / 180f
                val rx = cx + cos(angle).toFloat() * radius
                val ry = cy + sin(angle).toFloat() * radius
                drawLine(
                    color = SketchRedSpike,
                    start = Offset(cx, cy),
                    end = Offset(rx, ry),
                    strokeWidth = 2f
                )
                drawCircle(
                    color = SketchRedSpike,
                    radius = 4f,
                    center = Offset(rx, ry)
                )
            }
        }
        ObjectType.GRAVITY_UP, ObjectType.GRAVITY_DOWN, ObjectType.GRAVITY_LEFT, ObjectType.GRAVITY_RIGHT -> {
            // Gravity Switch orb
            val radius = pixelW / 2f
            val cx = pixelX + radius
            val cy = pixelY + radius

            // Glowing cyan pencil circle
            drawCircle(
                color = SketchActiveBlue.copy(alpha = 0.12f),
                radius = radius + 6f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = SketchActiveBlue,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // Tiny center indicator arrow
            val arrowPath = Path()
            when (obj.type) {
                ObjectType.GRAVITY_UP -> {
                    arrowPath.moveTo(cx, cy - 8f)
                    arrowPath.lineTo(cx - 6f, cy)
                    arrowPath.lineTo(cx + 6f, cy)
                    arrowPath.close()
                    drawLine(SketchActiveBlue, Offset(cx, cy - 8f), Offset(cx, cy + 8f), strokeWidth = 3f)
                }
                ObjectType.GRAVITY_DOWN -> {
                    arrowPath.moveTo(cx, cy + 8f)
                    arrowPath.lineTo(cx - 6f, cy)
                    arrowPath.lineTo(cx + 6f, cy)
                    arrowPath.close()
                    drawLine(SketchActiveBlue, Offset(cx, cy + 8f), Offset(cx, cy - 8f), strokeWidth = 3f)
                }
                ObjectType.GRAVITY_LEFT -> {
                    arrowPath.moveTo(cx - 8f, cy)
                    arrowPath.lineTo(cx, cy - 6f)
                    arrowPath.lineTo(cx, cy + 6f)
                    arrowPath.close()
                    drawLine(SketchActiveBlue, Offset(cx - 8f, cy), Offset(cx + 8f, cy), strokeWidth = 3f)
                }
                ObjectType.GRAVITY_RIGHT -> {
                    arrowPath.moveTo(cx + 8f, cy)
                    arrowPath.lineTo(cx, cy - 6f)
                    arrowPath.lineTo(cx, cy + 6f)
                    arrowPath.close()
                    drawLine(SketchActiveBlue, Offset(cx + 8f, cy), Offset(cx - 8f, cy), strokeWidth = 3f)
                }
                else -> {}
            }
            drawPath(arrowPath, SketchActiveBlue)
        }
    }
}

fun DrawScope.drawExitPortal(normalizedPos: Offset) {
    val pixelX = (normalizedPos.x / 100f) * size.width
    val pixelY = (normalizedPos.y / 100f) * size.height
    val doorRadius = 4.5f / 100f * size.width

    // Draw spiraling whirlpool portal
    for (i in 1..4) {
        val r = doorRadius - (i * 4f)
        if (r > 2f) {
            drawCircle(
                color = SketchCoal.copy(alpha = 1f - (i * 0.2f)),
                radius = r,
                center = Offset(pixelX, pixelY),
                style = Stroke(width = 2f)
            )
        }
    }

    // Outer sketched door frame
    drawCircle(
        color = SketchCoal,
        radius = doorRadius,
        center = Offset(pixelX, pixelY),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Hand drawn arrow flag pointing into portal
    drawLine(
        color = SketchActiveBlue,
        start = Offset(pixelX - doorRadius - 12f, pixelY - doorRadius),
        end = Offset(pixelX - doorRadius, pixelY - doorRadius - 12f),
        strokeWidth = 3f
    )
    drawLine(
        color = SketchActiveBlue,
        start = Offset(pixelX - doorRadius, pixelY - doorRadius - 12f),
        end = Offset(pixelX - doorRadius - 4f, pixelY - doorRadius),
        strokeWidth = 3f
    )
}

fun DrawScope.drawProtagonist(character: CharacterState, hint: String) {
    val pixelX = (character.x / 100f) * size.width
    val pixelY = (character.y / 100f) * size.height
    val radius = (character.radius / 100f) * size.width

    // Main ball shadow (doodle look)
    drawCircle(
        color = SketchCoal.copy(alpha = 0.08f),
        radius = radius + 2f,
        center = Offset(pixelX, pixelY + 1.5f)
    )

    // Draw sketchy circle
    drawCircle(
        color = SketchCoal,
        radius = radius,
        center = Offset(pixelX, pixelY),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Face styling based on active gravity and speed directions
    val eyeRadius = 3f
    var leftEyeX = pixelX - radius * 0.35f
    var rightEyeX = pixelX + radius * 0.35f
    var eyeY = pixelY - radius * 0.2f

    // Shift eyes based on active gravity pull
    if (character.activeGravityY < 0f) {
        // Gravitational floating up
        eyeY -= 3f
    } else if (character.activeGravityX > 0f) {
        // Gravity pulls right
        leftEyeX += 3f
        rightEyeX += 3f
    } else if (character.activeGravityX < 0f) {
        // Gravity pulls left
        leftEyeX -= 3f
        rightEyeX -= 3f
    }

    // Happy eyes, dead eyes, wide eyes
    when {
        character.isDead -> {
            // Draw XX dead eyes
            drawLine(SketchRedSpike, Offset(leftEyeX - 4f, eyeY - 4f), Offset(leftEyeX + 4f, eyeY + 4f), strokeWidth = 2.5f)
            drawLine(SketchRedSpike, Offset(leftEyeX + 4f, eyeY - 4f), Offset(leftEyeX - 4f, eyeY + 4f), strokeWidth = 2.5f)

            drawLine(SketchRedSpike, Offset(rightEyeX - 4f, eyeY - 4f), Offset(rightEyeX + 4f, eyeY + 4f), strokeWidth = 2.5f)
            drawLine(SketchRedSpike, Offset(rightEyeX + 4f, eyeY - 4f), Offset(rightEyeX - 4f, eyeY + 4f), strokeWidth = 2.5f)
        }
        else -> {
            // Living standard circular eyes
            drawCircle(SketchCoal, eyeRadius, Offset(leftEyeX, eyeY))
            drawCircle(SketchCoal, eyeRadius, Offset(rightEyeX, eyeY))

            // Gentle mouth smirk
            drawArc(
                color = SketchCoal,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(pixelX - 6f, pixelY + 2f),
                size = Size(12f, 8f),
                style = Stroke(width = 2f)
            )
        }
    }
}

/**
 * Draws a hand-drawn crayon stroke by overlapping multiple lines
 * of varying alphas and widths to mimic graphite crayon crayon textures.
 */
fun DrawScope.drawSketchyLine(points: List<Offset>, baseColor: Color) {
    if (points.size < 2) return

    val path = Path()
    val startPxX = (points[0].x / 100f) * size.width
    val startPxY = (points[0].y / 100f) * size.height
    path.moveTo(startPxX, startPxY)

    for (i in 1 until points.size) {
        val pxX = (points[i].x / 100f) * size.width
        val pxY = (points[i].y / 100f) * size.height
        path.lineTo(pxX, pxY)
    }

    // Background fat crayon graphite halo bleed
    drawPath(
        path = path,
        color = baseColor.copy(alpha = 0.2f),
        style = Stroke(
            width = 6.dp.toPx(),
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )

    // Solid inner dark pencil core
    drawPath(
        path = path,
        color = baseColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}

// Temporary inline custom font attribute binder override to prevent Android custom font failures
fun Modifier.fontFamilyHack(family: FontFamily) = this

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(0) } // 0 is Presentación, 1 is FAQ
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 24.dp)
                .padding(end = 6.dp, bottom = 6.dp) // Neo-brutalist shadow room
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SketchCardInks),
                border = BorderStroke(3.dp, SketchCoal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(cornerRadius = 16f, offset = 6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row with title and close X button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_title).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = SketchbookCSS.fontCursive,
                            color = SketchCoal
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .background(SketchCream, RoundedCornerShape(6.dp))
                                .border(1.5.dp, SketchCoal, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = SketchCoal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Notebook Paper sheet separator
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    ) {
                        // Drawing notebook spiral / ruling line
                        drawLine(
                            color = SketchMarginRed,
                            start = Offset(0f, 3f),
                            end = Offset(size.width, 3f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab selector row matching neoclassical/neo-brutalist theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Presentación Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .neoShadow(cornerRadius = 8f, offset = if (selectedTab == 0) 0f else 2f)
                                .background(
                                    if (selectedTab == 0) SketchCoal else SketchCream,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 0 }
                                .testTag("tab_presentation"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.faq_tab_presentation).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SketchbookCSS.fontTypewriter,
                                color = if (selectedTab == 0) Color.White else SketchCoal
                            )
                        }

                        // FAQ Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .neoShadow(cornerRadius = 8f, offset = if (selectedTab == 1) 0f else 2f)
                                .background(
                                    if (selectedTab == 1) SketchCoal else SketchCream,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 1 }
                                .testTag("tab_faq"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.faq_tab_faq).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SketchbookCSS.fontTypewriter,
                                color = if (selectedTab == 1) Color.White else SketchCoal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content Box based on selectedTab and currentPage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == 0) {
                            when (currentPage) {
                                0 -> TutorialSlideContent(
                                    title = stringResource(R.string.tutorial_slide1_title),
                                    description = stringResource(R.string.tutorial_slide1_desc),
                                    illustration = {
                                        // Bouncing ball happy drawing
                                        val infiniteTransition = rememberInfiniteTransition(label = "TutBall")
                                        val offsetY by infiniteTransition.animateFloat(
                                            initialValue = -15f,
                                            targetValue = 15f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = EaseInOutSine),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "tutOffsetY"
                                        )
                                        Canvas(modifier = Modifier.size(100.dp)) {
                                            val center = Offset(size.width / 2f, size.height / 2f + offsetY.dp.toPx())
                                            val radius = 22.dp.toPx()
                                            // Ball standard character
                                            drawCircle(
                                                color = SketchCoal,
                                                radius = radius,
                                                center = center,
                                                style = Stroke(width = 3.dp.toPx())
                                            )
                                            // Face expression
                                            drawCircle(
                                                color = SketchCoal,
                                                radius = 3.dp.toPx(),
                                                center = Offset(center.x - 6.dp.toPx(), center.y - 4.dp.toPx())
                                            )
                                            drawCircle(
                                                color = SketchCoal,
                                                radius = 3.dp.toPx(),
                                                center = Offset(center.x + 6.dp.toPx(), center.y - 4.dp.toPx())
                                            )
                                            drawArc(
                                                color = SketchCoal,
                                                startAngle = 10f,
                                                sweepAngle = 160f,
                                                useCenter = false,
                                                topLeft = Offset(center.x - 7.dp.toPx(), center.y + 1.dp.toPx()),
                                                size = Size(14.dp.toPx(), 8.dp.toPx()),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                            
                                            // Escape doorway hint
                                            drawRect(
                                                color = SketchMarginRed.copy(alpha = 0.5f),
                                                topLeft = Offset(size.width - 20.dp.toPx(), size.height - 30.dp.toPx()),
                                                size = Size(15.dp.toPx(), 25.dp.toPx())
                                            )
                                            drawRect(
                                                color = SketchCoal,
                                                topLeft = Offset(size.width - 20.dp.toPx(), size.height - 30.dp.toPx()),
                                                size = Size(15.dp.toPx(), 25.dp.toPx()),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                                )
                                1 -> TutorialSlideContent(
                                    title = stringResource(R.string.tutorial_slide2_title),
                                    description = stringResource(R.string.tutorial_slide2_desc),
                                    illustration = {
                                        // Pencil/crayon drafting path
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = SketchActiveBlue,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            // Custom line drawing
                                            Canvas(modifier = Modifier.width(130.dp).height(24.dp)) {
                                                val p = Path().apply {
                                                    moveTo(0f, size.height * 0.8f)
                                                    quadraticTo(size.width * 0.3f, size.height * 0.1f, size.width * 0.7f, size.height * 0.5f)
                                                    lineTo(size.width, size.height * 0.2f)
                                                }
                                                drawPath(
                                                    path = p,
                                                    color = SketchCoal,
                                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                            }
                                        }
                                    }
                                )
                                2 -> TutorialSlideContent(
                                    title = stringResource(R.string.tutorial_slide3_title),
                                    description = stringResource(R.string.tutorial_slide3_desc),
                                    illustration = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Warning Spikes
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Canvas(modifier = Modifier.size(40.dp)) {
                                                    val sizePx = size.width
                                                    val toothWidth = sizePx / 4f
                                                    val p = Path().apply {
                                                        moveTo(0f, sizePx)
                                                        for (i in 0..4) {
                                                            val x = i * toothWidth
                                                            val y = if (i % 2 == 0) sizePx else 0f
                                                            lineTo(x, y)
                                                        }
                                                        lineTo(sizePx, sizePx)
                                                        close()
                                                    }
                                                    drawPath(p, color = SketchRedSpike)
                                                    drawPath(p, color = SketchCoal, style = Stroke(width = 2.dp.toPx()))
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Spikes", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SketchbookCSS.fontTypewriter, color = SketchCoal)
                                            }

                                            // Spring / Bouncer
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Canvas(modifier = Modifier.size(40.dp)) {
                                                    val center = Offset(size.width / 2f, size.height / 2f)
                                                    val radius = size.minDimension * 0.40f
                                                    drawCircle(
                                                        color = SketchGreenSpring,
                                                        radius = radius,
                                                        center = center
                                                    )
                                                    drawCircle(
                                                        color = SketchCoal,
                                                        radius = radius,
                                                        center = center,
                                                        style = Stroke(width = 2.dp.toPx())
                                                    )
                                                    // Spring coil sketch line
                                                    drawLine(
                                                        color = SketchCoal,
                                                        start = Offset(center.x - 10f, center.y - 10f),
                                                        end = Offset(center.x + 10f, center.y + 10f),
                                                        strokeWidth = 2f
                                                    )
                                                    drawLine(
                                                        color = SketchCoal,
                                                        start = Offset(center.x - 10f, center.y + 10f),
                                                        end = Offset(center.x + 10f, center.y - 10f),
                                                        strokeWidth = 2f
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Bouncer", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SketchbookCSS.fontTypewriter, color = SketchCoal)
                                            }
                                        }
                                    }
                                )
                                3 -> TutorialSlideContent(
                                    title = stringResource(R.string.tutorial_slide4_title),
                                    description = stringResource(R.string.tutorial_slide4_desc),
                                    illustration = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Retry character button sketch
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(SketchCream, RoundedCornerShape(8.dp))
                                                        .border(2.dp, SketchCoal, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        tint = SketchActiveBlue,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Keep Ink", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = SketchbookCSS.fontTypewriter, color = SketchCoal)
                                            }
                                            
                                            // Star rating illustration
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(44.dp)
                                            )

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(SketchCream, RoundedCornerShape(8.dp))
                                                        .border(2.dp, SketchCoal, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = SketchRedSpike,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Clear All", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = SketchbookCSS.fontTypewriter, color = SketchCoal)
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            var expandedIndex by remember { mutableStateOf<Int?>(null) }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 2.dp)
                            ) {
                                val faqs = listOf(
                                    Pair(stringResource(R.string.faq_q1), stringResource(R.string.faq_a1)),
                                    Pair(stringResource(R.string.faq_q2), stringResource(R.string.faq_a2)),
                                    Pair(stringResource(R.string.faq_q3), stringResource(R.string.faq_a3)),
                                    Pair(stringResource(R.string.faq_q4), stringResource(R.string.faq_a4))
                                )
                                
                                faqs.forEachIndexed { index, (q, a) ->
                                    val isExpanded = expandedIndex == index
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SketchCream),
                                        border = BorderStroke(1.5.dp, SketchCoal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable { 
                                                expandedIndex = if (isExpanded) null else index 
                                            }
                                            .testTag("faq_item_$index")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = q,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = SketchbookCSS.fontPrint,
                                                    color = SketchCoal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isExpanded) "−" else "+",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = SketchbookCSS.fontTypewriter,
                                                    color = SketchActiveBlue
                                                )
                                            }
                                            if (isExpanded) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = a,
                                                    fontSize = 11.sp,
                                                    fontFamily = SketchbookCSS.fontPrint,
                                                    color = SketchCoal.copy(alpha = 0.85f),
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTab == 0) {
                        // Pager Page Dots styling (handcrafted circles of sketchbook notebook)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(22.dp)
                        ) {
                            for (i in 0..3) {
                                val isActive = i == currentPage
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = if (isActive) SketchActiveBlue else Color.Transparent,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = SketchCoal,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    } else {
                        // Keep constant height vertical spacing to prevent container resize jump!
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentPage > 0) {
                                    TextButton(
                                        onClick = { currentPage-- },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                            .border(2.dp, SketchCoal, RoundedCornerShape(8.dp))
                                            .testTag("tutorial_prev_button")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.tutorial_prev),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SketchbookCSS.fontTypewriter,
                                            color = SketchCoal
                                        )
                                    }
                                } else {
                                    // Empty spacer to occupy weight space so Siguiente is aligned right properly
                                    Spacer(modifier = Modifier.weight(1f).padding(end = 8.dp))
                                }

                                Button(
                                    onClick = {
                                        if (currentPage < 3) {
                                            currentPage++
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentPage == 3) SketchGreenSpring else SketchCoal
                                    ),
                                    border = BorderStroke(2.dp, SketchCoal),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("tutorial_next_button")
                                ) {
                                    Text(
                                        text = if (currentPage == 3) {
                                            stringResource(R.string.tutorial_finish)
                                        } else {
                                            stringResource(R.string.tutorial_next)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SketchbookCSS.fontPrint,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            // Single full width Finish / Got it button under FAQ
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = SketchGreenSpring),
                                border = BorderStroke(2.dp, SketchCoal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tutorial_finish_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.tutorial_finish).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SketchbookCSS.fontPrint,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialSlideContent(
    title: String,
    description: String,
    illustration: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = SketchbookCSS.fontPrint,
                color = SketchCoal,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                fontFamily = SketchbookCSS.fontPrint,
                color = SketchCoal.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            illustration()
        }
    }
}
