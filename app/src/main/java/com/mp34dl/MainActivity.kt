package com.mp34dl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mp34dl.ui.settings.ThemeModeSelector
import com.mp34dl.ui.theme.AppTheme
import com.mp34dl.ui.theme.AppThemeMode

/**
 * PLACEHOLDER: hanya agar proyek bisa dibuild. Hapus file ini jika sudah punya MainActivity sendiri.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var mode by remember { mutableStateOf(AppThemeMode.MATERIAL) }
            AppTheme(mode = mode, darkTheme = isSystemInDarkTheme()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    ThemeModeSelector(selected = mode, onSelected = { mode = it })
                }
            }
        }
    }
}
