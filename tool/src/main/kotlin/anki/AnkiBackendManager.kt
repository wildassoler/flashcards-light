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

    /**
     * Returns the open Backend, creating and opening the collection
     * on first call. collectionDir should be the app's internal files dir.
     */
    fun getBackend(collectionDir: File): Backend {
        val existing = backend
        if (existing != null && existing.isOpen()) return existing

        val newBackend = BackendFactory.getBackend(listOf("en"))
        val collectionPath = File(collectionDir, "collection.anki2").absolutePath
        newBackend.openCollection(collectionPath)

        backend = newBackend
        return newBackend
    }

    fun close() {
        backend?.close()
        backend = null
    }
}