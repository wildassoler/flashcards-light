package anki

import anki.notes.Note
import anki.notetypes.StockNotetype
import anki.scheduler.CardAnswer
import anki.scheduler.SchedulingStates
import net.ankiweb.rsdroid.Backend

/** The 3 rating buttons this app exposes, mapped to Anki's real ratings. */
enum class CardRating { HARD, GOOD, VERY_GOOD }

/** A card ready to review: its content plus the scheduling states needed to answer it. */
data class ReviewCard(
    val cardId: Long,
    val front: String,
    val back: String,
    val states: SchedulingStates,
)

/** Everything the app's ViewModels need from the Anki collection. */
interface AnkiRepository {
    fun listDecks(): List<Pair<Long, String>>
    fun createDeck(name: String): Long
    fun renameDeck(deckId: Long, newName: String)
    fun deleteDeck(deckId: Long)
    fun countCardsInDeck(deckName: String): Int
    fun countReviewableInDeck(deckName: String): Int
    fun listCardsInDeck(deckName: String): List<Triple<Long, String, String>>
    fun addCard(front: String, back: String, deckId: Long)
    fun updateCard(noteId: Long, front: String, back: String)
    fun deleteCard(noteId: Long)
    fun getNextCardInDeck(deckName: String): ReviewCard?
    fun answerReviewCard(reviewCard: ReviewCard, rating: CardRating)
}

/** Real implementation, backed by the Anki JNI backend. */
class RealAnkiRepository(private val backend: Backend) : AnkiRepository {

    private var cachedNotetypeId: Long? = null

    private fun ensureNotetypeId(): Long {
        cachedNotetypeId?.let { return it }
        val existingId = backend.getNotetypeIdByName("Basic")
        if (existingId != 0L) {
            cachedNotetypeId = existingId
            return existingId
        }
        val notetypeJson = backend.getStockNotetypeLegacy(StockNotetype.Kind.KIND_BASIC)
        val opChanges = backend.addNotetypeLegacy(notetypeJson)
        cachedNotetypeId = opChanges.id
        return opChanges.id
    }

    /** Escapes characters that would break Anki's search syntax (e.g. a deck name containing quotes). */
    private fun escapeForSearch(text: String): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    override fun listDecks(): List<Pair<Long, String>> {
        return backend.getDeckNames(skipEmptyDefault = true, includeFiltered = false)
            .map { it.id to it.name }
    }

    override fun createDeck(name: String): Long {
        val blankDeck = backend.newDeck()
        val deck = blankDeck.toBuilder().setName(name.trim()).build()
        val opChanges = backend.addDeck(deck)
        return opChanges.id
    }

    override fun renameDeck(deckId: Long, newName: String) {
        backend.renameDeck(deckId, newName.trim())
    }

    override fun deleteDeck(deckId: Long) {
        backend.removeDecks(listOf(deckId))
    }

    private fun noSortOrder() = anki.search.SortOrder.newBuilder()
        .setNone(anki.generic.Empty.getDefaultInstance())
        .build()

    override fun countCardsInDeck(deckName: String): Int {
        return backend.searchCards("deck:\"${escapeForSearch(deckName)}\"", noSortOrder()).size
    }

    override fun countReviewableInDeck(deckName: String): Int {
        return backend.searchCards("deck:\"${escapeForSearch(deckName)}\" (is:due or is:new)", noSortOrder()).size
    }

    override fun listCardsInDeck(deckName: String): List<Triple<Long, String, String>> {
        val cardIds = backend.searchCards("deck:\"${escapeForSearch(deckName)}\"", noSortOrder())
        val seenNoteIds = mutableSetOf<Long>()
        val result = mutableListOf<Triple<Long, String, String>>()
        for (cardId in cardIds) {
            val card = backend.getCard(cardId)
            if (seenNoteIds.add(card.noteId)) {
                val note = backend.getNote(card.noteId)
                val front = note.fieldsList.getOrElse(0) { "" }
                val back = note.fieldsList.getOrElse(1) { "" }
                result.add(Triple(card.noteId, front, back))
            }
        }
        return result
    }

    override fun addCard(front: String, back: String, deckId: Long) {
        val notetypeId = ensureNotetypeId()
        val blankNote: Note = backend.newNote(notetypeId)
        val note = blankNote.toBuilder()
            .setFields(0, front)
            .setFields(1, back)
            .build()
        backend.addNote(note, deckId)
    }

    override fun updateCard(noteId: Long, front: String, back: String) {
        val note = backend.getNote(noteId)
        val updated = note.toBuilder()
            .setFields(0, front)
            .setFields(1, back)
            .build()
        backend.updateNotes(listOf(updated), false)
    }

    override fun deleteCard(noteId: Long) {
        backend.removeNotes(listOf(noteId), emptyList())
    }

    override fun getNextCardInDeck(deckName: String): ReviewCard? {
        val cardIds = backend.searchCards("deck:\"${escapeForSearch(deckName)}\" (is:due or is:new)", noSortOrder())
        val cardId = cardIds.firstOrNull() ?: return null
        val card = backend.getCard(cardId)
        val note = backend.getNote(card.noteId)
        val front = note.fieldsList.getOrElse(0) { "" }
        val back = note.fieldsList.getOrElse(1) { "" }
        val states = backend.getSchedulingStates(cardId)
        return ReviewCard(cardId, front, back, states)
    }

    override fun answerReviewCard(reviewCard: ReviewCard, rating: CardRating) {
        val states = reviewCard.states
        val newState = when (rating) {
            CardRating.HARD -> states.hard
            CardRating.GOOD -> states.good
            CardRating.VERY_GOOD -> states.easy
        }
        val protoRating = when (rating) {
            CardRating.HARD -> CardAnswer.Rating.HARD
            CardRating.GOOD -> CardAnswer.Rating.GOOD
            CardRating.VERY_GOOD -> CardAnswer.Rating.EASY
        }

        val answer = CardAnswer.newBuilder()
            .setCardId(reviewCard.cardId)
            .setCurrentState(states.current)
            .setNewState(newState)
            .setRating(protoRating)
            .setAnsweredAtMillis(System.currentTimeMillis())
            .setMillisecondsTaken(0)
            .build()

        backend.answerCard(answer)
    }
}