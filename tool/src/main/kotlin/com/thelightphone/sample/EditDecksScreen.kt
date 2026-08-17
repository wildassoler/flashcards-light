package com.thelightphone.sample

import anki.AnkiBackendManager
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
import anki.AnkiRepository
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditDecksUiState(
    val isLoading: Boolean = true,
    val decks: List<Pair<Long, String>> = emptyList(),
)

class EditDecksScreenViewModel(private val repository: AnkiRepository) : LightViewModel<Unit>() {

    val uiState = MutableStateFlow(EditDecksUiState())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadDecks()
    }

    fun loadDecks() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true)
            val decks = withContext(Dispatchers.IO) { repository.listDecks() }
            uiState.value = EditDecksUiState(isLoading = false, decks = decks)
        }
    }

    fun renameDeck(deckId: Long, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.renameDeck(deckId, newName) }
            loadDecks()
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteDeck(deckId) }
            loadDecks()
        }
    }
}

class EditDecksScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, EditDecksScreenViewModel>(sealedActivity) {
    override val viewModelClass: Class<EditDecksScreenViewModel>
        get() = EditDecksScreenViewModel::class.java

    override fun createViewModel(): EditDecksScreenViewModel {
        return EditDecksScreenViewModel(AnkiBackendManager.getRepository(lightContext.filesDir))
    }

    @Composable
    override fun Content() {
        val state by viewModel.uiState.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()
        var editingDeckId by remember { mutableStateOf<Long?>(null) }
        var renameInput by remember { mutableStateOf("") }
        var deckPendingDelete by remember { mutableStateOf<Long?>(null) }
        val keyboardController = LocalSoftwareKeyboardController.current

        fun openRenameForm(deckId: Long, currentName: String) {
            editingDeckId = deckId
            renameInput = currentName
        }

        fun submitRenameForm() {
            val deckId = editingDeckId ?: return
            if (renameInput.isNotBlank()) {
                viewModel.renameDeck(deckId, renameInput)
                editingDeckId = null
                keyboardController?.hide()
            }
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(32.dp)
            ) {
                LightIcon(
                    icon = LightIcons.BACK,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .lightClickable {
                            when {
                                deckPendingDelete != null -> deckPendingDelete = null
                                editingDeckId != null -> editingDeckId = null
                                else -> goBack()
                            }
                        },
                )
                LightText(
                    text = "Edit decks",
                    variant = LightTextVariant.Heading,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                val deletingId = deckPendingDelete
                val renamingId = editingDeckId
                when {
                    deletingId != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "Delete this deck and all its cards?",
                                variant = LightTextVariant.Copy,
                            )
                        }
                        ConfirmationBar(
                            onCancel = { deckPendingDelete = null },
                            onConfirm = {
                                viewModel.deleteDeck(deletingId)
                                deckPendingDelete = null
                            },
                        )
                    }
                    renamingId != null -> {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("Deck name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitRenameForm() }),
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
                                    .lightClickable { submitRenameForm() },
                            )
                            LightText(
                                text = "Cancel",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                modifier = Modifier.lightClickable { editingDeckId = null },
                            )
                        }
                    }
                    else -> {
                        when {
                            state.isLoading -> {
                                LightText(text = "Loading...", variant = LightTextVariant.Copy, lighten = true)
                            }
                            state.decks.isEmpty() -> {
                                LightText(
                                    text = "No decks yet.",
                                    variant = LightTextVariant.Copy,
                                    lighten = true,
                                )
                            }
                            else -> {
                                LazyColumn {
                                    items(state.decks) { (id, name) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            LightText(
                                                text = name,
                                                variant = LightTextVariant.Copy,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .lightClickable { openRenameForm(id, name) },
                                            )
                                            LightIcon(
                                                icon = LightIcons.PENCIL,
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .lightClickable { openRenameForm(id, name) },
                                            )
                                            LightIcon(
                                                icon = LightIcons.TRASH,
                                                modifier = Modifier.lightClickable { deckPendingDelete = id },
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