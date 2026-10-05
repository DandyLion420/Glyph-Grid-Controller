
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.core.content.edit
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput


class MainActivity : ComponentActivity() {
    private lateinit var glyphManager: GlyphMatrixManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glyphManager = GlyphMatrixManager.getInstance(this)
        glyphManager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(p0: ComponentName?) {
                println("GLYPH CONNECTED")
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
            var patternName by remember { mutableStateOf("") }
            var showSaveDialog by remember { mutableStateOf(false) }
            val prefs =
                getSharedPreferences("glyph_patterns", MODE_PRIVATE)
            val savedPatterns =
                remember { mutableStateListOf<String>()
                }
            LaunchedEffect(Unit) {
                savedPatterns.addAll(prefs.all.keys)
            }
            LaunchedEffect(pixels.toList(), brightness) {
                println("GLYPH UPDATE")
                glyphManager.setAppMatrixFrame(
                    IntArray(
                        169
                    ) { if (pixels[it]) brightness else 0 }) }
            GlyphEditorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(it)) {
                        GlyphGrid(
                            pixels = pixels,
                            onClear = { pixels.replaceAll { false } },
                            brightness = brightness,
                            onBrightnessChange = { brightness = it },
                            onInvert = { pixels.replaceAll { !it } },
                            onSave = { showSaveDialog = true }
                        )
                        if (showSaveDialog) {
                            AlertDialog(onDismissRequest = { showSaveDialog = false },
                                title = { Text("Save Pattern") },
                                text = {
                                    OutlinedTextField(value = patternName,
                                        onValueChange = { patternName = it },
                                        label = { Text("Pattern name") }
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val patternData =
                                            pixels.joinToString("") { if (it) "1" else "0" }
                                        prefs.edit { putString(patternName, patternData) }
                                        if (!savedPatterns.contains(patternName)) savedPatterns.add(patternName)
                                        patternName = ""
                                        showSaveDialog = false
                                    }) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSaveDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                                    }
                                    }
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
    onBrightnessChange: (Int) -> Unit,
    onInvert: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Save / Load
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }

            Button(
                onClick = { },
                modifier = Modifier.weight(1f)
            ) {
                Text("Load")
            }
        }

        // Main 13 x 13 editor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    var paintState = true
                    val visitedPixels = mutableSetOf<Int>()

                    detectDragGestures(
                        onDragStart = { offset ->
                            visitedPixels.clear()

                            val col = (offset.x / (size.width / 13f))
                                .toInt()
                                .coerceIn(0, 12)

                            val row = (offset.y / (size.height / 13f))
                                .toInt()
                                .coerceIn(0, 12)

                            val index = row * 13 + col

                            paintState = !pixels[index]
                            pixels[index] = paintState
                            visitedPixels.add(index)
                        },

                        onDrag = { change, _ ->
                            change.consume()

                            val col = (change.position.x / (size.width / 13f))
                                .toInt()
                                .coerceIn(0, 12)

                            val row = (change.position.y / (size.height / 13f))
                                .toInt()
                                .coerceIn(0, 12)

                            val index = row * 13 + col

                            if (index !in visitedPixels) {
                                pixels[index] = paintState
                                visitedPixels.add(index)
                            }
                        }
                    )
                }
        ) {
            for (row in 0 until 13) {
                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    for (col in 0 until 13) {
                        val index = row * 13 + col
                        val isOn = pixels[index]

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .background(
                                    color = if (isOn) Color.White else Color.DarkGray,
                                    shape = RoundedCornerShape(35)
                                )
                                .clickable {
                                    pixels[index] = !pixels[index]
                                }
                        )
                    }
                }
            }
        }

        // Brightness / Lock
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Slider(
                value = brightness.toFloat(),
                onValueChange = {
                    onBrightnessChange(it.toInt())
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { }
            ) {
                Text("Lock")
            }
        }

        // Clear / Invert
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onClear,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }

            Button(
                onClick = onInvert,
                modifier = Modifier.weight(1f)
            ) {
                Text("Invert")
            }
        }
    }
}
