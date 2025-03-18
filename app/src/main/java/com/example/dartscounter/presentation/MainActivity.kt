package com.example.dartscounter.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            DartCounterApp()
        }
    }
}

@Composable
fun DartCounterApp(viewModel: DartCounterViewModel = viewModel()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        val scrollState = rememberScalingLazyListState()
        LaunchedEffect(Unit) {
            scrollState.scrollToItem(1, 65) // Scroll to the first item at offset 0
        }
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { PlayerScoreSection(viewModel) }
            item { CurrentTurnThrows(viewModel)  }
            item { InputButtons(viewModel) }
        }
    }
}

@Composable
fun PlayerScoreSection(viewModel: DartCounterViewModel) {
    val playerOneScore by viewModel.playerOneScore.collectAsState()
    val playerTwoScore by viewModel.playerTwoScore.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = viewModel.playerOneName,
                style = MaterialTheme.typography.caption1,
                fontSize = 12.sp,
                color = if (viewModel.currentPlayer == 1) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            Text(
                text = playerOneScore.toString(),
                style = MaterialTheme.typography.body1,
                fontSize = 16.sp,
                color = if (viewModel.currentPlayer == 1) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = viewModel.playerTwoName,
                style = MaterialTheme.typography.caption1,
                fontSize = 12.sp,
                color = if (viewModel.currentPlayer == 2) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            Text(
                text = playerTwoScore.toString(),
                style = MaterialTheme.typography.body1,
                fontSize = 16.sp,
                color = if (viewModel.currentPlayer == 2) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
fun CurrentTurnThrows(viewModel: DartCounterViewModel) {

    // List of three elements showing the points scored in the current turn and the current input
    // e.g. "-, -, -" or "1, -, -" or "12, 17, -"
    val throwsWithPending = viewModel.currentTurnThrowsWithPending.collectAsState()
    val currentThrowIndex = viewModel.throwsInCurrentTurn.collectAsState()

    // Blinking animation
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.padding(0.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp)
        ) {
            throwsWithPending.value.forEachIndexed { index, throwText ->
                val isCurrentThrow = index == currentThrowIndex.value
                Text(
                    modifier = Modifier.padding(0.dp),
                    text = throwText,
                    style = MaterialTheme.typography.caption2,
                    fontSize = 10.sp,
                    color = when {
                        isCurrentThrow -> MaterialTheme.colors.primary.copy(alpha = alpha)
                        throwText == "-" -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        else -> MaterialTheme.colors.onSurface
                    }
                )
            }
        }
    }

}

@Composable
fun InputButtons(viewModel: DartCounterViewModel) {
    val availableButtons by viewModel.availableButtons.collectAsState()
    val allButtons = listOf(
        listOf("7", "8", "9", "OK"),
        listOf("4", "5", "6", "x2"),
        listOf("1", "2", "3", "x3"),
        listOf("", "0", "↩", "")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        allButtons.forEach { row ->
            ButtonRow(row, availableButtons, onButtonClick = viewModel::onButtonClick)
        }
    }
}

@Composable
fun ButtonRow(buttons: List<String>, availableButtons: List<String> ,onButtonClick: (String) -> Unit) {
    val buttonColor = Color(0xFF222222)
    val undoColor = Color(0xFFCC3333)
    val textColor = Color.White
    val textColorDisabled = Color.DarkGray

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp)
    ) {
        for (button in buttons) {
            if (button.isNotEmpty()) {
                val active = button in availableButtons

                Button(
                    enabled = active,
                    onClick = { onButtonClick(button) },
                    modifier = Modifier
                        .padding(4.dp, 0.dp)
                        .width(32.dp)
                        .height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (button == "↩") undoColor else buttonColor,
                        contentColor = textColor
                    )
                ) {
                    Text(button, color = if (active) textColor else textColorDisabled)
                }
            }
        }
    }
}