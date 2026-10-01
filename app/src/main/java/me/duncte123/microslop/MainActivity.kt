package me.duncte123.microslop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.duncte123.microslop.ui.theme.MicroslopTheme

class ValueHolder<T> {
    private var callback: (T) -> Unit = {}

    fun listen(cb: (T) -> Unit) {
        callback = cb
    }

    fun update(newVal: T) {
        callback(newVal)
    }
}

val windowStateHolder = ValueHolder<WindowLayoutInfo>()

class MainActivity : ComponentActivity() {
    private lateinit var windowInfoTracker: WindowInfoTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowInfoTracker = WindowInfoTracker.getOrCreate(this@MainActivity)

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowInfoTracker.windowLayoutInfo(this@MainActivity)
                    .collect { value ->
                        windowStateHolder.update(value)
                    }
            }
        }

        enableEdgeToEdge()
        setContent {
            MicroslopTheme {
                val wmc = WindowMetricsCalculator.getOrCreate()
//                val currentWM = wmc.computeCurrentWindowMetrics(this).bounds.flattenToString()
                val maximumWM = wmc.computeMaximumWindowMetrics(this)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RootView(
                        metrics = maximumWM,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ColourPickerArea(currentColour: HsvColor, modifier: Modifier = Modifier, colourChanged: (HsvColor) -> Unit = {}) {
    Row(
        modifier = modifier
    ) {
        HarmonyColorPicker (
            harmonyMode = ColorHarmonyMode.NONE,
            modifier = Modifier.padding(5.dp),
//            showAlphaBar = false,
            showBrightnessBar = true,
            color = currentColour,
            onColorChanged = { color: HsvColor ->
                colourChanged(color)
            }
        )
    }
}

@Composable
fun RootView(
    metrics: WindowMetrics,
    modifier: Modifier = Modifier
) {
    var layoutInfo by remember { mutableStateOf<WindowLayoutInfo?>(null) }

    windowStateHolder.listen {
        layoutInfo = it
    }

    if (layoutInfo == null) {
        Text(
            text = "No layout info, oops",
            modifier = modifier,
        )
        return
    }

    if (layoutInfo!!.displayFeatures.isEmpty()) {
        Text(
            text = "This app only works when spanned across two displays",
            modifier = modifier,
        )
        return
    }

    val foldingFeature = layoutInfo!!.displayFeatures.find { it is FoldingFeature } as FoldingFeature?

    if (foldingFeature?.isSeparating == false) {
        Text(
            text = "Sorry, you must have two separate displays for this app",
            modifier = modifier,
        )
        return
    }

    var currentColour by remember { mutableStateOf(HsvColor.from(Color.Red)) }

    Row(modifier = modifier) {
        ColourPickerArea(
            currentColour,
            modifier = Modifier/*.background(Color.Yellow)*/
                .width((metrics.widthDp / 2).dp - 16.dp) // 16dp for hinge
                .padding(5.dp),
        ) { newColour ->
            currentColour = newColour
        }
        /*ColorPicker(
            modifier = Modifier.background(Color.Yellow)
                .width((metrics.widthDp / 2).dp + 16.dp) // 16dp for hinge
                .padding(5.dp)
        )*/

        Column(
            modifier = Modifier
                .offset(x = 30.dp)
                .width((metrics.widthDp / 2).dp - 10.dp)
                .padding(5.dp)
                .fillMaxHeight()
                .background(currentColour.toColor())
        ) {
            Text(
                text = "$layoutInfo",
                fontSize = 25.sp
            )
            /*Text(
                text = "Colour: ${currentColour.toColor()}",
                fontSize = 25.sp
            )*/
        }
    }
}

@Composable
fun DisplayLayoutInfo(modifier: Modifier = Modifier) {
    var layoutInfo by remember { mutableStateOf<WindowLayoutInfo?>(null) }

    windowStateHolder.listen {
        layoutInfo = it
    }

    if (layoutInfo == null) {
        Text(
            text = "No layout info",
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier) {
        if (layoutInfo!!.displayFeatures.isEmpty()) {
            Text(
                text = "One logic/physical display - unspanned (this app only works when spanned)",
            )
        } else {
            Text(
                text = "Spanned across displays",
            )
        }

        Text(
            text = "($layoutInfo)",
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MicroslopTheme {
        Greeting("Android")
    }
}