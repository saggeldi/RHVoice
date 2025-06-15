package com.github.olga_yakovleva.rhvoice.compose.actions

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.olga_yakovleva.rhvoice.android.R
import java.util.Locale

@Composable
fun ActionsScreen() {
    val actions = listOf<ActionItem>(
        ActionItem(
            title = "Täzelikleri okamak",
            description = "Täzelikleri okamak",
            image = painterResource(R.drawable.ic_launcher_foreground),
            prompt = "",
            actionType = ActionType.NEWS
        ),
        ActionItem(
            title = "Kitap okamak",
            description = "Kitap okamak",
            image = painterResource(R.drawable.ic_launcher_foreground),
            prompt = "",
            actionType = ActionType.LISTEN_BOOK
        ),
        ActionItem(
            title = "Zehin synagy",
            description = "Zehin synagy",
            image = painterResource(R.drawable.ic_launcher_foreground),
            prompt = "",
            actionType = ActionType.QUIZ
        )
    )

    ActionsList(actions = actions)

}

@Composable
fun ActionsList(actions: List<ActionItem>) {

    val selectedIndex = remember {
        mutableIntStateOf(0)
    }

    val context = LocalContext.current

    val textToSpeech = TextToSpeech(context) { i ->
        // if No error is found then only it will run

        if (i != TextToSpeech.ERROR) {
            // To Choose language of speech
            println("Error")
        }
    }
    textToSpeech.language = Locale.forLanguageTag("tuk")

    VolumeButtonsHandler(
        onVolumeUp = {
            selectedIndex.intValue = (selectedIndex.intValue + 1) % actions.size
        },
        onVolumeDown = {
            selectedIndex.intValue = (selectedIndex.intValue - 1 + actions.size) % actions.size
        },
        onPowerButtonSingleClick = {
            textToSpeech.speak("Basma", TextToSpeech.QUEUE_FLUSH, null)
        },
        onPowerButtonDoubleClick = {
            textToSpeech.speak("Iki gezek", TextToSpeech.QUEUE_FLUSH, null)
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(actions.size) { index ->
            ActionItemUi(
                action = actions[index],
                selected = selectedIndex.intValue == index,
                textToSpeech = textToSpeech
            )
        }

    }
}

@Composable
fun ActionItemUi(
    action: ActionItem,
    selected: Boolean = false,
    textToSpeech: TextToSpeech
) {

    LaunchedEffect(selected) {
        if (selected) {
            textToSpeech.speak(action.title,TextToSpeech.QUEUE_FLUSH,null)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = action.title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall
                .copy(
                    fontWeight = FontWeight.W900
                )
        )
    }
}