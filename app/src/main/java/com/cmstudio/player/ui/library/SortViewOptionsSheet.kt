package com.cmstudio.player.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cmstudio.player.library.MediaQualityFilterType
import com.cmstudio.player.ui.AppSettings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortViewOptionsSheet(
    sortOption: String, // "TITLE", "DATE", "SIZE"
    sortAscending: Boolean,
    viewMode: String, // "FOLDER", "TREE"
    layoutMode: String, // "LIST", "GRID"
    showFolderSize: Boolean,
    showFolderPath: Boolean,
    showDate: Boolean,
    showResolutionBadge: Boolean,
    onSortOptionChange: (String) -> Unit,
    onSortAscendingChange: (Boolean) -> Unit,
    onViewModeChange: (String) -> Unit,
    onLayoutModeChange: (String) -> Unit,
    onShowFolderSizeChange: (Boolean) -> Unit,
    onShowFolderPathChange: (Boolean) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onShowResolutionBadgeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val currentExtension by AppSettings.showExtensionField.collectAsState()
    val currentDuration by AppSettings.showDurationField.collectAsState()
    val currentProgressBar by AppSettings.showProgressBar.collectAsState()
    val currentUnlimitedLines by AppSettings.unlimitedNameLines.collectAsState()
    val currentCenterGridTitles by AppSettings.centerGridTitles.collectAsState()
    val currentTotalVideos by AppSettings.showTotalVideosChip.collectAsState()
    val currentTotalDuration by AppSettings.showTotalDurationChip.collectAsState()
    val currentFolderThumbs by AppSettings.showFolderThumbnails.collectAsState()
    val currentNewBadge by AppSettings.showNewBadge.collectAsState()
    val currentDualPaneTablet by AppSettings.dualPaneTablet.collectAsState()
    val currentManualGridColumns by AppSettings.manualGridColumnsEnabled.collectAsState()
    val currentSeparateFolderVideo by AppSettings.separateFolderVideoLayout.collectAsState()
    val currentQualityFilter by AppSettings.mediaQualityFilter.collectAsState()
    val currentOnlyShowFolders by AppSettings.onlyShowFoldersInHome.collectAsState()
    val currentHomeCompactGrid by AppSettings.homeCompactGrid.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val gridColsPortrait by AppSettings.videoGridColumnsPortrait.collectAsState()
    val gridColsLandscape by AppSettings.videoGridColumnsLandscape.collectAsState()
    val currentGridCols = if (isLandscape) gridColsLandscape else gridColsPortrait

    // Draft transaction state for Apply / Cancel / Reset
    var draftSortOption by remember(sortOption) { mutableStateOf(sortOption) }
    var draftSortAscending by remember(sortAscending) { mutableStateOf(sortAscending) }
    var draftViewMode by remember(viewMode) { mutableStateOf(viewMode) }
    var draftLayoutMode by remember(layoutMode) { mutableStateOf(layoutMode) }
    var draftShowFolderSize by remember(showFolderSize) { mutableStateOf(showFolderSize) }
    var draftShowFolderPath by remember(showFolderPath) { mutableStateOf(showFolderPath) }
    var draftShowDate by remember(showDate) { mutableStateOf(showDate) }
    var draftShowResolutionBadge by remember(showResolutionBadge) { mutableStateOf(showResolutionBadge) }
    var draftExtension by remember(currentExtension) { mutableStateOf(currentExtension) }
    var draftDuration by remember(currentDuration) { mutableStateOf(currentDuration) }
    var draftProgressBar by remember(currentProgressBar) { mutableStateOf(currentProgressBar) }
    var draftUnlimitedLines by remember(currentUnlimitedLines) { mutableStateOf(currentUnlimitedLines) }
    var draftCenterGridTitles by remember(currentCenterGridTitles) { mutableStateOf(currentCenterGridTitles) }
    var draftTotalVideos by remember(currentTotalVideos) { mutableStateOf(currentTotalVideos) }
    var draftTotalDuration by remember(currentTotalDuration) { mutableStateOf(currentTotalDuration) }
    var draftFolderThumbs by remember(currentFolderThumbs) { mutableStateOf(currentFolderThumbs) }
    var draftNewBadge by remember(currentNewBadge) { mutableStateOf(currentNewBadge) }
    var draftDualPaneTablet by remember(currentDualPaneTablet) { mutableStateOf(currentDualPaneTablet) }
    var draftManualGridColumns by remember(currentManualGridColumns) { mutableStateOf(currentManualGridColumns) }
    var draftSeparateFolderVideo by remember(currentSeparateFolderVideo) { mutableStateOf(currentSeparateFolderVideo) }
    var draftQualityFilter by remember(currentQualityFilter) { mutableStateOf(currentQualityFilter) }
    var draftOnlyShowFolders by remember(currentOnlyShowFolders) { mutableStateOf(currentOnlyShowFolders) }
    var draftHomeCompactGrid by remember(currentHomeCompactGrid) { mutableStateOf(currentHomeCompactGrid) }
    var draftGridCols by remember(currentGridCols) { mutableStateOf(currentGridCols) }

    var fieldsExpanded by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (fieldsExpanded) 180f else 0f,
        label = "fieldsArrowRotation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                // Header Title
                Text(
                    text = "Sort & View Options",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

            Spacer(modifier = Modifier.height(18.dp))

            // Section 1: "Sort by"
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortTypeButton(
                    modifier = Modifier.weight(1f),
                    label = "Name",
                    isSelected = draftSortOption == "TITLE",
                    onClick = { draftSortOption = "TITLE" },
                    customIconContent = { isSelected ->
                        Text(
                            text = "T",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                SortTypeButton(
                    modifier = Modifier.weight(1f),
                    label = "Date Added",
                    isSelected = draftSortOption == "DATE_ADDED",
                    onClick = { draftSortOption = "DATE_ADDED" },
                    icon = Icons.Rounded.EventAvailable
                )

                SortTypeButton(
                    modifier = Modifier.weight(1f),
                    label = "Modified",
                    isSelected = draftSortOption == "DATE",
                    onClick = { draftSortOption = "DATE" },
                    icon = Icons.Rounded.CalendarToday
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortTypeButton(
                    modifier = Modifier.weight(1f),
                    label = "Duration",
                    isSelected = draftSortOption == "DURATION",
                    onClick = { draftSortOption = "DURATION" },
                    icon = Icons.Rounded.Schedule
                )

                SortTypeButton(
                    modifier = Modifier.weight(1f),
                    label = "File Size",
                    isSelected = draftSortOption == "SIZE",
                    onClick = { draftSortOption = "SIZE" },
                    icon = Icons.Rounded.SwapVert
                )

                SortTypeButton(
                    modifier = Modifier.weight(1f),
                    label = "Recent",
                    isSelected = draftSortOption == "RECENTLY_PLAYED",
                    onClick = {
                        draftSortOption = "RECENTLY_PLAYED"
                        draftSortAscending = false
                    },
                    icon = Icons.Rounded.History
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val ascLabel = when (draftSortOption) {
                "DATE", "DATE_ADDED" -> "Oldest First"
                "RECENTLY_PLAYED" -> "Oldest Played"
                "DURATION" -> "Shortest"
                "SIZE" -> "Smallest"
                else -> "A-Z"
            }
            val descLabel = when (draftSortOption) {
                "DATE", "DATE_ADDED" -> "Newest First"
                "RECENTLY_PLAYED" -> "Most Recent"
                "DURATION" -> "Longest"
                "SIZE" -> "Largest"
                else -> "Z-A"
            }

            // Segmented Order Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                            .background(if (draftSortAscending) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { draftSortAscending = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = if (draftSortAscending) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ascLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (draftSortAscending) FontWeight.Bold else FontWeight.Medium,
                                color = if (draftSortAscending) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                            .background(if (!draftSortAscending) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { draftSortAscending = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (!draftSortAscending) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = descLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!draftSortAscending) FontWeight.Bold else FontWeight.Medium,
                                color = if (!draftSortAscending) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: View Mode (Folder | Tree | All Media)
            Text(
                text = "View Mode",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeToggleTile(
                    modifier = Modifier.weight(1f),
                    label = "Folder",
                    icon = Icons.Rounded.GridView,
                    isSelected = draftViewMode == "FOLDER",
                    onClick = { draftViewMode = "FOLDER" }
                )
                ModeToggleTile(
                    modifier = Modifier.weight(1f),
                    label = "Tree",
                    icon = Icons.Rounded.AccountTree,
                    isSelected = draftViewMode == "TREE",
                    onClick = { draftViewMode = "TREE" }
                )
                ModeToggleTile(
                    modifier = Modifier.weight(1f),
                    label = "All Media",
                    icon = Icons.Rounded.VideoLibrary,
                    isSelected = draftViewMode == "MEDIA",
                    onClick = { draftViewMode = "MEDIA" }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Only Show Folders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = draftOnlyShowFolders,
                    onCheckedChange = { draftOnlyShowFolders = it }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 3: Layout & Grid Columns
            Text(
                text = "Layout",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeToggleTile(
                    modifier = Modifier.weight(1f),
                    label = "List",
                    icon = Icons.AutoMirrored.Rounded.List,
                    isSelected = draftLayoutMode == "LIST",
                    onClick = {
                        draftLayoutMode = "LIST"
                        draftHomeCompactGrid = false
                    }
                )
                ModeToggleTile(
                    modifier = Modifier.weight(1f),
                    label = "Grid",
                    icon = Icons.Rounded.ViewModule,
                    isSelected = draftLayoutMode == "GRID" && !draftHomeCompactGrid,
                    onClick = {
                        draftLayoutMode = "GRID"
                        draftHomeCompactGrid = false
                    }
                )
                ModeToggleTile(
                    modifier = Modifier.weight(1f),
                    label = "Compact Grid",
                    icon = Icons.Rounded.GridView,
                    isSelected = draftLayoutMode == "GRID" && draftHomeCompactGrid,
                    onClick = {
                        draftLayoutMode = "GRID"
                        draftHomeCompactGrid = true
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Separate Folder & Video Layout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = draftSeparateFolderVideo,
                    onCheckedChange = { draftSeparateFolderVideo = it }
                )
            }

            AnimatedVisibility(visible = draftLayoutMode == "GRID") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLandscape) "Landscape Grid Columns" else "Portrait Grid Columns",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = draftManualGridColumns,
                            onClick = { draftManualGridColumns = !draftManualGridColumns },
                            label = { Text(if (draftManualGridColumns) "Manual" else "Adaptive") }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val choices = if (isLandscape) listOf(2, 3, 4, 5, 6) else listOf(1, 2, 3, 4)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        choices.forEach { cols ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = draftGridCols == cols,
                                onClick = {
                                    draftManualGridColumns = true
                                    draftGridCols = cols
                                },
                                label = { Text("${cols}x") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Media Quality Filter
            Text(
                text = "Media Quality Filter",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MediaQualityFilterType.entries.forEach { qFilter ->
                    FilterChip(
                        selected = draftQualityFilter == qFilter.id,
                        onClick = { draftQualityFilter = qFilter.id },
                        label = { Text(qFilter.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: "Fields" Accordion Dropdown
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { fieldsExpanded = !fieldsExpanded },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fields",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Toggle Fields",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            // Accordion Body
            AnimatedVisibility(visible = fieldsExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick FilterChip pill strip for the most toggled card fields
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val quickPills = listOf(
                            Triple("Duration", draftDuration, { v: Boolean -> draftDuration = v }),
                            Triple("Resolution", draftShowResolutionBadge, { v: Boolean -> draftShowResolutionBadge = v }),
                            Triple("Extension", draftExtension, { v: Boolean -> draftExtension = v }),
                            Triple("Progress", draftProgressBar, { v: Boolean -> draftProgressBar = v }),
                            Triple("NEW Badge", draftNewBadge, { v: Boolean -> draftNewBadge = v })
                        )
                        quickPills.forEach { (label, checked, setter) ->
                            FilterChip(
                                selected = checked,
                                onClick = { setter(!checked) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FieldCheckRow(
                            label = "Show File Size",
                            checked = draftShowFolderSize,
                            onCheckedChange = { draftShowFolderSize = it }
                        )
                        FieldCheckRow(
                            label = "Show File Path",
                            checked = draftShowFolderPath,
                            onCheckedChange = { draftShowFolderPath = it }
                        )
                        FieldCheckRow(
                            label = "Show Date Modified",
                            checked = draftShowDate,
                            onCheckedChange = { draftShowDate = it }
                        )
                        FieldCheckRow(
                            label = "Show Resolution Badges",
                            checked = draftShowResolutionBadge,
                            onCheckedChange = { draftShowResolutionBadge = it }
                        )
                        FieldCheckRow(
                            label = "Show Full Names (Unlimited Lines)",
                            checked = draftUnlimitedLines,
                            onCheckedChange = { draftUnlimitedLines = it }
                        )
                        FieldCheckRow(
                            label = "Show File Extension",
                            checked = draftExtension,
                            onCheckedChange = { draftExtension = it }
                        )
                        FieldCheckRow(
                            label = "Show Duration Badge",
                            checked = draftDuration,
                            onCheckedChange = { draftDuration = it }
                        )
                        FieldCheckRow(
                            label = "Show Progress Bar",
                            checked = draftProgressBar,
                            onCheckedChange = { draftProgressBar = it }
                        )
                        FieldCheckRow(
                            label = "Show Total Videos Count",
                            checked = draftTotalVideos,
                            onCheckedChange = { draftTotalVideos = it }
                        )
                        FieldCheckRow(
                            label = "Show Total Folder Duration",
                            checked = draftTotalDuration,
                            onCheckedChange = { draftTotalDuration = it }
                        )
                        FieldCheckRow(
                            label = "Show Folder Thumbnails",
                            checked = draftFolderThumbs,
                            onCheckedChange = { draftFolderThumbs = it }
                        )
                        FieldCheckRow(
                            label = "Center Grid Titles",
                            checked = draftCenterGridTitles,
                            onCheckedChange = { draftCenterGridTitles = it }
                        )
                        FieldCheckRow(
                            label = "Show NEW Unplayed Badge",
                            checked = draftNewBadge,
                            onCheckedChange = { draftNewBadge = it }
                        )
                        FieldCheckRow(
                            label = "Tablet Dual-Pane Split View",
                            checked = draftDualPaneTablet,
                            onCheckedChange = { draftDualPaneTablet = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        draftSortOption = "TITLE"
                        draftSortAscending = true
                        draftViewMode = "FOLDER"
                        draftLayoutMode = "LIST"
                        draftShowFolderSize = true
                        draftShowFolderPath = true
                        draftShowDate = false
                        draftShowResolutionBadge = true
                        draftExtension = true
                        draftDuration = true
                        draftProgressBar = true
                        draftUnlimitedLines = false
                        draftCenterGridTitles = false
                        draftTotalVideos = true
                        draftTotalDuration = true
                        draftFolderThumbs = true
                        draftNewBadge = true
                        draftDualPaneTablet = true
                        draftManualGridColumns = false
                        draftSeparateFolderVideo = false
                        draftQualityFilter = MediaQualityFilterType.ALL.id
                        draftOnlyShowFolders = false
                        draftHomeCompactGrid = false
                        draftGridCols = if (isLandscape) 4 else 2
                    }
                ) {
                    Text("Reset")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSortOptionChange(draftSortOption)
                        onSortAscendingChange(draftSortAscending)
                        onViewModeChange(draftViewMode)
                        onLayoutModeChange(draftLayoutMode)
                        onShowFolderSizeChange(draftShowFolderSize)
                        onShowFolderPathChange(draftShowFolderPath)
                        onShowDateChange(draftShowDate)
                        onShowResolutionBadgeChange(draftShowResolutionBadge)
                        AppSettings.setShowExtensionField(draftExtension)
                        AppSettings.setShowDurationField(draftDuration)
                        AppSettings.setShowProgressBar(draftProgressBar)
                        AppSettings.setUnlimitedNameLines(draftUnlimitedLines)
                        AppSettings.setCenterGridTitles(draftCenterGridTitles)
                        AppSettings.setShowTotalVideosChip(draftTotalVideos)
                        AppSettings.setShowTotalDurationChip(draftTotalDuration)
                        AppSettings.setShowFolderThumbnails(draftFolderThumbs)
                        AppSettings.setShowNewBadge(draftNewBadge)
                        AppSettings.setDualPaneTablet(draftDualPaneTablet)
                        AppSettings.setManualGridColumnsEnabled(draftManualGridColumns)
                        AppSettings.setSeparateFolderVideoLayout(draftSeparateFolderVideo)
                        AppSettings.setMediaQualityFilter(draftQualityFilter)
                        AppSettings.setOnlyShowFoldersInHome(draftOnlyShowFolders)
                        AppSettings.setHomeCompactGrid(draftHomeCompactGrid)
                        if (draftLayoutMode == "GRID") {
                            val cols = if (draftHomeCompactGrid) {
                                (draftGridCols + 1).coerceAtMost(if (isLandscape) 6 else 4)
                            } else {
                                draftGridCols
                            }
                            if (isLandscape) {
                                AppSettings.setFolderGridColumnsLandscape(cols)
                                AppSettings.setVideoGridColumnsLandscape(cols)
                            } else {
                                AppSettings.setFolderGridColumnsPortrait(cols)
                                AppSettings.setVideoGridColumnsPortrait(cols)
                            }
                        }
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
}

@Composable
private fun SortTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    customIconContent: (@Composable (Boolean) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (customIconContent != null) {
                customIconContent(isSelected)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModeToggleTile(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FieldCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}
