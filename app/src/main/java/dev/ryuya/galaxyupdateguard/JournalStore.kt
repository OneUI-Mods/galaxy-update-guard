package dev.ryuya.galaxyupdateguard

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException
import java.util.LinkedHashMap
import org.json.JSONObject

class JournalStore(context: Context) : UpdateBlocker.Store {
    private val prefs: SharedPreferences

    init {
        prefs =
            context.createDeviceProtectedStorageContext().getSharedPreferences("restore_journal", 0)
    }

    @Throws(Exception::class)
    override fun load(): UpdateBlocker.Journal? {
        val encoded = prefs.getString("journal", null)
        if (encoded == null) return null
        val value = JSONObject(encoded)
        if (value.getInt("format") != 1 || value.getInt("user") != 0)
            throw IOException("Unsupported restore journal")
        val states = value.getJSONObject("original")
        val original = LinkedHashMap<String?, Int?>()
        val keys = states.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val state = states.getInt(name)
            UpdatePackages.validate(name, state)
            original.put(name, state)
        }
        return UpdateBlocker.Journal(original, value.getBoolean("requested"))
    }

    @Throws(Exception::class)
    override fun save(journal: UpdateBlocker.Journal) {
        val value =
            JSONObject()
                .put("format", 1)
                .put("user", 0)
                .put("requested", journal.requested)
                .put("original", JSONObject(journal.original))
        if (!prefs.edit().putString("journal", value.toString()).commit())
            throw IOException("Original state could not be saved")
    }

    @Throws(IOException::class)
    override fun clear() {
        if (!prefs.edit().remove("journal").commit())
            throw IOException("Restore journal could not be cleared")
    }
}
