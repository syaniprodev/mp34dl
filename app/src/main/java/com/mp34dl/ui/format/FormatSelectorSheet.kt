package com.mp34dl.ui.format

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mp34dl.command.VideoContainer
import com.mp34dl.model.AvailableMediaOptions
import com.mp34dl.model.MediaFormatOption
import com.mp34dl.ui.theme.GlassSurface
import com.mp34dl.ui.theme.LocalLiquidGlassTokens
import com.mp34dl.ui.theme.livingFlowBrush
import java.util.Locale

/**
 * Hasil pilihan pengguna dari [FormatSelectorSheet].
 * Bisa langsung diteruskan ke `CommandBuilder.buildRequest(...)`:
 *
 * ```
 * CommandBuilder.buildRequest(
 *     url = url,
 *     selectedOption = selection.option,
 *     targetContainer = selection.targetContainer,
 *     outputBitrate = selection.outputBitrate
 * )
 * ```
 */
data class FormatSelection(
    val option: MediaFormatOption,
    val targetContainer: VideoContainer = VideoContainer.MP4,
    val outputBitrate: String? = null
)

/**
 * Bottom sheet pemilih format. Hanya merender chip untuk resolusi, FPS, HDR,
 * format audio, dan bitrate yang benar-benar ada di [options]. Baris/tab yang
 * tidak punya pilihan disembunyikan total (tidak ada placeholder atau chip disabled).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectorSheet(
    options: AvailableMediaOptions,
    onDismiss: () -> Unit,
    onConfirm: (FormatSelection) -> Unit,
    modifier: Modifier = Modifier,
    initialContainer: VideoContainer = VideoContainer.MP4
) {
    val tokens = LocalLiquidGlassTokens.current
    val isGlass = tokens.surfaceAlpha < 1f
    val colors = MaterialTheme.colorScheme
    val topShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    val videoPicker = remember(options) { VideoPicker(options.videoOptions) }
    val audioPicker = remember(options) { AudioPicker(options.audioOptions) }

    // Tab hanya dibuat untuk kategori yang punya opsi.
    val tabs = remember(options) {
        buildList {
            if (options.hasVideoOptions) add(SheetTab.VIDEO)
            if (options.hasAudioOptions) add(SheetTab.AUDIO)
        }
    }
    var tabIndex by remember(options) { mutableStateOf(0) }
    val activeTab = tabs.getOrNull(tabIndex)
    var container by remember(options) { mutableStateOf(initialContainer) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = topShape,
        containerColor = if (isGlass) colors.background.copy(alpha = 0.88f) else colors.surface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(topShape)
                .background(livingFlowBrush())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                options.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )
                }

                if (activeTab == null) {
                    Text(
                        text = "Tidak ada format yang tersedia untuk URL ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                    return@Column
                }

                if (tabs.size > 1) {
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == tabIndex,
                                onClick = { tabIndex = index },
                                text = { Text(tab.label) }
                            )
                        }
                    }
                }

                when (activeTab) {
                    SheetTab.VIDEO -> VideoSection(
                        picker = videoPicker,
                        container = container,
                        onContainerChange = { container = it }
                    )
                    SheetTab.AUDIO -> AudioSection(picker = audioPicker)
                }

                val selectedOption = when (activeTab) {
                    SheetTab.VIDEO -> videoPicker.selected
                    SheetTab.AUDIO -> audioPicker.selected
                }

                selectedOption?.let { option ->
                    Text(
                        text = describe(option),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.7f)
                    )
                    Button(
                        onClick = {
                            onConfirm(
                                FormatSelection(
                                    option = option,
                                    targetContainer = container,
                                    outputBitrate = if (option.isAudioOnly) option.bitrateLabel else null
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unduh")
                    }
                }
            }
        }
    }
}

private enum class SheetTab(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio")
}

// ---------------------------------------------------------------------------
// State pemilihan. Nilai pilihan pengguna selalu di-coerce ke opsi yang tersedia,
// sehingga mengganti resolusi tidak pernah menyisakan FPS/HDR yang tidak ada.
// ---------------------------------------------------------------------------

@Stable
private class VideoPicker(private val options: List<MediaFormatOption>) {
    private var heightChoice by mutableStateOf<Int?>(null)
    private var fpsChoice by mutableStateOf<Int?>(null)
    private var hdrChoice by mutableStateOf<Boolean?>(null)

    val heights: List<Int> = options.mapNotNull { it.height }.distinct().sortedDescending()

    val height: Int? get() = heightChoice?.takeIf { it in heights } ?: heights.firstOrNull()

    private val byHeight: List<MediaFormatOption> get() = options.filter { it.height == height }

    val fpsValues: List<Int>
        get() = byHeight.mapNotNull { it.fps }.distinct().sortedDescending()

    val fps: Int? get() = fpsChoice?.takeIf { it in fpsValues } ?: fpsValues.firstOrNull()

    private val byFps: List<MediaFormatOption>
        get() = fps?.let { f -> byHeight.filter { it.fps == f } } ?: byHeight

    /** SDR (false) lebih dulu, lalu HDR (true); hanya nilai yang benar-benar ada. */
    val hdrValues: List<Boolean> get() = byFps.map { it.isHdr }.distinct().sorted()

    /** Baris HDR hanya tampil jika ada stream HDR untuk kombinasi saat ini. */
    val showHdrRow: Boolean get() = true in hdrValues

    val hdr: Boolean? get() = hdrChoice?.takeIf { it in hdrValues } ?: hdrValues.firstOrNull()

    val selected: MediaFormatOption?
        get() = byFps.firstOrNull { it.isHdr == hdr } ?: byFps.firstOrNull()

    fun selectHeight(value: Int) { heightChoice = value }
    fun selectFps(value: Int) { fpsChoice = value }
    fun selectHdr(value: Boolean) { hdrChoice = value }
}

@Stable
private class AudioPicker(private val options: List<MediaFormatOption>) {
    private var formatChoice by mutableStateOf<String?>(null)
    private var bitrateChoice by mutableStateOf<String?>(null)

    private fun keyOf(option: MediaFormatOption): String =
        option.ext?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: option.formatId

    val formatKeys: List<String> = options.map(::keyOf).distinct()

    val formatKey: String? get() = formatChoice?.takeIf { it in formatKeys } ?: formatKeys.firstOrNull()

    private val inFormat: List<MediaFormatOption>
        get() = options.filter { keyOf(it) == formatKey }

    fun formatLabel(key: String): String {
        val original = options.any { keyOf(it) == key && it.isLosslessOriginal }
        return key.uppercase() + if (original) " · Original" else ""
    }

    val bitrateLabels: List<String>
        get() = inFormat.mapNotNull { it.bitrateLabel?.trim()?.lowercase()?.takeIf { l -> l.isNotEmpty() } }
            .distinct()
            .sortedByDescending { it.removeSuffix("k").toIntOrNull() ?: Int.MAX_VALUE }

    val bitrateLabel: String?
        get() = bitrateChoice?.takeIf { it in bitrateLabels } ?: bitrateLabels.firstOrNull()

    val selected: MediaFormatOption?
        get() = inFormat.firstOrNull { it.bitrateLabel?.trim()?.lowercase() == bitrateLabel }
            ?: inFormat.firstOrNull()

    fun selectFormat(key: String) { formatChoice = key }
    fun selectBitrate(label: String) { bitrateChoice = label }
}

// ---------------------------------------------------------------------------
// Section
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoSection(
    picker: VideoPicker,
    container: VideoContainer,
    onContainerChange: (VideoContainer) -> Unit
) {
    SectionCard {
        ChipGroup(title = "Resolusi") {
            picker.heights.forEach { h ->
                OptionChip("${h}p", selected = h == picker.height) { picker.selectHeight(h) }
            }
        }
        if (picker.fpsValues.isNotEmpty()) {
            ChipGroup(title = "Frame rate") {
                picker.fpsValues.forEach { f ->
                    OptionChip("$f fps", selected = f == picker.fps) { picker.selectFps(f) }
                }
            }
        }
        if (picker.showHdrRow) {
            ChipGroup(title = "Rentang dinamis") {
                picker.hdrValues.forEach { isHdr ->
                    OptionChip(
                        label = if (isHdr) "HDR" else "SDR",
                        selected = isHdr == picker.hdr
                    ) { picker.selectHdr(isHdr) }
                }
            }
        }
        // Kontainer remux didukung CommandBuilder untuk semua stream video; bukan opsi dari yt-dlp.
        ChipGroup(title = "Kontainer") {
            VideoContainer.values().forEach { c ->
                OptionChip(c.ext.uppercase(), selected = c == container) { onContainerChange(c) }
            }
        }
    }
}

@Composable
private fun AudioSection(picker: AudioPicker) {
    SectionCard {
        ChipGroup(title = "Format") {
            picker.formatKeys.forEach { key ->
                OptionChip(picker.formatLabel(key), selected = key == picker.formatKey) {
                    picker.selectFormat(key)
                }
            }
        }
        if (picker.bitrateLabels.isNotEmpty()) {
            ChipGroup(title = "Bitrate") {
                picker.bitrateLabels.forEach { label ->
                    OptionChip(bitrateText(label), selected = label == picker.bitrateLabel) {
                        picker.selectBitrate(label)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Komponen kecil
// ---------------------------------------------------------------------------

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    val tokens = LocalLiquidGlassTokens.current
    val isGlass = tokens.surfaceAlpha < 1f
    val animation: FiniteAnimationSpec<IntSize> = if (isGlass) {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    } else {
        tween(durationMillis = 150)
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animation),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(title: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            chips()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalLiquidGlassTokens.current
    val isGlass = tokens.surfaceAlpha < 1f
    val colors = MaterialTheme.colorScheme

    // Mode glass: chip terpilih membesar sedikit dengan spring dari token tema.
    val scale by animateFloatAsState(
        targetValue = if (selected && isGlass) 1.06f else 1f,
        animationSpec = if (isGlass) tokens.springSpec else tween(durationMillis = 120),
        label = "chipScale"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.scale(scale),
        colors = if (isGlass) {
            FilterChipDefaults.filterChipColors(
                containerColor = colors.onSurface.copy(alpha = 0.06f),
                selectedContainerColor = colors.primary.copy(alpha = 0.28f),
                labelColor = colors.onSurface,
                selectedLabelColor = colors.onSurface
            )
        } else {
            FilterChipDefaults.filterChipColors()
        }
    )
}

private fun bitrateText(label: String): String = when {
    label == "best" -> "Best"
    label == "max" -> "Max"
    label.endsWith("k") && label.dropLast(1).toIntOrNull() != null -> "${label.dropLast(1)} kbps"
    else -> label
}

private fun describe(option: MediaFormatOption): String {
    val parts = mutableListOf<String>()
    if (option.isAudioOnly) {
        option.ext?.let { parts.add(it.uppercase()) }
        option.bitrateKbps?.let { parts.add("$it kbps") }
        option.acodec?.let { parts.add(it) }
    } else {
        option.height?.let { h ->
            parts.add(if (option.fps != null) "${h}p${option.fps}" else "${h}p")
        }
        if (option.isHdr) parts.add(option.dynamicRange ?: "HDR")
        option.vcodec?.let { parts.add(it) }
    }
    option.filesizeApprox?.takeIf { it > 0 }?.let { parts.add("≈ ${formatSize(it)}") }
    return parts.joinToString(" · ")
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return if (mb >= 1024) {
        String.format(Locale.US, "%.2f GB", mb / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
