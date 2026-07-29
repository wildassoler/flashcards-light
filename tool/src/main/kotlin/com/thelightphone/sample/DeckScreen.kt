package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import anki.CardRating
import anki.scheduler.QueuedCards
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

data class DeckReviewUiState(
    val isLoading: Boolean = true,
    val hasCardDue: Boolean = false,
    val front: String = "",
    val back: String = "",
    val showAnswer: Boolean = false,
)

class DeckScreenViewModel(filesDir: File, private val deckId: Long) : LightViewModel<Unit>() {

    private val repository: AnkiRepository by lazy {
        AnkiRepository(AnkiBackendManager.getBackend(filesDir))
    }

    val uiState = MutableStateFlow(DeckReviewUiState())

    private var currentQueuedCard: QueuedCards.QueuedCard? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadNextCard()
    }

    fun loadNextCard() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true, showAnswer = false)
            // Note: this currently pulls the next due card across ALL decks,
            // not just this one — Anki's GetQueuedCards has no per-deck filter
            // in the API surface we've wired up yet.
            val queuedCard = withContext(Dispatchers.IO) { repository.getNextCard() }
            currentQueuedCard = queuedCard

            uiState.value = if (queuedCard == null) {
                DeckReviewUiState(isLoading = false, hasCardDue = false)
            } else {
                val (front, back) = withContext(Dispatchers.IO) { repository.getCardFields(queuedCard) }
                DeckReviewUiState(isLoading = false, hasCardDue = true, front = front, back = back)
            }
        }
    }

    fun revealAnswer() {
        uiState.value = uiState.value.copy(showAnswer = true)
    }

    fun rate(rating: CardRating) {
        val queuedCard = currentQueuedCard ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.answerCard(queuedCard, rating) }
            loadNextCard()
        }
    }

    fun addCard(front: String, back: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addCard(front, back, deckId) }
            if (!uiState.value.hasCardDue) {
                loadNextCard()
            }
        }
    }
}

class DeckScreen(
    sealedActivity: SealedLightActivity,
    private val deckId: Long,
    private val deckName: String,
) : LightScreen<Unit, DeckScreenViewModel>(sealedActivity) {
    override val viewModelClass: Class<DeckScreenViewModel>
        get() = DeckScreenViewModel::class.java

    override fun createViewModel(): DeckScreenViewModel {
        return DeckScreenViewModel(lightContext.filesDir, deckId)
    }

    @Composable
    override fun Content() {
        val state by viewModel.uiState.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        var showAddForm by remember { mutableStateOf(false) }
        var frontInput by remember { mutableStateOf("") }
        var backInput by remember { mutableStateOf("") }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(32.dp)
            ) {
                LightText(
                    text = "‹ Back",
                    variant = LightTextVariant.Copy,
                    lighten = true,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .lightClickable { goBack() },
                )
                LightText(
                    text = deckName,
                    variant = LightTextVariant.Heading,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                if (showAddForm) {
                    OutlinedTextField(
                        value = frontInput,
                        onValueChange = { frontInput = it },
                        label = { Text("Front") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backInput,
                        onValueChange = { backInput = it },
                        label = { Text("Back") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (frontInput.isNotBlank() && backInput.isNotBlank()) {
                            viewModel.addCard(frontInput, backInput)
                            frontInput = ""
                            backInput = ""
                            showAddForm = false
                        }
                    }) {
                        Text("Save card")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LightText(
                        text = "Cancel",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        modifier = Modifier.lightClickable { showAddForm = false },
                    )
                } else {
                    when {
                        state.isLoading -> {
                            LightText(text = "Loading...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        !state.hasCardDue -> {
                            LightText(
                                text = "Nothing to review right now.",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                modifier = Modifier.padding(bottom = 24.dp),
                            )
                        }
                        else -> {
                            LightText(
                                text = state.front,
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                            if (state.showAnswer) {
                                LightText(
                                    text = state.back,
                                    variant = LightTextVariant.Copy,
                                    lighten = true,
                                    modifier = Modifier.padding(bottom = 24.dp),
                                )
                                Row {
                                    LightText(
                                        text = "Hard",
                                        variant = LightTextVariant.Copy,
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .lightClickable { viewModel.rate(CardRating.HARD) },
                                    )
                                    LightText(
                                        text = "Good",
                                        variant = LightTextVariant.Copy,
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .lightClickable { viewModel.rate(CardRating.GOOD) },
                                    )
                                    LightText(
                                        text = "Very Good",
                                        variant = LightTextVariant.Copy,
                                        modifier = Modifier.lightClickable { viewModel.rate(CardRating.VERY_GOOD) },
                                    )
                                }
                            } else {
                                LightText(
                                    text = "Show answer",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.lightClickable { viewModel.revealAnswer() },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    LightText(
                        text = "+ Add card",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        modifier = Modifier.lightClickable { showAddForm = true },
                    )
                }
            }
        }
    }
}