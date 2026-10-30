package dev.ryuya.galaxyupdateguard

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.jvm.JvmStatic

/* Fixed commands only. Both process lifetime and captured output are bounded. */
object RootShell {
    @JvmStatic
    @Throws(Exception::class)
    fun requireRoot() {
        try {
            if (RootShell.run("exec /system/bin/id -u")!!.trim { it <= ' ' } != "0")
                throw RootShell.Denied()
        } catch (denied: RootShell.CommandFailed) {
            throw RootShell.Denied()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun run(command: String?): String? {
        val process: Process?
        try {
            process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        } catch (missing: IOException) {
            throw RootShell.Denied()
        }
        val reader = Executors.newSingleThreadExecutor()
        val output =
            reader.submit<String?>(
                Callable {
                    process.getInputStream().use { input ->
                        ByteArrayOutputStream().use { result ->
                            val buffer = ByteArray(1024)
                            var count: Int
                            while ((input.read(buffer).also { count = it }) != -1) {
                                if (result.size() + count > 16384)
                                    throw IOException("Command output too large")
                                result.write(buffer, 0, count)
                            }
                            return@use result.toString(StandardCharsets.UTF_8)
                        }
                    }
                }
            )
        try {
            if (!process.waitFor(20, TimeUnit.SECONDS)) throw RootShell.TimedOut()
            val text = output.get(2, TimeUnit.SECONDS)
            if (process.exitValue() != 0) throw RootShell.CommandFailed()
            return text
        } finally {
            process.destroyForcibly()
            reader.shutdownNow()
        }
    }

    class Denied : IOException()

    class CommandFailed : IOException()

    class TimedOut : IOException()
}
