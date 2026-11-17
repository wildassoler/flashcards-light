package com.thelightphone.sample

import anki.AnkiBackendManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import anki.AnkiRepository
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

data class DeckMenuUiState(
    val isLoading: Boolean = true,
    val reviewCount: Int = 0,
)

class DeckScreenViewModel(
    private val repository: AnkiRepository,
    private val deckId: Long,
    private val deckName: String,
) : LightViewModel<Unit>() {

    val uiState = MutableStateFlow(DeckMenuUiState())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true)
            val count = withContext(Dispatchers.IO) { repository.countReviewableInDeck(deckName) }
            uiState.value = DeckMenuUiState(isLoading = false, reviewCount = count)
        }
    }

    fun addCard(front: String, back: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addCard(front, back, deckId) }
            loadStatus()
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
        return DeckScreenViewModel(AnkiBackendManager.getRepository(lightContext.filesDir), deckId, deckName)
    }

    @Composable
    override fun Content() {
        val state by viewModel.uiState.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        var showAddForm by remember { mutableStateOf(false) }
        var frontInput by remember { mutableStateOf("") }
        var backInput by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        fun submitAddForm() {
            if (frontInput.isNotBlank() && backInput.isNotBlank()) {
                viewModel.addCard(frontInput, backInput)
                frontInput = ""
                backInput = ""
                showAddForm = false
                keyboardController?.hide()
            }
        }

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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    if (showAddForm) {
                        OutlinedTextField(
                            value = frontInput,
                            onValueChange = { frontInput = it },
                            label = { Text("Front") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backInput,
                            onValueChange = { backInput = it },
                            label = { Text("Back") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitAddForm() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            LightText(
                                text = "Save",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier
                                    .padding(end = 24.dp)
                                    .lightClickable { submitAddForm() },
                            )
                            LightText(
                                text = "Cancel",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                modifier = Modifier.lightClickable { showAddForm = false },
                            )
                        }
                    } else {
                        when {
                            state.isLoading -> {
                                LightText(text = "Loading...", variant = LightTextVariant.Copy, lighten = true)
                            }
                            state.reviewCount > 0 -> {
                                LightText(
                                    text = "${state.reviewCount} to review",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier
                                        .padding(bottom = 24.dp)
                                        .lightClickable {
                                            navigateTo<Unit>(screenFactory = { sealedActivity ->
                                                ReviewScreen(sealedActivity, deckName)
                                            })
                                        },
                                )
                            }
                            else -> {
                                LightText(
                                    text = "Nothing to review right now.",
                                    variant = LightTextVariant.Copy,
                                    lighten = true,
                                    modifier = Modifier.padding(bottom = 24.dp),
                                )
                            }
                        }

                        LightText(
                            text = "+ Add card",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                            modifier = Modifier.lightClickable { showAddForm = true },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LightText(
                            text = "Edit cards",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                            modifier = Modifier.lightClickable {
                                navigateTo<Unit>(screenFactory = { sealedActivity ->
                                    CardsScreen(sealedActivity, deckName)
                                })
                            },
                        )
                    }
                }
            }
        }
    }
}
