package dev.ryuya.galaxyupdateguard;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.json.JSONObject;

final class JournalStore implements UpdateBlocker.Store {
  private final SharedPreferences prefs;

  JournalStore(Context context) {
    prefs =
        context.createDeviceProtectedStorageContext().getSharedPreferences("restore_journal", 0);
  }

  @Override
  public UpdateBlocker.Journal load() throws Exception {
    String encoded = prefs.getString("journal", null);
    if (encoded == null) return null;
    JSONObject value = new JSONObject(encoded);
    if (value.getInt("format") != 1 || value.getInt("user") != 0)
      throw new IOException("Unsupported restore journal");
    JSONObject states = value.getJSONObject("original");
    var original = new LinkedHashMap<String, Integer>();
    for (var keys = states.keys(); keys.hasNext(); ) {
      String name = keys.next();
      int state = states.getInt(name);
      UpdatePackages.validate(name, state);
      original.put(name, state);
    }
    return new UpdateBlocker.Journal(original, value.getBoolean("requested"));
  }

  @Override
  public void save(UpdateBlocker.Journal journal) throws Exception {
    JSONObject value =
        new JSONObject()
            .put("format", 1)
            .put("user", 0)
            .put("requested", journal.requested())
            .put("original", new JSONObject(journal.original()));
    if (!prefs.edit().putString("journal", value.toString()).commit())
      throw new IOException("Original state could not be saved");
  }

  @Override
  public void clear() throws IOException {
    if (!prefs.edit().remove("journal").commit())
      throw new IOException("Restore journal could not be cleared");
  }
}
