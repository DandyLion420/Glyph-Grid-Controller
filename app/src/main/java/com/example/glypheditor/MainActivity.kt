package com.example.glypheditor

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.glypheditor.ui.theme.GlyphEditorTheme
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.Glyph


class MainActivity : ComponentActivity() {
    private lateinit var glyphManager: GlyphMatrixManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glyphManager = GlyphMatrixManager.getInstance(this)
        glyphManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(p0: ComponentName?) {
                glyphManager.register(Glyph.DEVICE_25111p)
                val colors = IntArray(169)
                val frame = colors.copyOf()
                glyphManager.setAppMatrixFrame(frame)
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                glyphManager.unInit()
            }
        })
        enableEdgeToEdge()
        setContent {
            val pixels =
                remember { mutableStateListOf<Boolean>().apply { repeat(169) { add(false) } } }
            var brightness by remember { mutableStateOf(255) }
            LaunchedEffect(pixels.toList(), brightness) {
                glyphManager.setAppMatrixFrame(
                    IntArray(
                        169
                    ) { if (pixels[it]) brightness else 0 })
            }
            GlyphEditorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(it)) {
                        GlyphGrid(
                            pixels = pixels,
                            onClear = { pixels.replaceAll { false } },
                            brightness = brightness,
                            onBrightnessChange = { brightness = it })
                    }
                }
            }
        }
    }

    @Composable
    fun GlyphGrid(
        pixels: MutableList<Boolean>,
        onClear: () -> Unit,
        brightness: Int,
        onBrightnessChange: (Int) -> Unit
    ) {
        Column {
            Button(onClick = onClear) {
                Text("Clear All")
            }
            Slider(
                value = brightness.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 0f..255f
            )
            for (row in 0 until 13) {
                Row {
                    for (col in 0 until 13) {
                        val index = row * 13 + col
                        val isOn = pixels[index]
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(if (isOn) Color.White else Color.Black)
                                .clickable { pixels[index] = !pixels[index] }
                                .padding(1.dp),
                            content = {}
                        )
                    }
                }
            }
        }
    }
}