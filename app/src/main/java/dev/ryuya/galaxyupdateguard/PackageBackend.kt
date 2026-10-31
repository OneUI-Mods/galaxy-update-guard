package dev.ryuya.galaxyupdateguard

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Process
import java.util.LinkedHashMap
import java.util.Map
import kotlin.jvm.JvmRecord
import kotlin.jvm.JvmStatic

class PackageBackend(context: Context) : UpdateBlocker.Backend {
    @JvmRecord
    data class Target(val name: String?, val label: String?, val version: String?, val state: Int) {

        override fun toString(): String =
            "Target[name=$name, label=$label, version=$version, state=$state]"
    }

    private val packages: PackageManager

    init {
        packages = context.getPackageManager()
    }

    @Throws(Exception::class)
    fun targets(): MutableMap<String?, PackageBackend.Target> {
        if (!PackageBackend.supportedDevice()) return Map.of<String?, PackageBackend.Target?>()
        val result = LinkedHashMap<String?, PackageBackend.Target>()
        for (name in UpdatePackages.ALL!!) {
            try {
                val info = packages.getPackageInfo(name!!, PackageManager.MATCH_DISABLED_COMPONENTS)
                val app = info.applicationInfo
                if (app == null || (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) continue
                val state = packages.getApplicationEnabledSetting(name!!)
                result.put(
                    name,
                    PackageBackend.Target(
                        name,
                        packages.getApplicationLabel(app).toString(),
                        info.versionName,
                        state,
                    ),
                )
            } catch (missing: NameNotFoundException) {
                // Carrier variants may omit one updater; never guess an unrelated replacement.
            }
        }
        return result
    }

    @Throws(Exception::class)
    override fun states(): MutableMap<String?, Int?> {
        val result = LinkedHashMap<String?, Int?>()
        targets()!!.forEach { (name: String?, target: PackageBackend.Target?) ->
            result.put(name, target!!.state)
        }
        return result
    }

    @Throws(Exception::class)
    override fun set(name: String?, state: Int) {
        UpdatePackages.validate(name, state)
        check(targets()!!.containsKey(name)) { "Updater identity changed" }
        RootShell.run(UpdatePackages.command(name, state))
    }

    companion object {
        @JvmStatic
        fun supportedDevice(): Boolean {
            return Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
                (Build.VERSION.SDK_INT == 36 || Build.VERSION.SDK_INT == 37) &&
                Process.myUid() / 100000 == 0
        }
    }
}
