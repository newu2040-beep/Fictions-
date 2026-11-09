package com.editingcells.fictions.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editingcells.fictions.domain.model.CsvTableData
import com.editingcells.fictions.domain.model.VaultFile
import com.editingcells.fictions.domain.model.VaultFileType
import com.editingcells.fictions.ui.components.AmbientVaultBackground
import com.editingcells.fictions.ui.components.EmptyVaultState
import com.editingcells.fictions.ui.components.FileTypeBadge
import com.editingcells.fictions.ui.components.GlassSurface
import com.editingcells.fictions.ui.components.LoadingSkeletonList
import com.editingcells.fictions.ui.components.LocalGlassScrollPhase
import com.editingcells.fictions.ui.components.LocalVaultBackdrop
import com.editingcells.fictions.ui.theme.JetBrainsMonoFamily
import com.editingcells.fictions.ui.theme.LocalResponsiveLayout
import com.editingcells.fictions.ui.theme.PlusJakartaSansFamily
import com.editingcells.fictions.ui.viewmodel.ViewerUiState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * Full-bleed Viewer Screen for PDF, CSV, and TXT files.
 * Features:
 * - Floating iOS 27-style liquid-glass top pill (Back circle button, Title + Type badge, More circle button)
 * - Floating iOS 27-style liquid-glass bottom toolbar (Zoom / Page indicator / Column search / Font size / Share)
 * - Auto-hides bottom pill when IME keyboard opens, centers pills with responsive maxWidth on wide screens,
 *   and shifts specular rim highlight gently on scroll/pan.
 */
@Composable
fun ViewerScreen(
    viewerState: ViewerUiState,
    readerFontSizeSp: Float,
    onBack: () -> Unit,
    onOpenActionsSheet: (VaultFile) -> Unit,
    onShareFile: (VaultFile) -> Unit,
    onEditTxtNote: (VaultFile, String) -> Unit,
    onAdjustFontSize: (Float) -> Unit
) {
    BackHandler(onBack = onBack)

    val responsive = LocalResponsiveLayout.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    var chromeVisible by remember { mutableStateOf(true) }
    var searchBarExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var viewerScrollPhase by remember { mutableFloatStateOf(0f) }

    // PDF viewer state hoisted so bottom toolbar can control page & zoom
    var pdfPageIndex by remember { mutableIntStateOf(0) }
    var pdfPageCount by remember { mutableIntStateOf(1) }
    var pdfZoom by remember { mutableFloatStateOf(1f) }
    var pdfPan by remember { mutableStateOf(Offset.Zero) }

    val viewerBackdrop = rememberLayerBackdrop()
    val file = viewerState.file

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Recorded background content layer (LocalVaultBackdrop is null inside here to prevent self-draw cycles)
        CompositionLocalProvider(LocalVaultBackdrop provides null) {
            AmbientVaultBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(viewerBackdrop)
            ) {
                when {
                    viewerState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(responsive.space24),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingSkeletonList(count = 5)
                        }
                    }

                    viewerState.error != null || file == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(responsive.space24),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyVaultState(
                                title = "Unable to Open Document",
                                subtitle = viewerState.error ?: "File not found",
                                actionLabel = "Go Back",
                                onActionClick = onBack
                            )
                        }
                    }

                    file.type == VaultFileType.PDF -> {
                        PdfViewerContent(
                            filePath = file.filePath,
                            currentPage = pdfPageIndex,
                            zoom = pdfZoom,
                            pan = pdfPan,
                            onPageCountLoaded = { count ->
                                pdfPageCount = count.coerceAtLeast(1)
                                if (pdfPageIndex >= pdfPageCount) pdfPageIndex = 0
                            },
                            onTransformChange = { newZoom, newPan ->
                                pdfZoom = newZoom
                                pdfPan = newPan
                                viewerScrollPhase = ((newPan.x + newPan.y) / 900f) % 1f
                            },
                            onToggleChrome = { chromeVisible = !chromeVisible }
                        )
                    }

                    file.type == VaultFileType.CSV -> {
                        CsvViewerContent(
                            csvData = viewerState.csvData ?: CsvTableData(emptyList(), emptyList()),
                            searchQuery = searchQuery,
                            onToggleChrome = { chromeVisible = !chromeVisible },
                            onScrollPhaseChanged = { viewerScrollPhase = it }
                        )
                    }

                    file.type == VaultFileType.TXT -> {
                        TxtViewerContent(
                            text = viewerState.txtContent,
                            fontSizeSp = readerFontSizeSp,
                            searchQuery = searchQuery,
                            onToggleChrome = { chromeVisible = !chromeVisible },
                            onScrollPhaseChanged = { viewerScrollPhase = it }
                        )
                    }
                }
            }
        }

        // 2. Foreground Liquid Glass overlays sampling viewerBackdrop (Max 2 glass layers on screen)
        CompositionLocalProvider(
            LocalVaultBackdrop provides viewerBackdrop,
            LocalGlassScrollPhase provides viewerScrollPhase
        ) {
            // Floating Liquid Glass Top Pill
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { -it }
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                        .padding(horizontal = responsive.screenMargin, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassSurface(
                        shape = CircleShape,
                        blurRadius = 24.dp,
                        refractionHeight = 20.dp,
                        refractionAmount = 34.dp,
                        elevation = 14.dp,
                        modifier = Modifier
                            .widthIn(max = responsive.floatingBarMaxWidth)
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                onClick = onBack,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (file != null) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    FileTypeBadge(type = file.type)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    onClick = { onOpenActionsSheet(file) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "More actions",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Expandable search bar for CSV / TXT viewer
                    AnimatedVisibility(visible = searchBarExpanded && file?.type != VaultFileType.PDF) {
                        GlassSurface(
                            shape = CircleShape,
                            blurRadius = 22.dp,
                            refractionHeight = 16.dp,
                            refractionAmount = 26.dp,
                            modifier = Modifier
                                .widthIn(max = responsive.floatingBarMaxWidth)
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = if (file?.type == VaultFileType.CSV) "Search columns & cells…"
                                        else "Find in text…",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            searchBarExpanded = false
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close search")
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Floating Liquid Glass Bottom Pill Toolbar (auto-hides when keyboard opens)
            AnimatedVisibility(
                visible = chromeVisible && file != null && !isImeVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (file != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout))
                            .padding(horizontal = responsive.screenMargin, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassSurface(
                            shape = CircleShape,
                            blurRadius = 26.dp,
                            refractionHeight = 22.dp,
                            refractionAmount = 36.dp,
                            elevation = 16.dp,
                            modifier = Modifier
                                .widthIn(max = responsive.floatingBarMaxWidth)
                                .fillMaxWidth()
                                .heightIn(min = 66.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                when (file.type) {
                                    VaultFileType.PDF -> {
                                        // Zoom Out / Zoom In
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            ToolbarCircleButton(
                                                icon = Icons.Default.ZoomOut,
                                                contentDescription = "Zoom out",
                                                onClick = {
                                                    pdfZoom = (pdfZoom - 0.25f).coerceAtLeast(1f)
                                                    if (pdfZoom == 1f) pdfPan = Offset.Zero
                                                }
                                            )
                                            ToolbarCircleButton(
                                                icon = Icons.Default.ZoomIn,
                                                contentDescription = "Zoom in",
                                                onClick = {
                                                    pdfZoom = (pdfZoom + 0.5f).coerceAtMost(4f)
                                                }
                                            )
                                        }

                                        // Page Indicator Pill with Prev / Next
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ToolbarCircleButton(
                                                icon = Icons.AutoMirrored.Filled.NavigateBefore,
                                                contentDescription = "Previous page",
                                                onClick = {
                                                    if (pdfPageIndex > 0) {
                                                        pdfPageIndex--
                                                        pdfZoom = 1f
                                                        pdfPan = Offset.Zero
                                                    }
                                                }
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "${pdfPageIndex + 1} / $pdfPageCount",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                            ToolbarCircleButton(
                                                icon = Icons.AutoMirrored.Filled.NavigateNext,
                                                contentDescription = "Next page",
                                                onClick = {
                                                    if (pdfPageIndex < pdfPageCount - 1) {
                                                        pdfPageIndex++
                                                        pdfZoom = 1f
                                                        pdfPan = Offset.Zero
                                                    }
                                                }
                                            )
                                        }

                                        ToolbarCircleButton(
                                            icon = Icons.Default.Share,
                                            contentDescription = "Share PDF",
                                            onClick = { onShareFile(file) }
                                        )
                                    }

                                    VaultFileType.CSV -> {
                                        val rowCount = viewerState.csvData?.rows?.size ?: 0
                                        val colCount = viewerState.csvData?.headers?.size ?: 0
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "$rowCount rows × $colCount cols",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ToolbarCircleButton(
                                                icon = Icons.Default.Search,
                                                contentDescription = "Search CSV columns",
                                                onClick = { searchBarExpanded = !searchBarExpanded }
                                            )
                                            ToolbarCircleButton(
                                                icon = Icons.Default.Share,
                                                contentDescription = "Share CSV",
                                                onClick = { onShareFile(file) }
                                            )
                                        }
                                    }

                                    VaultFileType.TXT -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ToolbarCircleButton(
                                                icon = Icons.Default.Remove,
                                                contentDescription = "Decrease font size",
                                                onClick = { onAdjustFontSize(-2f) }
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${readerFontSizeSp.toInt()} sp",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                            ToolbarCircleButton(
                                                icon = Icons.Default.Add,
                                                contentDescription = "Increase font size",
                                                onClick = { onAdjustFontSize(2f) }
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ToolbarCircleButton(
                                                icon = Icons.Default.Search,
                                                contentDescription = "Search in text",
                                                onClick = { searchBarExpanded = !searchBarExpanded }
                                            )
                                            ToolbarCircleButton(
                                                icon = Icons.Default.EditNote,
                                                contentDescription = "Edit note",
                                                onClick = { onEditTxtNote(file, viewerState.txtContent) }
                                            )
                                            ToolbarCircleButton(
                                                icon = Icons.Default.Share,
                                                contentDescription = "Share TXT",
                                                onClick = { onShareFile(file) }
                                            )
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
}

@Composable
private fun ToolbarCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * PDF Viewer Content using Android's PdfRenderer on Dispatchers.IO with pinch-zoom & double-tap zoom.
 */
@Composable
private fun PdfViewerContent(
    filePath: String,
    currentPage: Int,
    zoom: Float,
    pan: Offset,
    onPageCountLoaded: (Int) -> Unit,
    onTransformChange: (Float, Offset) -> Unit,
    onToggleChrome: () -> Unit
) {
    val responsive = LocalResponsiveLayout.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(filePath) {
        onDispose {
            pageBitmap = null
        }
    }

    LaunchedEffect(filePath, currentPage) {
        renderError = null
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                if (!file.exists()) error("PDF file missing")
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val total = renderer.pageCount
                        withContext(Dispatchers.Main) {
                            onPageCountLoaded(total)
                        }
                        if (total > 0) {
                            val safeIndex = currentPage.coerceIn(0, total - 1)
                            renderer.openPage(safeIndex).use { page ->
                                val scale = (1440f / maxOf(page.width, page.height, 1)).coerceIn(1f, 2.2f)
                                val width = (page.width * scale).toInt().coerceIn(400, 1600)
                                val height = (page.height * scale).toInt().coerceIn(500, 2200)
                                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                withContext(Dispatchers.Main) {
                                    pageBitmap = bmp
                                }
                            }
                        }
                    }
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    renderError = err.localizedMessage ?: "Failed to render PDF page"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleChrome() },
                    onDoubleTap = {
                        if (zoom > 1.05f) {
                            onTransformChange(1f, Offset.Zero)
                        } else {
                            onTransformChange(2.2f, Offset.Zero)
                        }
                    }
                )
            }
            .pointerInput(zoom, pan) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(1f, 4f)
                    val maxPan = 600f * (newZoom - 1f)
                    val newPan = if (newZoom <= 1.01f) {
                        Offset.Zero
                    } else {
                        Offset(
                            x = (pan.x + panChange.x).coerceIn(-maxPan, maxPan),
                            y = (pan.y + panChange.y).coerceIn(-maxPan, maxPan)
                        )
                    }
                    onTransformChange(newZoom, newPan)
                }
            }
            .padding(
                horizontal = responsive.screenMargin,
                vertical = 92.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            renderError != null -> {
                EmptyVaultState(
                    title = "PDF Render Error",
                    subtitle = renderError ?: "Unknown PDF error"
                )
            }
            pageBitmap != null -> {
                val bmp = pageBitmap!!
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 12.dp,
                    color = Color.White,
                    modifier = Modifier
                        .widthIn(max = responsive.contentMaxWidth)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        }
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPage + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            else -> {
                LoadingSkeletonList(count = 3)
            }
        }
    }
}

/**
 * CSV Viewer Content: Scrollable 2D table with sticky header row and column search.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CsvViewerContent(
    csvData: CsvTableData,
    searchQuery: String,
    onToggleChrome: () -> Unit,
    onScrollPhaseChanged: (Float) -> Unit
) {
    val responsive = LocalResponsiveLayout.current
    val listState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val rawPixels = index * 120f + offset
                onScrollPhaseChanged((rawPixels / 1100f) % 1f)
            }
    }

    val filteredRows = remember(csvData, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            csvData.rows
        } else {
            csvData.rows.filter { row ->
                row.any { cell -> cell.contains(q, ignoreCase = true) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars)),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleChrome() },
            contentPadding = PaddingValues(
                start = responsive.screenMargin,
                end = responsive.screenMargin,
                top = 96.dp,
                bottom = 112.dp
            )
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    csvData.headers.forEach { header ->
                        Box(
                            modifier = Modifier
                                .width(156.dp)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            itemsIndexed(filteredRows) { rowIndex, row ->
                val isEven = rowIndex % 2 == 0
                val rowBg = if (isEven) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .background(rowBg)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = (rowIndex + 1).toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    row.forEach { cell ->
                        val isHighlighted = searchQuery.isNotBlank() &&
                            cell.contains(searchQuery.trim(), ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isHighlighted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * TXT Viewer Content: Solid, high-contrast literary surface (never glass on body text blocks)
 * with adjustable font size & search filter.
 */
@Composable
private fun TxtViewerContent(
    text: String,
    fontSizeSp: Float,
    searchQuery: String,
    onToggleChrome: () -> Unit,
    onScrollPhaseChanged: (Float) -> Unit
) {
    val responsive = LocalResponsiveLayout.current
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { offset ->
                onScrollPhaseChanged((offset / 1100f) % 1f)
            }
    }

    val lines = remember(text, searchQuery) {
        val allLines = text.lines()
        val q = searchQuery.trim()
        if (q.isEmpty()) allLines else allLines.filter { it.contains(q, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleChrome() }
                .padding(
                    start = responsive.screenMargin,
                    end = responsive.screenMargin,
                    top = 96.dp,
                    bottom = 116.dp
                )
        ) {
            // Solid, readable surface for body text blocks (WCAG AA contrast)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(responsive.space24),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${text.lines().size} lines • ${
                                text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                            } words",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "${lines.size} matching lines",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = lines.joinToString("\n"),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.55f).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
