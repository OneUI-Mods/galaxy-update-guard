package dev.ryuya.galaxyupdateguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Map
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmRecord

/* Survives configuration changes; no service or periodic root work. */
class GuardModel(app: Application) : AndroidViewModel(app!!) {
    @JvmRecord
    data class State(
        val targets: MutableMap<String?, PackageBackend.Target>,
        val journal: UpdateBlocker.Journal?,
        val busy: Boolean,
        val error: Int,
    ) {
        override fun toString(): String =
            "State[targets=$targets, journal=$journal, busy=$busy, error=$error]"
    }

    private val state: MutableLiveData<GuardModel.State> = MutableLiveData<GuardModel.State>()
    private val worker: ExecutorService? = Executors.newSingleThreadExecutor()
    private val backend: PackageBackend
    private val store: JournalStore
    private val blocker: UpdateBlocker
    @Volatile private var busy: Boolean = false

    init {
        backend = PackageBackend(app)
        store = JournalStore(app)
        blocker = UpdateBlocker(backend, store)
        refresh()
    }

    fun state(): LiveData<GuardModel.State> {
        return state
    }

    fun refresh() {
        worker!!.execute(Runnable { publish(0) })
    }

    fun change(block: Boolean) {
        if (busy) return
        busy = true
        val previous = state.getValue()
        if (previous != null)
            state.setValue(GuardModel.State(previous.targets, previous.journal, true, 0))
        worker!!.execute(
            Runnable {
                var error: Int = 0
                try {
                    RootShell.requireRoot()
                    if (block) blocker.block() else blocker.restore()
                } catch (denied: RootShell.Denied) {
                    error = R.string.root_denied
                } catch (timeout: RootShell.TimedOut) {
                    error = R.string.root_timeout
                } catch (failure: Exception) {
                    error = R.string.operation_error
                } finally {
                    busy = false
                    publish(error)
                }
            }
        )
    }

    private fun publish(error: Int) {
        try {
            state.postValue(GuardModel.State(backend.targets(), store.load(), busy, error))
        } catch (failure: Exception) {
            state.postValue(
                GuardModel.State(
                    Map.of<String?, PackageBackend.Target>(),
                    null,
                    busy,
                    R.string.state_failed,
                )
            )
        }
    }

    override fun onCleared() {
        worker!!.shutdown()
    }
}
