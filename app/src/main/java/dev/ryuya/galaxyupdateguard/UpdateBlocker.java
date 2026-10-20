package dev.ryuya.galaxyupdateguard;

import java.util.Map;

/** Durable original-state journal; partial writes are rolled back and never reported as success. */
final class UpdateBlocker {
  record Journal(Map<String, Integer> original, boolean requested) {
    Journal {
      if (original.isEmpty()) throw new IllegalArgumentException("Empty original state");
      original.forEach(UpdatePackages::validate);
      original = Map.copyOf(original);
    }
  }

  interface Backend {
    Map<String, Integer> states() throws Exception;

    void set(String name, int state) throws Exception;
  }

  interface Store {
    Journal load() throws Exception;

    void save(Journal journal) throws Exception;

    void clear() throws Exception;
  }

  private final Backend backend;
  private final Store store;

  UpdateBlocker(Backend backend, Store store) {
    this.backend = backend;
    this.store = store;
  }

  void block() throws Exception {
    Map<String, Integer> current = backend.states();
    if (current.isEmpty()) throw new IllegalStateException("No supported updater");
    Journal previous = store.load();
    Journal journal = new Journal(previous == null ? current : previous.original, true);
    store.save(journal); // Must reach durable storage before the first privileged mutation.
    try {
      for (String name : journal.original.keySet()) backend.set(name, 3);
      Map<String, Integer> actual = backend.states();
      for (String name : journal.original.keySet())
        if (!Integer.valueOf(3).equals(actual.get(name)))
          throw new IllegalStateException("Block could not be verified");
    } catch (Exception failure) {
      try {
        restore();
      } catch (Exception rollback) {
        failure.addSuppressed(rollback);
      }
      throw failure;
    }
  }

  void restore() throws Exception {
    Journal saved = store.load();
    if (saved == null) return; // Never enable a service disabled by somebody else.
    store.save(new Journal(saved.original, false));
    Exception failure = null;
    for (var entry : saved.original.entrySet()) {
      try {
        backend.set(entry.getKey(), entry.getValue());
      } catch (Exception error) {
        if (failure == null) failure = error;
        else failure.addSuppressed(error);
      }
    }
    Map<String, Integer> actual = backend.states();
    for (var entry : saved.original.entrySet())
      if (!entry.getValue().equals(actual.get(entry.getKey()))) {
        if (failure == null) failure = new IllegalStateException("Restore could not be verified");
      }
    if (failure != null) throw failure;
    store.clear(); // Forget original states only after every service has been verified.
  }
}
