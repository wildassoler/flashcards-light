package anki

import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendFactory
import java.io.File

/**
 * Keeps a single Backend instance alive for the whole app.
 * The Anki backend does not support multiple open collections at once,
 * so this must stay a singleton.
 */
object AnkiBackendManager {
    private var backend: Backend? = null
    private var libraryLoaded = false

    private fun ensureLibraryLoaded() {
        if (libraryLoaded) return
        System.loadLibrary("rsdroid")
        libraryLoaded = true
    }

    /**
     * Returns the open Backend, creating and opening the collection
     * on first call. collectionDir should be the app's internal files dir.
     */
    fun getBackend(collectionDir: File): Backend {
        val existing = backend
        if (existing != null && existing.isOpen()) return existing

        ensureLibraryLoaded()

        val newBackend = BackendFactory.getBackend(listOf("en"))
        val collectionPath = File(collectionDir, "collection.anki2").absolutePath
        newBackend.openCollection(collectionPath)

        // New collections may default to the legacy scheduler, where
        // GetQueuedCards/AnswerCard (v3 scheduler APIs) return nothing.
        // This is a no-op if already on the modern scheduler.
        newBackend.upgradeScheduler()

        backend = newBackend
        return newBackend
    }

    fun close() {
        backend?.close()
        backend = null
    }
}