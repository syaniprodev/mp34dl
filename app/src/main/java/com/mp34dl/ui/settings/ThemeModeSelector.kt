package com.mp34dl.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mp34dl.ui.theme.AppThemeMode

/**
 * Pilihan tema untuk halaman Settings: `MATERIAL` atau `LIQUID_GLASS`.
 *
 * Komponen ini stateless. Simpan nilai di Settings storage (mis. DataStore) dan
 * teruskan ke `AppTheme(mode = ...)`:
 *
 * ```
 * ThemeModeSelector(
 *     selected = themeMode,
 *     onSelected = { viewModel.setThemeMode(it) }
 * )
 * ```
 */
@Composable
fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tema aplikasi",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppThemeMode.values().forEach { mode ->
                ThemeModeCard(
                    mode = mode,
                    selected = mode == selected,
                    onClick = { onSelected(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    mode: AppThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    // Kartu terpilih sedikit membesar dengan spring ala "living flow".
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "themeCardScale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = shape,
        color = colors.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else colors.onSurface.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemePreview(mode)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (mode) {
                            AppThemeMode.MATERIAL -> "Material"
                            AppThemeMode.LIQUID_GLASS -> "Liquid Glass"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when (mode) {
                            AppThemeMode.MATERIAL -> "Material 3 standar"
                            AppThemeMode.LIQUID_GLASS -> "Kaca tembus pandang"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.7f)
                    )
                }
                // Klik ditangani oleh Surface.selectable, radio hanya indikator.
                RadioButton(selected = selected, onClick = null)
            }
        }
    }
}

/**
 * Pratinjau mini gaya visual tiap mode, memakai warna tema yang sedang aktif.
 */
@Composable
private fun ThemePreview(mode: AppThemeMode) {
    val colors = MaterialTheme.colorScheme
    val previewShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(previewShape)
    ) {
        when (mode) {
            AppThemeMode.MATERIAL -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.primary.copy(alpha = 0.12f))
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .size(width = 36.dp, height = 20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primary)
                        )
                        Box(
                            Modifier
                                .size(width = 24.dp, height = 20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.secondary)
                        )
                    }
                }
            }

            AppThemeMode.LIQUID_GLASS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.primary.copy(alpha = 0.55f),
                                    colors.secondary.copy(alpha = 0.45f)
                                )
                            )
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        Modifier
                            .size(width = 64.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface.copy(alpha = 0.35f))
                            .border(
                                1.dp,
                                colors.onSurface.copy(alpha = 0.35f),
                                RoundedCornerShape(14.dp)
                            )
                    )
                }
            }
        }
    }
}
