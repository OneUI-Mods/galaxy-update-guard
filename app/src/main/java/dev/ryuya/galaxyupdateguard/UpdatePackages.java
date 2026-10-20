package dev.ryuya.galaxyupdateguard;

import java.util.List;

/** Explicit OS-updater allowlist; app stores, Play system updates and SDM config are excluded. */
final class UpdatePackages {
  static final List<String> ALL = List.of("com.wssyncmldm", "com.sec.android.soagent");

  static void validate(String name, int state) {
    if (!ALL.contains(name) || state < 0 || state > 4)
      throw new IllegalArgumentException("Unsupported updater state");
  }

  static boolean stopped(int state) {
    return state == 2 || state == 3;
  }

  static String command(String name, int state) {
    validate(name, state);
    String operation =
        switch (state) {
          case 0 -> "default-state";
          case 1 -> "enable";
          case 2 -> "disable";
          case 3 -> "disable-user";
          case 4 -> "disable-until-used";
          default -> throw new IllegalArgumentException();
        };
    return "exec /system/bin/pm " + operation + " --user 0 " + name;
  }
}
