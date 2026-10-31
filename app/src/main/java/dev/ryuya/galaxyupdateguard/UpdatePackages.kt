package dev.ryuya.galaxyupdateguard

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/* Explicit OS-updater allowlist; app stores, Play system updates and SDM config are excluded. */
object UpdatePackages {
    @JvmField val ALL: List<String> = java.util.List.of("com.wssyncmldm", "com.sec.android.soagent")

    @JvmStatic
    fun validate(name: String?, state: Int) {
        require(!(!UpdatePackages.ALL!!.contains(name) || state < 0 || state > 4)) {
            "Unsupported updater state"
        }
    }

    @JvmStatic
    fun stopped(state: Int): Boolean {
        return state == 2 || state == 3
    }

    @JvmStatic
    fun command(name: String?, state: Int): String {
        UpdatePackages.validate(name, state)
        val operation: String? =
            when (state) {
                0 -> "default-state"
                1 -> "enable"
                2 -> "disable"
                3 -> "disable-user"
                4 -> "disable-until-used"
                else -> throw IllegalArgumentException()
            }
        return "exec /system/bin/pm " + operation + " --user 0 " + name
    }
}
