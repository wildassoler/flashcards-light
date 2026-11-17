package com.thelightphone.sample

import anki.AnkiBackendManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import anki.AnkiRepository
import anki.CardRating
import anki.ReviewCard
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewUiState(
    val isLoading: Boolean = true,
    val hasCard: Boolean = false,
    val front: String = "",
    val back: String = "",
    val showAnswer: Boolean = false,
)

class ReviewScreenViewModel(
    private val repository: AnkiRepository,
    private val deckName: String,
) : LightViewModel<Unit>() {

    val uiState = MutableStateFlow(ReviewUiState())

    private var currentCard: ReviewCard? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadNextCard()
    }

    fun loadNextCard() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true, showAnswer = false)
            val card = withContext(Dispatchers.IO) { repository.getNextCardInDeck(deckName) }
            currentCard = card

            uiState.value = if (card == null) {
                ReviewUiState(isLoading = false, hasCard = false)
            } else {
                ReviewUiState(isLoading = false, hasCard = true, front = card.front, back = card.back)
            }
        }
    }

    fun revealAnswer() {
        uiState.value = uiState.value.copy(showAnswer = true)
    }

    fun rate(rating: CardRating) {
        val card = currentCard ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.answerReviewCard(card, rating) }
            loadNextCard()
        }
    }
}

class ReviewScreen(
    sealedActivity: SealedLightActivity,
    private val deckName: String,
) : LightScreen<Unit, ReviewScreenViewModel>(sealedActivity) {
    override val viewModelClass: Class<ReviewScreenViewModel>
        get() = ReviewScreenViewModel::class.java

    override fun createViewModel(): ReviewScreenViewModel {
        return ReviewScreenViewModel(AnkiBackendManager.getRepository(lightContext.filesDir), deckName)
    }

    @Composable
    override fun Content() {
        val state by viewModel.uiState.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text(deckName.uppercase()),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.isLoading -> {
                            LightText(text = "Loading...", variant = LightTextVariant.Copy, lighten = true)
                        }
                        !state.hasCard -> {
                            LightText(
                                text = "Nothing more to review.",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                LightText(
                                    text = state.front,
                                    variant = LightTextVariant.Heading,
                                    modifier = Modifier.padding(bottom = 24.dp),
                                )
                                if (state.showAnswer) {
                                    LightText(
                                        text = state.back,
                                        variant = LightTextVariant.Copy,
                                        lighten = true,
                                        modifier = Modifier.padding(bottom = 32.dp),
                                    )
                                    Row {
                                        LightText(
                                            text = "Hard",
                                            variant = LightTextVariant.Copy,
                                            modifier = Modifier
                                                .padding(end = 24.dp)
                                                .alpha(0.5f)
                                                .lightClickable { viewModel.rate(CardRating.HARD) },
                                        )
                                        LightText(
                                            text = "Good",
                                            variant = LightTextVariant.Copy,
                                            modifier = Modifier
                                                .padding(end = 24.dp)
                                                .alpha(0.75f)
                                                .lightClickable { viewModel.rate(CardRating.GOOD) },
                                        )
                                        LightText(
                                            text = "Very Good",
                                            variant = LightTextVariant.Copy,
                                            modifier = Modifier
                                                .alpha(1f)
                                                .lightClickable { viewModel.rate(CardRating.VERY_GOOD) },
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
                    }
                }
            }
        }
    }
}
