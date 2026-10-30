package dev.ryuya.galaxyupdateguard

/** Persist original states before changing packages, and retain them until restoration succeeds. */
class UpdateBlocker(private val backend: Backend, private val store: Store) {
    /** Owns an immutable snapshot; callers cannot change a journal after it has been saved. */
    class Journal(original: Map<String?, Int?>?, val requested: Boolean) {
        val original: Map<String?, Int?>

        init {
            val values = original!!
            require(values.isNotEmpty()) { "Empty original state" }
            values.forEach { (name, state) -> UpdatePackages.validate(name, state!!) }
            this.original = java.util.Map.copyOf(values)
        }

        fun original(): Map<String?, Int?> = original

        fun requested(): Boolean = requested

        override fun equals(other: Any?): Boolean =
            other is Journal && original == other.original && requested == other.requested

        override fun hashCode(): Int = 31 * original.hashCode() + requested.hashCode()

        override fun toString(): String = "Journal[original=$original, requested=$requested]"
    }

    interface Backend {
        @Throws(Exception::class) fun states(): MutableMap<String?, Int?>?

        @Throws(Exception::class) fun set(name: String?, state: Int)
    }

    interface Store {
        @Throws(Exception::class) fun load(): Journal?

        @Throws(Exception::class) fun save(journal: Journal)

        @Throws(Exception::class) fun clear()
    }

    @Throws(Exception::class)
    fun block() {
        val current = backend.states()!!
        check(current.isNotEmpty()) { "No supported updater" }
        val journal = Journal(store.load()?.original ?: current, true)
        store.save(journal)
        try {
            for (name in journal.original.keys) backend.set(name, 3)
            val actual = backend.states()!!
            for (name in journal.original.keys) {
                check(actual[name] == 3) { "Block could not be verified" }
            }
        } catch (failure: Exception) {
            try {
                restore()
            } catch (rollback: Exception) {
                failure.addSuppressed(rollback)
            }
            throw failure
        }
    }

    @Throws(Exception::class)
    fun restore() {
        // No journal means there is no original state owned by this application to restore.
        val saved = store.load() ?: return
        store.save(Journal(saved.original, false))
        var failure: Exception? = null
        for ((name, state) in saved.original) {
            try {
                backend.set(name, state!!)
            } catch (error: Exception) {
                if (failure == null) failure = error else failure.addSuppressed(error)
            }
        }
        val actual = backend.states()!!
        for ((name, state) in saved.original) {
            if (state != actual[name] && failure == null) {
                failure = IllegalStateException("Restore could not be verified")
            }
        }
        failure?.let { throw it }
        store.clear()
    }
}
