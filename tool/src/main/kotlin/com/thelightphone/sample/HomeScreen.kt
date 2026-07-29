package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import anki.AnkiBackendManager
import anki.AnkiRepository
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DecksUiState(
    val isLoading: Boolean = true,
    val decks: List<Pair<Long, String>> = emptyList(),
)

class HomeScreenViewModel(filesDir: File) : LightViewModel<Unit>() {

    private val repository: AnkiRepository by lazy {
        AnkiRepository(AnkiBackendManager.getBackend(filesDir))
    }

    val uiState = MutableStateFlow(DecksUiState())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadDecks()
    }

    fun loadDecks() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true)
            val decks = withContext(Dispatchers.IO) { repository.listDecks() }
            uiState.value = DecksUiState(isLoading = false, decks = decks)
        }
    }

    fun createDeck(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.createDeck(name) }
            loadDecks()
        }
    }
}

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, HomeScreenViewModel>(sealedActivity) {
    override val viewModelClass: Class<HomeScreenViewModel>
        get() = HomeScreenViewModel::class.java

    override fun createViewModel(): HomeScreenViewModel {
        return HomeScreenViewModel(lightContext.filesDir)
    }

    @Composable
    override fun Content() {
        val state by viewModel.uiState.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        var showAddDeckForm by remember { mutableStateOf(false) }
        var deckNameInput by remember { mutableStateOf("") }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(32.dp)
            ) {
                LightText(
                    text = "FlashCards Light",
                    variant = LightTextVariant.Heading,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                if (showAddDeckForm) {
                    OutlinedTextField(
                        value = deckNameInput,
                        onValueChange = { deckNameInput = it },
                        label = { Text("Deck name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (deckNameInput.isNotBlank()) {
                            viewModel.createDeck(deckNameInput)
                            deckNameInput = ""
                            showAddDeckForm = false
                        }
                    }) {
                        Text("Save deck")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LightText(
                        text = "Cancel",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        modifier = Modifier.lightClickable { showAddDeckForm = false },
                    )
                } else {
                    when {
                        state.isLoading -> {
                            LightText(text = "Loading...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        state.decks.isEmpty() -> {
                            LightText(
                                text = "No decks yet.",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                modifier = Modifier.padding(bottom = 24.dp),
                            )
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                                items(state.decks) { (id, name) ->
                                    LightText(
                                        text = name,
                                        variant = LightTextVariant.Copy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .lightClickable {
                                                // TODO: navigate into this deck (add/review cards)
                                            }
                                            .padding(vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }

                    LightText(
                        text = "+ Add deck",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        modifier = Modifier.lightClickable { showAddDeckForm = true },
                    )
                }
            }
        }
    }
}