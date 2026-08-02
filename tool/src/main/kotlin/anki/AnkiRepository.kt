package anki

import anki.notes.Note
import anki.notetypes.StockNotetype
import anki.scheduler.CardAnswer
import anki.scheduler.QueuedCards
import anki.scheduler.SchedulingState
import net.ankiweb.rsdroid.Backend

/** The 3 rating buttons this app exposes, mapped to Anki's real ratings. */
enum class CardRating { HARD, GOOD, VERY_GOOD }

class AnkiRepository(private val backend: Backend) {

    private var cachedNotetypeId: Long? = null

    /** Ensures a "Basic" (front/back) notetype exists, returns its id. Creates it on first run only. */
    private fun ensureNotetypeId(): Long {
        cachedNotetypeId?.let { return it }

        val existingId = backend.getNotetypeIdByName("Basic")
        if (existingId != 0L) {
            cachedNotetypeId = existingId
            return existingId
        }

        // First run: fetch the stock "Basic" notetype JSON and register it.
        val notetypeJson = backend.getStockNotetypeLegacy(StockNotetype.Kind.KIND_BASIC)
        val opChanges = backend.addNotetypeLegacy(notetypeJson)
        cachedNotetypeId = opChanges.id
        return opChanges.id
    }

    /** Lists all existing decks as (id, name) pairs. */
    fun listDecks(): List<Pair<Long, String>> {
        return backend.getDeckNames(skipEmptyDefault = true, includeFiltered = false)
            .map { it.id to it.name }
    }

    /** Creates a new deck with the given name and returns its id. */
    fun createDeck(name: String): Long {
        val blankDeck = backend.newDeck()
        val deck = blankDeck.toBuilder().setName(name.trim()).build()
        val opChanges = backend.addDeck(deck)
        return opChanges.id
    }

    /** Counts how many cards exist in the given deck. */
    fun countCardsInDeck(deckName: String): Int {
        val noOrder = anki.search.SortOrder.newBuilder()
            .setNone(anki.generic.Empty.getDefaultInstance())
            .build()
        return backend.searchCards("deck:\"$deckName\"", noOrder).size
    }

    /** Lists all cards in a deck as (noteId, front, back) triples. One entry per note, even if it has multiple cards. */
    fun listCardsInDeck(deckName: String): List<Triple<Long, String, String>> {
        val noOrder = anki.search.SortOrder.newBuilder()
            .setNone(anki.generic.Empty.getDefaultInstance())
            .build()
        val cardIds = backend.searchCards("deck:\"$deckName\"", noOrder)
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

    /** Adds a new flashcard from the user's front/back text, into the given deck. */
    fun addCard(front: String, back: String, deckId: Long) {
        val notetypeId = ensureNotetypeId()

        val blankNote: Note = backend.newNote(notetypeId)
        val note = blankNote.toBuilder()
            .setFields(0, front)
            .setFields(1, back)
            .build()

        backend.addNote(note, deckId)
    }

    /** Updates an existing note's front/back text. */
    fun updateCard(noteId: Long, front: String, back: String) {
        val note = backend.getNote(noteId)
        val updated = note.toBuilder()
            .setFields(0, front)
            .setFields(1, back)
            .build()
        backend.updateNotes(listOf(updated), false)
    }

    /** Deletes a note (and its card(s)) entirely. */
    fun deleteCard(noteId: Long) {
        backend.removeNotes(listOf(noteId), emptyList())
    }

    /** Fetches the next due card (across all decks), or null if there's nothing to review. */
    fun getNextCard(): QueuedCards.QueuedCard? {
        val result = backend.getQueuedCards(1, false)
        return result.cardsList.firstOrNull()
    }

    /** Given a queued card, returns its front/back text by looking up the underlying note. */
    fun getCardFields(queuedCard: QueuedCards.QueuedCard): Pair<String, String> {
        val note = backend.getNote(queuedCard.card.noteId)
        val front = note.fieldsList.getOrElse(0) { "" }
        val back = note.fieldsList.getOrElse(1) { "" }
        return front to back
    }

    /** Submits the user's rating for a card, driving the real spaced-repetition scheduler. */
    fun answerCard(queuedCard: QueuedCards.QueuedCard, rating: CardRating) {
        val states = queuedCard.states
        val newState: SchedulingState = when (rating) {
            CardRating.HARD -> states.hard
            CardRating.GOOD -> states.good
            CardRating.VERY_GOOD -> states.easy
        }
        val protoRating: CardAnswer.Rating = when (rating) {
            CardRating.HARD -> CardAnswer.Rating.HARD
            CardRating.GOOD -> CardAnswer.Rating.GOOD
            CardRating.VERY_GOOD -> CardAnswer.Rating.EASY
        }

        val answer = CardAnswer.newBuilder()
            .setCardId(queuedCard.card.id)
            .setCurrentState(states.current)
            .setNewState(newState)
            .setRating(protoRating)
            .setAnsweredAtMillis(System.currentTimeMillis())
            .setMillisecondsTaken(0)
            .build()

        backend.answerCard(answer)
    }
}