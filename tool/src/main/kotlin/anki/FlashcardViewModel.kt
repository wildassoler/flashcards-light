package anki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import anki.scheduler.QueuedCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FlashcardUiState(
    val isLoading: Boolean = true,
    val hasCardDue: Boolean = false,
    val front: String = "",
    val back: String = "",
    val showAnswer: Boolean = false,
)

class FlashcardViewModel(private val repository: AnkiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var currentQueuedCard: QueuedCards.QueuedCard? = null

    init {
        loadNextCard()
    }

    /** Loads the next due card from the scheduler, or shows the empty state if none. */
    fun loadNextCard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showAnswer = false)

            val queuedCard = withContext(Dispatchers.IO) { repository.getNextCard() }
            currentQueuedCard = queuedCard

            _uiState.value = if (queuedCard == null) {
                FlashcardUiState(isLoading = false, hasCardDue = false)
            } else {
                val (front, back) = withContext(Dispatchers.IO) { repository.getCardFields(queuedCard) }
                FlashcardUiState(isLoading = false, hasCardDue = true, front = front, back = back)
            }
        }
    }

    /** Reveals the back of the current card. */
    fun revealAnswer() {
        _uiState.value = _uiState.value.copy(showAnswer = true)
    }

    /** Submits the user's rating for the current card, then loads the next one. */
    fun rate(rating: CardRating) {
        val queuedCard = currentQueuedCard ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.answerCard(queuedCard, rating) }
            loadNextCard()
        }
    }

    /** Adds a new card the user created, from front/back text they typed. */
    fun addCard(front: String, back: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addCard(front, back) }
            if (!_uiState.value.hasCardDue) {
                loadNextCard()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AnkiBackendManager.close()
    }

    /** Builds the ViewModel with its dependencies, without needing an Application reference. */
    class Factory(private val collectionDir: File) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val backend = AnkiBackendManager.getBackend(collectionDir)
            val repository = AnkiRepository(backend)
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(repository) as T
        }
    }
}