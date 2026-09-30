package com.example.kanchibattleroyale

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Bundle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                typography = MaterialTheme.typography
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF111827)) {
                    KanchiBattleRoyaleApp()
                }
            }
        }
    }
}

@Composable
fun KanchiBattleRoyaleApp() {
    var selectedCharacter by remember { mutableIntStateOf(0) }
    var inGame by remember { mutableStateOf(false) }
    val gameState = remember { GameState() }

    if (inGame) {
        GameScreen(
            gameState = gameState,
            onExitToMenu = { inGame = false }
        )
    } else {
        StartMenu(
            selectedCharacter = selectedCharacter,
            onCharacterSelected = { selectedCharacter = it },
            onPlay = {
                gameState.start(selectedCharacter)
                inGame = true
            }
        )
    }
}

@Composable
fun StartMenu(
    selectedCharacter: Int,
    onCharacterSelected: (Int) -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1B2D)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(24.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071426))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "KANCHI BATTLE ROYALE",
                    color = Color(0xFFFBBF24),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Silk streets. Local legends. One survivor. Choose your character and enter the arena.",
                    color = Color(0xFFE2E8F0),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )

                CharacterOptions(
                    selectedCharacter = selectedCharacter,
                    onCharacterSelected = onCharacterSelected
                )

                Text(
                    text = "Chellam Keerthi ♥ Vimal Don",
                    color = Color(0xFFFCC3D7),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("PLAY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Fictional Kanchipuram-inspired arena. Offline solo match against bots.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CharacterOptions(
    selectedCharacter: Int,
    onCharacterSelected: (Int) -> Unit
) {
    val characters = listOf(
        "Vimal Don — 120 health",
        "Bottle Mani — faster shooting",
        "Aunty Madhan — 150 health",
        "Vidhakaran Vishnu — faster movement",
        "Chellam Keerthi — health regeneration"
    )

    Column {
        characters.forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onCharacterSelected(index) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCharacter == index,
                    onClick = { onCharacterSelected(index) }
                )
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onExitToMenu: () -> Unit
) {
    val hudText = if (gameState.player != null) {
        "${gameState.player!!.name} | HP ${gameState.player!!.hp.toInt()} | Ammo ${gameState.player!!.ammo} | Alive ${gameState.actors.count { it.alive }} | Kills ${gameState.player!!.kills}"
    } else {
        "Preparing match..."
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameCanvas(gameState = gameState)

        if (!gameState.running && gameState.player != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f),
                colors = CardDefaults.cardColors(containerColor = Color(0x66071426))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val resultText = if (gameState.player!!.alive) {
                        "Victory! ${gameState.player!!.name} is the Kanchi champion."
                    } else {
                        "Match over! Choose a character and try again."
                    }
                    Text(resultText, color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onExitToMenu) {
                        Text("BACK TO MENU")
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC081120))
        ) {
            Text(
                text = hudText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 28.dp)
                .size(96.dp)
                .background(Color(0xFFEF4444), CircleShape)
                .clickable { }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x00FFFFFF), CircleShape)
                    .pointerInputHandle(
                        onPress = { gameState.firePressed = true },
                        onRelease = { gameState.firePressed = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("FIRE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
