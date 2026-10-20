package dev.ryuya.galaxyupdateguard;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Survives configuration changes; no service or periodic root work. */
public final class GuardModel extends AndroidViewModel {
  record State(
      Map<String, PackageBackend.Target> targets,
      UpdateBlocker.Journal journal,
      boolean busy,
      int error) {}

  private final MutableLiveData<State> state = new MutableLiveData<>();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final PackageBackend backend;
  private final JournalStore store;
  private final UpdateBlocker blocker;
  private volatile boolean busy;

  public GuardModel(Application app) {
    super(app);
    backend = new PackageBackend(app);
    store = new JournalStore(app);
    blocker = new UpdateBlocker(backend, store);
    refresh();
  }

  LiveData<State> state() {
    return state;
  }

  void refresh() {
    worker.execute(() -> publish(0));
  }

  void change(boolean block) {
    if (busy) return;
    busy = true;
    State previous = state.getValue();
    if (previous != null) state.setValue(new State(previous.targets, previous.journal, true, 0));
    worker.execute(
        () -> {
          int error = 0;
          try {
            RootShell.requireRoot();
            if (block) blocker.block();
            else blocker.restore();
          } catch (RootShell.Denied denied) {
            error = R.string.root_denied;
          } catch (RootShell.TimedOut timeout) {
            error = R.string.root_timeout;
          } catch (Exception failure) {
            error = R.string.operation_error;
          } finally {
            busy = false;
            publish(error);
          }
        });
  }

  private void publish(int error) {
    try {
      state.postValue(new State(backend.targets(), store.load(), busy, error));
    } catch (Exception failure) {
      state.postValue(new State(Map.of(), null, busy, R.string.state_failed));
    }
  }

  @Override
  protected void onCleared() {
    worker.shutdown();
  }
}
