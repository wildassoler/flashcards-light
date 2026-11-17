package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import anki.AnkiBackendManager
import anki.AnkiRepository
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
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

data class CardsUiState(
    val isLoading: Boolean = true,
    val cards: List<Triple<Long, String, String>> = emptyList(),
)

class CardsScreenViewModel(
    private val repository: AnkiRepository,
    private val deckName: String,
) : LightViewModel<Unit>() {

    val uiState = MutableStateFlow(CardsUiState())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadCards()
    }

    fun loadCards() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true)
            val cards = withContext(Dispatchers.IO) { repository.listCardsInDeck(deckName) }
            uiState.value = CardsUiState(isLoading = false, cards = cards)
        }
    }

    fun updateCard(noteId: Long, front: String, back: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateCard(noteId, front, back) }
            loadCards()
        }
    }

    fun deleteCard(noteId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteCard(noteId) }
            loadCards()
        }
    }
}

class CardsScreen(
    sealedActivity: SealedLightActivity,
    private val deckName: String,
) : LightScreen<Unit, CardsScreenViewModel>(sealedActivity) {
    override val viewModelClass: Class<CardsScreenViewModel>
        get() = CardsScreenViewModel::class.java

    override fun createViewModel(): CardsScreenViewModel {
        return CardsScreenViewModel(AnkiBackendManager.getRepository(lightContext.filesDir), deckName)
    }

    @Composable
    override fun Content() {
        val state by viewModel.uiState.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        var editingNoteId by remember { mutableStateOf<Long?>(null) }
        var frontInput by remember { mutableStateOf("") }
        var backInput by remember { mutableStateOf("") }
        var cardPendingDelete by remember { mutableStateOf<Long?>(null) }
        val keyboardController = LocalSoftwareKeyboardController.current

        fun openEditForm(noteId: Long, front: String, back: String) {
            editingNoteId = noteId
            frontInput = front
            backInput = back
        }

        fun submitEditForm() {
            val noteId = editingNoteId ?: return
            if (frontInput.isNotBlank() && backInput.isNotBlank()) {
                viewModel.updateCard(noteId, frontInput, backInput)
                editingNoteId = null
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
                        onClick = {
                            when {
                                cardPendingDelete != null -> cardPendingDelete = null
                                editingNoteId != null -> editingNoteId = null
                                else -> goBack()
                            }
                        },
                    ),
                    center = LightTopBarCenter.Text("EDIT CARDS"),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    val noteId = editingNoteId
                    val deletingId = cardPendingDelete
                    when {
                        deletingId != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                LightText(
                                    text = "Delete this card?",
                                    variant = LightTextVariant.Copy,
                                )
                            }
                            ConfirmationBar(
                                onCancel = { cardPendingDelete = null },
                                onConfirm = {
                                    viewModel.deleteCard(deletingId)
                                    cardPendingDelete = null
                                },
                            )
                        }
                        noteId != null -> {
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
                                keyboardActions = KeyboardActions(onDone = { submitEditForm() }),
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
                                        .lightClickable { submitEditForm() },
                                )
                                LightText(
                                    text = "Cancel",
                                    variant = LightTextVariant.Copy,
                                    lighten = true,
                                    modifier = Modifier.lightClickable { editingNoteId = null },
                                )
                            }
                        }
                        else -> {
                            when {
                                state.isLoading -> {
                                    LightText(text = "Loading...", variant = LightTextVariant.Copy, lighten = true)
                                }
                                state.cards.isEmpty() -> {
                                    LightText(
                                        text = "No cards yet.",
                                        variant = LightTextVariant.Copy,
                                        lighten = true,
                                    )
                                }
                                else -> {
                                    LazyColumn {
                                        items(state.cards) { (id, front, back) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                LightText(
                                                    text = front,
                                                    variant = LightTextVariant.Copy,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .lightClickable { openEditForm(id, front, back) },
                                                )
                                                LightIcon(
                                                    icon = LightIcons.PENCIL,
                                                    size = 1.5f,
                                                    modifier = Modifier
                                                        .padding(end = 16.dp)
                                                        .lightClickable { openEditForm(id, front, back) },
                                                )
                                                LightIcon(
                                                    icon = LightIcons.TRASH,
                                                    size = 1.5f,
                                                    modifier = Modifier.lightClickable { cardPendingDelete = id },
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
        }
    }
}
